package org.infinispan.online.service.caching.capcity;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.SaslQop;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class CachingServiceCapacityTest {

   @Test
   public void performCapacityTest() throws Exception {
      Options opt = new OptionsBuilder()
            .include(this.getClass().getName() + ".*")
            .mode(Mode.AverageTime)
            .timeUnit(TimeUnit.MILLISECONDS)
            .warmupIterations(5)
            .measurementIterations(10)
            .threads(1)
            .forks(1)
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .build();

      new Runner(opt).run();
   }

   @State(Scope.Benchmark)
   public static class BenchmarkState {

      private static final String TRUST_STORE_PATH = BenchmarkState.class.getClassLoader().getResource("./keystore_client.jks").getPath();

      private RemoteCache<String, byte[]> remoteCache;

      private AtomicLong counter = new AtomicLong();

      @Setup
      public void setup() throws Exception {
         ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
         configurationBuilder.addServers("172.17.0.2:11222");
         configurationBuilder
               .security()
               .ssl()
               .enabled(true)
               .trustStoreFileName(TRUST_STORE_PATH)
               .trustStorePassword("secret".toCharArray())
               .authentication().enabled(true)
               .username("test")
               .password("test")
               .realm("ApplicationRealm")
               .saslMechanism("DIGEST-MD5")
               .saslQop(SaslQop.AUTH)
               .serverName("cache-service");
         RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configurationBuilder.build());
         remoteCache = remoteCacheManager.getCache("default");
      }

      @TearDown
      public void tearDown() throws Exception {
         CloseableIteratorSet<String> entriesToBeDeleted = remoteCache.keySet();
         entriesToBeDeleted.stream().forEach(k -> remoteCache.remove(k));
      }

      @Benchmark
      public void measure_put() throws Exception {
         byte[] randomValue = generateConstBytes(1024 * 1024);
         String key = "" + counter.getAndIncrement();
         remoteCache.put(key, randomValue);
      }
   }

   public static byte[] generateConstBytes(int size) {
      byte[] array = new byte[size];
      for (int i = 0; i != array.length; i++) {
         array[i] = 'x';
      }
      return array;
   }

}
