package org.infinispan.online.service.datagrid;

import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.online.service.common.BasePermanentCacheTest;
import org.junit.runner.RunWith;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class PermanentCacheTest extends BasePermanentCacheTest {

   public PermanentCacheTest() {
      super("datagrid-service");
   }

}
