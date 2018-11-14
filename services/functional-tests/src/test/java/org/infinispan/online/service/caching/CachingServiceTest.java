package org.infinispan.online.service.caching;


import io.fabric8.kubernetes.api.model.Pod;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.client.hotrod.CacheTopologyInfo;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.online.service.endpoint.HotRodConfiguration;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.endpoint.HotRodUtil;
import org.infinispan.online.service.endpoint.HotRodUtil.LazyRemoteCacheManager;
import org.infinispan.online.service.endpoint.RESTTester;
import org.infinispan.online.service.scaling.ScalingTester;
import org.infinispan.online.service.utils.CommandLine;
import org.infinispan.online.service.utils.DeploymentHelper;
import org.infinispan.online.service.utils.OpenShiftHandle;
import org.infinispan.online.service.utils.ReadinessCheck;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class CachingServiceTest {

   private static final String SERVICE_NAME = "cache-service";

   private URL restService;
   private RESTTester restTester = new RESTTester(SERVICE_NAME);

   private ReadinessCheck readinessCheck = new ReadinessCheck();
   private OpenShiftHandle handle = new OpenShiftHandle();

   @Deployment
   public static Archive<?> deploymentApp() {
      return ShrinkWrap
            .create(WebArchive.class, "test.war")
            .addAsLibraries(DeploymentHelper.testLibs())
            .addPackage(CachingServiceTest.class.getPackage())
            .addPackage(ReadinessCheck.class.getPackage())
            .addPackage(ScalingTester.class.getPackage())
            .addPackage(HotRodTester.class.getPackage());
   }

   @Before
   public void before() throws MalformedURLException {
      readinessCheck.waitUntilAllPodsAreReady();
      restService = handle.getServiceWithName(SERVICE_NAME + "-https");
   }

   @Test
   public void should_read_and_write_through_rest_endpoint() {
      restTester.putGetRemoveTest(restService);
   }

   @Test(expected = HotRodClientException.class)
   public void should_default_cache_be_protected_via_hot_rod() {
      try (LazyRemoteCacheManager lazyRemote = HotRodUtil.lazyRemoteCacheManager()) {
         lazyRemote
            .andThen(RemoteCacheManager::<String, Object>getCache)
            .andThen(HotRodTester.putGetCache())
            .apply(HotRodConfiguration.encrypted().apply(SERVICE_NAME));
      }
   }

   @Test
   public void should_read_and_write_through_hotrod_endpoint() {
      try (LazyRemoteCacheManager lazyRemote = HotRodUtil.lazyRemoteCacheManager()) {
         lazyRemote
            .andThen(RemoteCacheManager::<String, Object>getCache)
            .andThen(HotRodTester.putGetCache())
            .apply(HotRodConfiguration.secured().apply(SERVICE_NAME));
      }
   }

   @Test(timeout = 600000)
   public void should_put_entries_until_first_one_gets_evicted() {
      try (LazyRemoteCacheManager lazyRemote = HotRodUtil.lazyRemoteCacheManager()) {
         lazyRemote
            .andThen(RemoteCacheManager::<String, byte[]>getCache)
            .andThen(HotRodTester.evictionTest())
            .apply(HotRodConfiguration.secured().apply(SERVICE_NAME));
      }
   }

   @Test
   public void only_default_cache_should_be_available() {
      try (LazyRemoteCacheManager lazyRemote = HotRodUtil.lazyRemoteCacheManager()) {
         lazyRemote
            .andThen(remote -> {
               assertNull(remote.getCache("memcachedCache"));
               assertNull(remote.getCache("nonExistent"));
               return remote;
            })
            .apply(HotRodConfiguration.secured().apply(SERVICE_NAME));
      }

      restTester.testCacheAvailability(restService, "nonExistent", false);
      restTester.testCacheAvailability(restService, "memcachedCache", false);
   }

   @Test
   public void should_default_cache_be_protected_via_REST() throws IOException {
      restTester.testIfEndpointIsProtected(restService);
   }

   @Test
   public void hotrod_should_see_all_pods() throws MalformedURLException {
      List<Pod> pods = handle.getPodsWithLabel("application", SERVICE_NAME);
      try (LazyRemoteCacheManager lazyRemote = HotRodUtil.lazyRemoteCacheManager()) {
         lazyRemote
            .andThen(RemoteCacheManager::<String, String[]>getCache)
            .andThen(remoteCache -> {
               CacheTopologyInfo topology = remoteCache.getCacheTopologyInfo();

               List<String> podIPs = pods.stream()
                  .map(pod -> pod.getStatus().getPodIP())
                  .sorted()
                  .collect(Collectors.toList());
               List<String> cacheNodeIPs = topology.getSegmentsPerServer().keySet().stream()
                  .map(addr -> ((InetSocketAddress) addr).getHostName())
                  .sorted()
                  .collect(Collectors.toList());

               assertThat(cacheNodeIPs).isEqualTo(podIPs);

               return remoteCache;
            })
            .apply(HotRodConfiguration.secured().apply(SERVICE_NAME));
      }
   }

   @RunAsClient
   @Test
   public void should_have_1_num_owners() {
      String podName = SERVICE_NAME + "-0";
      assertEquals(1, CommandLine.numOwners(podName));
   }

   @RunAsClient
   @Test
   public void should_store_data_off_heap() {
      String podName = SERVICE_NAME + "-0";
      assertEquals("OFF-HEAP", CommandLine.memory(podName));
   }

}
