package org.infinispan.online.service.datagrid;

import java.net.URL;

import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.online.service.common.BasePermanentCacheTest;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.junit.runner.RunWith;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class PermanentCacheTest extends BasePermanentCacheTest {

   public PermanentCacheTest() {
      super("datagrid-service");
   }

   @Override
   public void create_named_cache() throws Exception {
      URL hotRodService = handle.getServiceWithName(serviceName + "-hotrod");
      HotRodTester hotRodTester = new HotRodTester(serviceName, hotRodService, client);

      String cacheName = getCacheName();
      String cacheCfg = String.format(
            "<infinispan>" +
                  "<cache-container>" +
                      "<replicated-cache name=\"%1$s\"/>" +
                  "</cache-container>" +
            "</infinispan>",
            cacheName
      );

      hotRodTester.createNamedCache(cacheName, new XMLStringConfiguration(cacheCfg));
      hotRodTester.namedCachePutGetTest(cacheName);
   }
}
