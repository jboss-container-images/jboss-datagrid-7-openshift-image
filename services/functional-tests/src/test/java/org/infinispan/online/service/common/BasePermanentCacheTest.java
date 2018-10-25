package org.infinispan.online.service.common;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.online.service.caching.CachingServiceTest;
import org.infinispan.online.service.datagrid.DatagridServiceTest;
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

import java.net.MalformedURLException;
import java.util.function.Function;

public abstract class BasePermanentCacheTest {

   private final String serviceName;
   private final String cacheName;

   private ReadinessCheck readinessCheck = new ReadinessCheck();
   private ScalingTester scalingTester = new ScalingTester();

   public BasePermanentCacheTest(String serviceName) {
      this.serviceName = serviceName;
      this.cacheName = getCacheName(serviceName);
   }

   @Deployment
   public static Archive<?> deploymentApp() {
      return ShrinkWrap
         .create(WebArchive.class, "test.war")
         .addAsLibraries(DeploymentHelper.testLibs())
         .addPackage(DatagridServiceTest.class.getPackage())
         .addPackage(CachingServiceTest.class.getPackage())
         .addPackage(ReadinessCheck.class.getPackage())
         .addPackage(ScalingTester.class.getPackage())
         .addPackage(HotRodTester.class.getPackage())
         .addPackage(BasePermanentCacheTest.class.getPackage());
   }

   @Before
   public void before() throws MalformedURLException {
      readinessCheck.waitUntilAllPodsAreReady();
   }

   @InSequence(1)
   @Test
   public void create_named_cache() throws Exception {
      String cacheName = getCacheName(serviceName);

      try (LazyRemoteCacheManager lazyRemote = HotRodUtil.lazyRemoteCacheManager()) {
         lazyRemote
            .andThen(createDataCache(cacheName))
            .andThen(remote -> remote.getCache(cacheName))
            .andThen(HotRodTester.putOnCache())
            .andThen(HotRodTester.getFromCache())
            .apply(HotRodConfiguration.secured().apply(serviceName));
      }
   }

   protected abstract Function<RemoteCacheManager, RemoteCacheManager> createDataCache(String cacheName);

   @RunAsClient
   @InSequence(2) //must be run from the client where "oc" is installed
   @Test
   public void scale_down() {
      scalingTester.scaleDownStatefulSet(0, serviceName);
   }

   @RunAsClient
   @InSequence(3) //must be run from the client where "oc" is installed
   @Test
   public void scale_up() {
      scalingTester.scaleUpStatefulSet(1, serviceName);
   }

   @InSequence(4)
   @Test
   public void use_named_cache() throws Exception {
      try (LazyRemoteCacheManager lazyRemote = HotRodUtil.lazyRemoteCacheManager()) {
         lazyRemote
            .andThen(remote -> remote.getCache(cacheName))
            .andThen(HotRodTester.putOnCache())
            .andThen(HotRodTester.getFromCache())
            .apply(HotRodConfiguration.secured().apply(serviceName));
      }
   }

   private static String getCacheName(String serviceName) {
      return "custom-" + serviceName;
   }

}
