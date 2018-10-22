package org.infinispan.online.service.caching;

import java.net.URL;

import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.scaling.ScalingTester;
import org.infinispan.online.service.utils.DeploymentHelper;
import org.infinispan.online.service.utils.OpenShiftClientCreator;
import org.infinispan.online.service.utils.OpenShiftCommandlineClient;
import org.infinispan.online.service.utils.OpenShiftHandle;
import org.infinispan.online.service.utils.ReadinessCheck;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.fabric8.openshift.client.OpenShiftClient;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class ScalingTest {

   private static final String SERVICE_NAME = "cache-service";

   ScalingTester scalingTester = new ScalingTester();
   OpenShiftCommandlineClient commandlineClient = new OpenShiftCommandlineClient();

   ReadinessCheck readinessCheck = new ReadinessCheck();
   OpenShiftClient client = OpenShiftClientCreator.getClient();
   OpenShiftHandle handle = new OpenShiftHandle(client);

   @Deployment
   public static Archive<?> deploymentApp() {
      return ShrinkWrap
            .create(WebArchive.class, "test.war")
            .addAsLibraries(DeploymentHelper.testLibs())
            .addPackage(ScalingTest.class.getPackage())
            .addPackage(ReadinessCheck.class.getPackage())
            .addPackage(ScalingTester.class.getPackage())
            .addPackage(HotRodTester.class.getPackage());
   }

   //must be run from the client where "oc" is installed
   @RunAsClient
   @InSequence(1)
   @Test
   public void scale_up() {
      scalingTester.scaleUpStatefulSet(2, SERVICE_NAME, client, commandlineClient, readinessCheck);
   }

   @InSequence(2)
   @Test
   public void should_discover_new_cluster_members_when_scaling_up() throws Exception {
      URL hotRodService = handle.getServiceWithName(SERVICE_NAME + "-hotrod");
      HotRodTester hotRodTester = new HotRodTester(SERVICE_NAME, hotRodService, client);
      scalingTester.waitForClusterToForm(hotRodTester);
   }

   //must be run from the client where "oc" is installed
   @RunAsClient
   @InSequence(3)
   @Test
   public void scale_down() {
      scalingTester.scaleDownStatefulSet(1, SERVICE_NAME, client, commandlineClient, readinessCheck);
   }
}
