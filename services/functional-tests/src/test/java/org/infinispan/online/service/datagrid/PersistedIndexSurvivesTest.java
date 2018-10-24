package org.infinispan.online.service.datagrid;

import io.fabric8.openshift.client.OpenShiftClient;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.client.hotrod.Search;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.scaling.ScalingTester;
import org.infinispan.online.service.testdomain.AnalyzerTestEntity;
import org.infinispan.online.service.testdomain.AnalyzerTestEntityMarshaller;
import org.infinispan.online.service.utils.DeploymentHelper;
import org.infinispan.online.service.utils.OpenShiftClientCreator;
import org.infinispan.online.service.utils.OpenShiftCommandlineClient;
import org.infinispan.online.service.utils.OpenShiftHandle;
import org.infinispan.online.service.utils.ReadinessCheck;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.infinispan.online.service.utils.XmlUtils.prettyXml;
import static org.junit.Assert.assertEquals;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class PersistedIndexSurvivesTest {

   private static final Log log = LogFactory.getLog(PersistedIndexSurvivesTest.class);

   private static final String CUSTOM_ANALYZER_PROTO_SCHEMA = "package sample_bank_account;\n" +
      "/* @Indexed \n" +
      "   @Analyzer(definition = \"standard-with-stop\") */" +
      "message AnalyzerTestEntity {\n" +
      "\t/* @Field(store = Store.YES, analyze = Analyze.YES, analyzer = @Analyzer(definition = \"stemmer\")) */\n" +
      "\toptional string f1 = 1;\n" +
      "\t/* @Field(store = Store.YES, analyze = Analyze.NO, indexNullAs = \"-1\") */\n" +
      "\toptional int32 f2 = 2;\n" +
      "}\n";

   private static final String SERVICE_NAME = "datagrid-service";
   private static final String CACHE_NAME = "test-persistent-indexed";

   private static final String INDEX_LOCKING_CACHE_NAME = "test-persistent-indexed-locking";
   private static final String INDEX_METADATA_CACHE_NAME = "test-persistent-indexed-metadata";
   private static final String INDEX_DATA_CACHE_NAME = "test-persistent-indexed-data";

   private OpenShiftClient client = OpenShiftClientCreator.getClient();

   private ReadinessCheck readinessCheck = new ReadinessCheck();
   private OpenShiftHandle handle = new OpenShiftHandle(client);

   private ScalingTester scalingTester = new ScalingTester();
   private OpenShiftCommandlineClient commandlineClient = new OpenShiftCommandlineClient();

   @Deployment
   public static Archive<?> deploymentApp() {
      return ShrinkWrap
         .create(WebArchive.class, "test.war")
         .addAsLibraries(DeploymentHelper.testLibs())
         .addPackage(ReadinessCheck.class.getPackage())
         .addPackage(ScalingTester.class.getPackage())
         .addPackage(HotRodTester.class.getPackage())
         .addPackage(AnalyzerTestEntity.class.getPackage());
   }

   @Before
   public void before() {
      readinessCheck.waitUntilAllPodsAreReady(client);
   }

   @InSequence(1)
   @Test
   public void put_on_persisted_cache() throws Exception {
      URL hotRodService = handle.getServiceWithName(SERVICE_NAME + "-hotrod");
      HotRodTester hotRodTester = new HotRodTester(SERVICE_NAME, hotRodService, client, true);

      hotRodTester.initProtoSchema(
         FileDescriptorSource.fromString("custom_analyzer.proto", CUSTOM_ANALYZER_PROTO_SCHEMA)
         , new AnalyzerTestEntityMarshaller()
      );

      createIndexLockingCache(hotRodTester);
      createIndexMetadataCache(hotRodTester);
      createIndexDataCache(hotRodTester);
      createDataCache(hotRodTester);

      Map<String, AnalyzerTestEntity> entries = new HashMap<>();
      entries.put("analyzed1", new AnalyzerTestEntity("tested 123", 3));
      entries.put("analyzed2", new AnalyzerTestEntity("testing 1234", 3));
      entries.put("analyzed3", new AnalyzerTestEntity("xyz", null));

      hotRodTester.namedCachePutAllTest(entries, CACHE_NAME);
   }

   @InSequence(2)
   @Test
   public void query_from_memory() throws Exception {
      queryData();
   }

   @RunAsClient
   @InSequence(3) //must be run from the client where "oc" is installed
   @Test
   public void scale_down() {
      scalingTester.scaleDownStatefulSet(0, SERVICE_NAME, client, commandlineClient, readinessCheck);
   }

   @RunAsClient
   @InSequence(4) //must be run from the client where "oc" is installed
   @Test
   public void scale_up() {
      scalingTester.scaleUpStatefulSet(1, SERVICE_NAME, client, commandlineClient, readinessCheck);
   }

   @InSequence(5)
   @Test
   public void query_from_persistence() throws Exception {
      queryData();
   }

   private void queryData() throws MalformedURLException {
      URL hotRodService = handle.getServiceWithName(SERVICE_NAME + "-hotrod");
      HotRodTester hotRodTester = new HotRodTester(SERVICE_NAME, hotRodService, client, true);

      hotRodTester.initProtoSchema(
         FileDescriptorSource.fromString("custom_analyzer.proto", CUSTOM_ANALYZER_PROTO_SCHEMA)
         , new AnalyzerTestEntityMarshaller()
      );

      final QueryFactory queryFactory =
         Search.getQueryFactory(hotRodTester.getRemoteCacheManager().getCache(CACHE_NAME));
      final Query query = queryFactory
         .create("from sample_bank_account.AnalyzerTestEntity where f1:'test'");
      List<AnalyzerTestEntity> list = query.list();

      log.debugf("Query returned: %s", list);

      assertEquals(2, list.size());
   }

   private static void createIndexLockingCache(HotRodTester hotRodTester) {
      final String cacheName = INDEX_LOCKING_CACHE_NAME;

      String xml = indexCacheXml("replicated", false, cacheName);
      log.debugf("Index locking cache XML: %n%s", prettyXml(xml));

      hotRodTester.createNamedCache(cacheName, new XMLStringConfiguration(xml));
   }

   private static void createIndexMetadataCache(HotRodTester hotRodTester) {
      final String cacheName = INDEX_METADATA_CACHE_NAME;

      String xml = indexCacheXml("replicated", true, cacheName);
      log.debugf("Index metadata cache XML: %n%s", prettyXml(xml));

      hotRodTester.createNamedCache(cacheName, new XMLStringConfiguration(xml));
   }

   private static void createIndexDataCache(HotRodTester hotRodTester) {
      String cacheName = INDEX_DATA_CACHE_NAME;

      String xml = indexCacheXml("distributed", false, cacheName);
      log.debugf("Index data cache XML: %n%s", prettyXml(xml));

      hotRodTester.createNamedCache(cacheName, new XMLStringConfiguration(xml));
   }

   private static String indexCacheXml(String cacheMode, boolean preload, String cacheName) {
      return String.format(
         "<infinispan>" +
            "<cache-container>" +
               "<%2$s-cache name=\"%1$s\">" +
                  "<indexing index=\"NONE\"/>" +
                  "<persistence passivation=\"false\">" +
                     "<file-store " +
                        "shared=\"false\" " +
                        "fetch-state=\"true\" " +
                        "path=\"${jboss.server.data.dir}/datagrid-infinispan/%4$s\" " +
                        "preload=\"%3$s\" " +
                     "/>" +
                  "</persistence>" +
               "</%2$s-cache>" +
            "</cache-container>" +
         "</infinispan>",
         cacheName, cacheMode, preload, CACHE_NAME
      );
   }

   private static void createDataCache(HotRodTester hotRodTester) {
      String cacheName = CACHE_NAME;

      String xml = String.format(
         "<infinispan>" +
            "<cache-container>" +
               "<distributed-cache name=\"%1$s\">" +
                  "<indexing index=\"LOCAL\">" +
                     "<property name=\"default.indexmanager\">org.infinispan.query.indexmanager.InfinispanIndexManager</property>" +
                     "<property name=\"default.locking_cachename\">%2$s</property>" +
                     "<property name=\"default.metadata_cachename\">%3$s</property>" +
                     "<property name=\"default.data_cachename\">%4$s</property>" +
                  "</indexing>" +
                  "<persistence passivation=\"false\">" +
                     "<file-store " +
                        "shared=\"false\" " +
                        "fetch-state=\"true\" " +
                        "path=\"${jboss.server.data.dir}/datagrid-infinispan/%1$s\" " +
                     "/>" +
                  "</persistence>" +
               "</distributed-cache>" +
            "</cache-container>" +
         "</infinispan>",
         cacheName, INDEX_LOCKING_CACHE_NAME, INDEX_METADATA_CACHE_NAME, INDEX_DATA_CACHE_NAME
      );
      log.debugf("Data cache XML: %n%s", prettyXml(xml));

      hotRodTester.createNamedCache(cacheName, new XMLStringConfiguration(xml));
   }

}
