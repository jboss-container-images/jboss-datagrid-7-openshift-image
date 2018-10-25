package org.infinispan.online.service.datagrid;

import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.online.service.endpoint.HotRodConfiguration;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.endpoint.HotRodUtil;
import org.infinispan.online.service.endpoint.HotRodUtil.LazyRemoteCacheManager;
import org.infinispan.online.service.scaling.ScalingTester;
import org.infinispan.online.service.testdomain.AnalyzerTestEntity;
import org.infinispan.online.service.testdomain.AnalyzerTestEntityMarshaller;
import org.infinispan.online.service.utils.DeploymentHelper;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

   private ReadinessCheck readinessCheck = new ReadinessCheck();
   private ScalingTester scalingTester = new ScalingTester();

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
      readinessCheck.waitUntilAllPodsAreReady();
   }

   @InSequence(1)
   @Test
   public void put_on_persisted_cache() {
      try (LazyRemoteCacheManager lazyRemote = HotRodUtil.lazyRemoteCacheManager()) {
         lazyRemote
            .andThen(initProtoSchema())
            .andThen(createIndexLockingCache())
            .andThen(createIndexMetadataCache())
            .andThen(createIndexDataCache())
            .andThen(createDataCache())
            .andThen(remote -> remote.getCache(CACHE_NAME))
            .andThen(remoteCache -> {
               Map<String, AnalyzerTestEntity> entries = new HashMap<>();
               entries.put("analyzed1", new AnalyzerTestEntity("tested 123", 3));
               entries.put("analyzed2", new AnalyzerTestEntity("testing 1234", 3));
               entries.put("analyzed3", new AnalyzerTestEntity("xyz", null));
               remoteCache.putAll(entries);
               return remoteCache;
            })
            .apply(remoteConfiguration());
      }
   }

   @InSequence(2)
   @Test
   public void query_from_memory() {
      queryData();
   }

   @RunAsClient
   @InSequence(3) //must be run from the client where "oc" is installed
   @Test
   public void scale_down() {
      scalingTester.scaleDownStatefulSet(0, SERVICE_NAME);
   }

   @RunAsClient
   @InSequence(4) //must be run from the client where "oc" is installed
   @Test
   public void scale_up() {
      scalingTester.scaleUpStatefulSet(1, SERVICE_NAME);
   }

   @InSequence(5)
   @Test
   public void query_from_persistence() {
      queryData();
   }

   private void queryData() {
      try (LazyRemoteCacheManager lazyRemote = HotRodUtil.lazyRemoteCacheManager()) {
         lazyRemote
            .andThen(initProtoSchema())
            .andThen(remote -> remote.getCache(CACHE_NAME))
            .andThen(remoteCache -> {
               final QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
               final Query query = queryFactory
                  .create("from sample_bank_account.AnalyzerTestEntity where f1:'test'");

               List<AnalyzerTestEntity> list = query.list();
               log.debugf("Query returned: %s", list);
               assertEquals(2, list.size());

               return remoteCache;
            })
            .apply(remoteConfiguration());
      }
   }

   private ConfigurationBuilder remoteConfiguration() {
      return HotRodConfiguration
         .secured()
         .andThen(cfg -> {
            cfg.marshaller(new ProtoStreamMarshaller());
            return cfg;
         })
         .apply(SERVICE_NAME);
   }

   private Function<RemoteCacheManager, RemoteCacheManager> createIndexLockingCache() {
      return HotRodTester.createCache(
         indexCacheXml("replicated", false, INDEX_LOCKING_CACHE_NAME)
         , INDEX_LOCKING_CACHE_NAME
      );
   }

   private Function<RemoteCacheManager, RemoteCacheManager> createIndexMetadataCache() {
      return HotRodTester.createCache(
         indexCacheXml("replicated", true, INDEX_METADATA_CACHE_NAME)
         , INDEX_METADATA_CACHE_NAME
      );
   }

   private Function<RemoteCacheManager, RemoteCacheManager> createIndexDataCache() {
      return HotRodTester.createCache(
         indexCacheXml("distributed", false, INDEX_DATA_CACHE_NAME)
         , INDEX_DATA_CACHE_NAME
      );
   }

   private Function<RemoteCacheManager, RemoteCacheManager> createDataCache() {
      XMLStringConfiguration xml = new XMLStringConfiguration(String.format(
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
         CACHE_NAME, INDEX_LOCKING_CACHE_NAME, INDEX_METADATA_CACHE_NAME, INDEX_DATA_CACHE_NAME
      ));

      return HotRodTester.createCache(xml, CACHE_NAME);
   }

   private Function<RemoteCacheManager, RemoteCacheManager> initProtoSchema() {
      return HotRodUtil.initProtoSchema(
         FileDescriptorSource.fromString("custom_analyzer.proto", CUSTOM_ANALYZER_PROTO_SCHEMA)
         , new AnalyzerTestEntityMarshaller()
      );
   }

   private static XMLStringConfiguration indexCacheXml(String cacheMode, boolean preload, String cacheName) {
      return new XMLStringConfiguration(String.format(
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
      ));
   }

}
