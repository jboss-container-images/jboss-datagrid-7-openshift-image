package org.infinispan.online.service.caching;


import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.scaling.ScalingTester;
import org.infinispan.online.service.utils.CommandLine;
import org.infinispan.online.service.utils.DeploymentHelper;
import org.infinispan.online.service.utils.ReadinessCheck;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;


@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class CustomCachingServiceTest {

   private static final String SERVICE_NAME = "custom-cache";
   private static final String TEMPLATE_NAME = "cache-service";
   private static final String IMAGE = "jboss-datagrid-7/datagrid73-openshift";

   private ReadinessCheck readinessCheck = new ReadinessCheck();

   @Deployment
   public static Archive<?> deploymentApp() {
      return ShrinkWrap
            .create(WebArchive.class, "test.war")
            .addAsLibraries(DeploymentHelper.testLibs())
            .addPackage(CustomCachingServiceTest.class.getPackage())
            .addPackage(ReadinessCheck.class.getPackage())
            .addPackage(ScalingTester.class.getPackage())
            .addPackage(HotRodTester.class.getPackage());
   }

   @Before
   public void before() {
      final int numPodsBefore = readinessCheck.getAllPodsSize();
      CommandLine.newApp(SERVICE_NAME, "-p REPLICATION_FACTOR=3 -p EVICTION_POLICY=reject -e SCRIPT_DEBUG=true", TEMPLATE_NAME, IMAGE);
      readinessCheck.waitUntilAllPodsAreReady(numPodsBefore + 1);
   }

   @After
   public void after() {
      CommandLine.deleteApp(SERVICE_NAME);
   }

   @RunAsClient
   @Test
   public void should_have_3_num_owners() {
      String podName = SERVICE_NAME + "-0";
      assertEquals(3, CommandLine.numOwners(podName));
   }

   @RunAsClient
   @Test
   public void should_have_reject_eviction_policy() {
      String podName = SERVICE_NAME + "-0";
      assertEquals("EXCEPTION", CommandLine.evictionStrategy(podName));
   }

}
