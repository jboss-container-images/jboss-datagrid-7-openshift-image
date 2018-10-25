package org.infinispan.online.service.datagrid;

import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.online.service.common.BasePermanentCacheTest;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.junit.runner.RunWith;

import java.util.function.Function;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class PermanentCacheTest extends BasePermanentCacheTest {

   public PermanentCacheTest() {
      super("datagrid-service");
   }

   @Override
   protected Function<RemoteCacheManager, RemoteCacheManager> createDataCache(String cacheName) {
      XMLStringConfiguration cacheCfg = new XMLStringConfiguration(String.format(
         "<infinispan>" +
            "<cache-container>" +
               "<replicated-cache name=\"%1$s\"/>" +
            "</cache-container>" +
         "</infinispan>",
         cacheName
      ));

      return HotRodTester.createCache(cacheCfg, cacheName);
   }

}
