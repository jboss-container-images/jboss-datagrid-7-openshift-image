package org.infinispan.online.service.endpoint;

import static junit.framework.TestCase.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.infinispan.online.service.utils.TestObjectCreator.generateConstBytes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import org.infinispan.client.hotrod.CacheTopologyInfo;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.SaslQop;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.infinispan.online.service.utils.TrustStore;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.client.OpenShiftClient;

public class HotRodTester implements EndpointTester {

   private final String serviceName;
   private final String hotRodKey = "hotRodKey";
   private final TrustStore trustStore;
   private final RemoteCacheManager cacheManager;

   public HotRodTester(String serviceName, URL urlToService, OpenShiftClient client) {
      this.serviceName = serviceName;
      this.trustStore = new TrustStore(client, serviceName);
      this.cacheManager = getRemoteCacheManager(urlToService);
   }

   public void clear() {
      RemoteCache<String, String> defaultCache = cacheManager.getCache();
      CloseableIteratorSet<String> entriesToBeDeleted = defaultCache.keySet();
      entriesToBeDeleted.forEach(defaultCache::remove);
   }

   public void testBasicEndpointCapabilities(URL urlToService) {
      testBasicEndpointCapabilities(cacheManager);
   }

   private void testBasicEndpointCapabilities(RemoteCacheManager manager) {
      //given
      RemoteCache<String, String> defaultCache = manager.getCache();

      //when
      defaultCache.put("should_default_cache_be_accessible_via_hot_rod", "test");
      String valueObtainedFromTheCache = defaultCache.get("should_default_cache_be_accessible_via_hot_rod");

      //then
      assertThat(valueObtainedFromTheCache).isEqualTo("test");
   }

   @Override
   public void testIfEndpointIsProtected(URL urlToService) {
      testBasicEndpointCapabilities(getRemoteCacheManager(urlToService, false));
   }

   public void testPodsVisible(List<Pod> pods) {
      RemoteCache<String, String> defaultCache = cacheManager.getCache();
      CacheTopologyInfo topology = defaultCache.getCacheTopologyInfo();

      List<String> podIPs = pods.stream()
            .map(pod -> pod.getStatus().getPodIP())
            .sorted()
            .collect(Collectors.toList());
      List<String> cacheNodeIPs = topology.getSegmentsPerServer().keySet().stream()
            .map(addr -> ((InetSocketAddress) addr).getHostName())
            .sorted()
            .collect(Collectors.toList());

      assertThat(cacheNodeIPs).isEqualTo(podIPs);
   }

   public RemoteCache<String, String> getNamedCache(String cacheName) {
      return cacheManager.getCache(cacheName);
   }

   public int getNumberOfNodesInTheCluster() {
      RemoteCache<String, String> cache = cacheManager.getCache();
      try {
         Thread.sleep(1000 * 5);
      } catch (InterruptedException ignore) {
      }
      int numberOfNodes = cache.getCacheTopologyInfo().getSegmentsPerServer().keySet().size();
      System.err.println("NUMBER OF NODES=" + numberOfNodes);
      return numberOfNodes;
   }

   private RemoteCacheManager getRemoteCacheManager(URL urlToService) {
      return getRemoteCacheManager(urlToService, true);
   }

   private RemoteCacheManager getRemoteCacheManager(URL urlToService, boolean authenticate) {
      Configuration cachingServiceClientConfiguration = new ConfigurationBuilder()
            .addServer()
            .host(urlToService.getHost())
            .port(urlToService.getPort())
            .security()
            .ssl().enabled(true)
            .trustStoreFileName(trustStore.getPath())
            .trustStorePassword(TrustStore.TRUSTSTORE_PASSWORD)
            .authentication().enabled(authenticate)
            .username("test")
            .password("test")
            .realm("ApplicationRealm")
            .saslMechanism("DIGEST-MD5")
            .saslQop(SaslQop.AUTH)
            .serverName(serviceName)
            .build();

      return new RemoteCacheManager(cachingServiceClientConfiguration);
   }

   public void putGetTest() {
      stringPutGetTest();
      stringUpdateGetTest();
      stringRemoveTest();

      intPutGetTest();
      byteArrayPutGetTest();
   }

   private void byteArrayPutGetTest() {
      //given
      RemoteCache<String, byte[]> byteArrayCache = cacheManager.getCache();
      byte[] byteArrayValue = generateConstBytes(4096);
      //when
      byteArrayCache.put(hotRodKey, byteArrayValue);
      //then
      assertArrayEquals(byteArrayValue, byteArrayCache.get(hotRodKey));
   }

   private void intPutGetTest() {
      //given
      RemoteCache<String, Integer> intCache = cacheManager.getCache();
      Integer intValue = 5;
      //when
      intCache.put(hotRodKey, intValue);
      //then
      assertEquals(intValue, intCache.get(hotRodKey));
   }

   private void stringRemoveTest() {
      //given
      RemoteCache<String, String> stringCache = cacheManager.getCache();
      stringCache.put(hotRodKey, "value");
      //when
      stringCache.remove(hotRodKey);
      //then
      assertNull(stringCache.get(hotRodKey));
   }

   private void stringPutGetTest() {
      //given
      RemoteCache<String, String> stringCache = cacheManager.getCache();
      //when
      stringCache.put(hotRodKey, "value");
      //then
      assertEquals("value", stringCache.get(hotRodKey));
   }

   private void stringUpdateGetTest() {
      //given
      RemoteCache<String, String> stringCache = cacheManager.getCache();
      //when
      stringCache.put(hotRodKey, "value");
      stringCache.put(hotRodKey, "newValue");
      //then
      assertEquals("newValue", stringCache.get(hotRodKey));
   }

   public void evictionTest() {
      //given
      RemoteCache<String, byte[]> byteArrayCache = cacheManager.getCache();
      byte[] randomValue = generateConstBytes(1024 * 1024);
      int lastCacheSize;
      int currentCacheSize = -1;
      long counter = 0;

      //when
      do {
         lastCacheSize = currentCacheSize;
         String key = "key" + counter++;

         System.err.println(String.format("Put Key=%s, Val-length=%d", key, randomValue.length));

         byteArrayCache.put(key, randomValue);
         assertArrayEquals(randomValue, byteArrayCache.get(key));

         currentCacheSize = byteArrayCache.size();
      } while (currentCacheSize > lastCacheSize);
   }
}
