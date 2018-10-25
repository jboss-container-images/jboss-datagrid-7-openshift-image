package org.infinispan.online.service.endpoint;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.online.service.utils.Waiter;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.remote.client.MarshallerRegistration;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import java.io.IOException;
import java.util.function.Function;

import static junit.framework.TestCase.assertNull;
import static org.infinispan.online.service.utils.TestObjectCreator.generateConstBytes;
import static org.infinispan.online.service.utils.XmlUtils.prettyXml;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class HotRodTester {

   private static final Log log = LogFactory.getLog(HotRodTester.class);

   private HotRodTester() {
   }

   public static Function<RemoteCacheManager, RemoteCacheManager> createCache(
      XMLStringConfiguration xmlCfg, String cacheName
   ) {
      return remote -> {
         log.debugf("Create cache XML with: %n%s", prettyXml(xmlCfg.toXMLString()));

         remote.administration().removeCache(cacheName);
         remote.administration()
            .withFlags(CacheContainerAdmin.AdminFlag.PERMANENT)
            .createCache(cacheName, xmlCfg);

         return remote;
      };
   }

   public static Function<RemoteCacheManager, RemoteCacheManager> createCache(
      String templateName, String cacheName
   ) {
      return remote -> {
         remote.administration().removeCache(cacheName);
         remote.administration()
            .withFlags(CacheContainerAdmin.AdminFlag.PERMANENT)
            .createCache(cacheName, templateName);

         return remote;
      };
   }

   public static Function<RemoteCache<Object, Object>, RemoteCache<Object, Object>> getFromCache() {
      return remoteCache -> {
         assertEquals("value", remoteCache.get("hotRodKey"));
         return remoteCache;
      };
   }

   public static Function<RemoteCache<Object, Object>, RemoteCache<Object, Object>> putOnCache() {
      return remoteCache -> {
         remoteCache.put("hotRodKey", "value");
         return remoteCache;
      };
   }

   public static Function<RemoteCache<String, Object>, RemoteCache<String, Object>> putGetCache() {
      return remoteCache ->
         stringPutGetTest()
            .andThen(stringUpdateGetTest())
            .andThen(stringRemoveTest())
            .andThen(intPutGetTest())
            .andThen(byteArrayPutGetTest())
            .apply(remoteCache);
   }

   public static Function<RemoteCacheManager, RemoteCacheManager> waitForClusterToForm() {
      return remote -> {
         Waiter waiter = new Waiter();
         //Even though the Pods are ready, there might be bad timing in the discovery protocol
         //and Pods didn't manage to form a cluster yet.
         //We need to wait in a loop to see it that really happened.
         waiter.waitFor(() -> getNumberOfNodesInTheCluster(remote) == 2);

         return remote;
      };
   }

   private static int getNumberOfNodesInTheCluster(RemoteCacheManager remote) {
      RemoteCache<String, String> cache = remote.getCache();
      try {
         Thread.sleep(1000 * 5);
      } catch (InterruptedException ignore) {
      }
      int numberOfNodes = cache.getCacheTopologyInfo().getSegmentsPerServer().keySet().size();
      System.err.println("NUMBER OF NODES=" + numberOfNodes);
      return numberOfNodes;
   }

   private static Function<RemoteCache<String, Object>, RemoteCache<String, Object>> byteArrayPutGetTest() {
      return remoteCache -> {
         //given
         byte[] byteArrayValue = generateConstBytes(4096);
         //when
         remoteCache.put("hotRodKey", byteArrayValue);
         //then
         assertArrayEquals(byteArrayValue, (byte[]) remoteCache.get("hotRodKey"));

         return remoteCache;
      };
   }

   private static Function<RemoteCache<String, Object>, RemoteCache<String, Object>> intPutGetTest() {
      return remoteCache -> {
         Integer intValue = 5;
         //when
         remoteCache.put("hotRodKey", intValue);
         //then
         assertEquals(intValue, remoteCache.get("hotRodKey"));

         return remoteCache;
      };
   }

   private static Function<RemoteCache<String, Object>, RemoteCache<String, Object>> stringRemoveTest() {
      return remoteCache -> {
         //given
         remoteCache.put("hotRodKey", "value");
         //when
         remoteCache.remove("hotRodKey");
         //then
         assertNull(remoteCache.get("hotRodKey"));

         return remoteCache;
      };
   }

   private static Function<RemoteCache<String, Object>, RemoteCache<String, Object>> stringPutGetTest() {
      return remoteCache -> {
         //when
         remoteCache.put("hotRodKey", "value");
         //then
         assertEquals("value", remoteCache.get("hotRodKey"));

         return remoteCache;
      };
   }

   private static Function<RemoteCache<String, Object>, RemoteCache<String, Object>> stringUpdateGetTest() {
      return remoteCache -> {
         //when
         remoteCache.put("hotRodKey", "value");
         remoteCache.put("hotRodKey", "newValue");
         //then
         assertEquals("newValue", remoteCache.get("hotRodKey"));

         return remoteCache;
      };
   }

   public static Function<RemoteCache<String, byte[]>, RemoteCache<String, byte[]>> evictionTest() {
      return byteArrayCache -> {
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

         return byteArrayCache;
      };
   }

}