package org.infinispan.online.service.datagrid;

import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.online.service.endpoint.HotRodConfiguration;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.endpoint.HotRodUtil;
import org.infinispan.online.service.endpoint.HotRodUtil.LazyRemoteCacheManager;
import org.infinispan.online.service.scaling.ScalingTester;
import org.infinispan.online.service.utils.DeploymentHelper;
import org.infinispan.online.service.utils.ReadinessCheck;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.function.Function;

import static org.junit.Assert.assertEquals;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class PersistedDataSurvivesTest {

   private static final String SERVICE_NAME = "datagrid-service";
   private static final String CACHE_NAME = "custom-persistent";

   private ReadinessCheck readinessCheck = new ReadinessCheck();
   private ScalingTester scalingTester = new ScalingTester();

   @Deployment
   public static Archive<?> deploymentApp() {
      return ShrinkWrap
         .create(WebArchive.class, "test.war")
         .addAsLibraries(DeploymentHelper.testLibs())
         .addPackage(ReadinessCheck.class.getPackage())
         .addPackage(ScalingTester.class.getPackage())
         .addPackage(HotRodTester.class.getPackage());
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
            .andThen(createDataCache())
            .andThen(remote -> remote.getCache(CACHE_NAME))
            .andThen(putOnCache())
            .andThen(getFromCache())
            .apply(HotRodConfiguration.secured().apply(SERVICE_NAME));
      }
   }

   @RunAsClient
   @InSequence(2) //must be run from the client where "oc" is installed
   @Test
   public void scale_down() {
      scalingTester.scaleDownStatefulSet(0, SERVICE_NAME);
   }

   @RunAsClient
   @InSequence(3) //must be run from the client where "oc" is installed
   @Test
   public void scale_up() {
      scalingTester.scaleUpStatefulSet(1, SERVICE_NAME);
   }

   @InSequence(4)
   @Test
   public void get_on_persisted_cache() throws Exception {
      try (LazyRemoteCacheManager lazyRemote = HotRodUtil.lazyRemoteCacheManager()) {
         lazyRemote
            .andThen(remote -> remote.getCache(CACHE_NAME))
            .andThen(getFromCache())
            .apply(HotRodConfiguration.secured().apply(SERVICE_NAME));
      }
   }

   private Function<RemoteCacheManager, RemoteCacheManager> createDataCache() {
      XMLStringConfiguration xml = new XMLStringConfiguration(String.format(
         "<infinispan>" +
            "<cache-container>" +
               "<distributed-cache name=\"%1$s\">" +
                  "<persistence passivation=\"false\">" +
                     "<file-store " +
                        "shared=\"false\" " +
                        "fetch-state=\"true\" " +
                        "path=\"${jboss.server.data.dir}/datagrid-infinispan/%1$s\"" +
                     "/>" +
                  "</persistence>" +
               "</distributed-cache>" +
            "</cache-container>" +
         "</infinispan>",
         CACHE_NAME
      ));

      return HotRodTester.createCache(xml, CACHE_NAME);
   }

   private Function<RemoteCache<Object, Object>, RemoteCache<Object, Object>> getFromCache() {
      return remoteCache -> {
         assertEquals("value", remoteCache.get("hotRodKey"));
         return remoteCache;
      };
   }

   private Function<RemoteCache<Object, Object>, RemoteCache<Object, Object>> putOnCache() {
      return remoteCache -> {
         remoteCache.put("hotRodKey", "value");
         return remoteCache;
      };
   }

}
