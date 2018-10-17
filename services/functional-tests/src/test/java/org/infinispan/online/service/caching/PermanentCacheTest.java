package org.infinispan.online.service.caching;

import java.net.URL;

import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.online.service.common.BasePermanentCacheTest;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.junit.runner.RunWith;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class PermanentCacheTest extends BasePermanentCacheTest {

   public PermanentCacheTest() {
      super("cache-service");
   }

   // TODO extend this method to ensure that custom cache configurations cannot be created once this feature
   // has been implemented in Infinispan
   @Override
   public void create_named_cache() throws Exception {
      URL hotRodService = handle.getServiceWithName(serviceName + "-hotrod");
      HotRodTester hotRodTester = new HotRodTester(serviceName, hotRodService, client);

      String cacheName = getCacheName();
      hotRodTester.createNamedCache(cacheName, "default");
      hotRodTester.namedCachePutGetTest(cacheName);
   }
}
