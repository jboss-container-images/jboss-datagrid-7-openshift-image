package org.infinispan.online.service.caching;

import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.online.service.common.BasePermanentCacheTest;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.junit.runner.RunWith;

import java.util.function.Function;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class PermanentCacheTest extends BasePermanentCacheTest {

   public PermanentCacheTest() {
      super("cache-service");
   }

   @Override
   protected Function<RemoteCacheManager, RemoteCacheManager> createDataCache(String cacheName) {
      return HotRodTester.createCache("default", cacheName);
   }

}
