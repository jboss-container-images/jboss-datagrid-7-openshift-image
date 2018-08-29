package org.infinispan.online.service.datagrid;

import io.fabric8.openshift.client.OpenShiftClient;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class PermanentCacheTest {

   private static final String SERVICE_NAME = "datagrid-service";

   URL hotRodService;
   URL restService;
   OpenShiftClient client = OpenShiftClientCreator.getClient();

   ReadinessCheck readinessCheck = new ReadinessCheck();
   OpenShiftHandle handle = new OpenShiftHandle(client);

   ScalingTester scalingTester = new ScalingTester();
   OpenShiftCommandlineClient commandlineClient = new OpenShiftCommandlineClient();

   @Deployment
   public static Archive<?> deploymentApp() {
      return ShrinkWrap
         .create(WebArchive.class, "test.war")
         .addAsLibraries(DeploymentHelper.testLibs())
         .addPackage(DatagridServiceTest.class.getPackage())
         .addPackage(ReadinessCheck.class.getPackage())
         .addPackage(ScalingTester.class.getPackage())
         .addPackage(HotRodTester.class.getPackage());
   }

   @Before
   public void before() throws MalformedURLException {
      readinessCheck.waitUntilAllPodsAreReady(client);
      hotRodService = handle.getServiceWithName(SERVICE_NAME + "-hotrod");
      restService = handle.getServiceWithName(SERVICE_NAME + "-https");
      URL hotRodService = handle.getServiceWithName(SERVICE_NAME + "-hotrod");
   }

   @InSequence(1)
   @Test
   public void create_named_cache() throws Exception {
      URL hotRodService = handle.getServiceWithName(SERVICE_NAME + "-hotrod");
      HotRodTester hotRodTester = new HotRodTester(SERVICE_NAME, hotRodService, client);

      hotRodTester.createNamedCache("custom", "replicated");
      hotRodTester.namedCachePutGetTest("custom");
   }

   @RunAsClient
   @InSequence(2) //must be run from the client where "oc" is installed
   @Test
   public void scale_down() {
      scalingTester.scaleDownStatefulSet(0, SERVICE_NAME, client, commandlineClient, readinessCheck);
   }

   @RunAsClient
   @InSequence(3) //must be run from the client where "oc" is installed
   @Test
   public void scale_up() {
      scalingTester.scaleUpStatefulSet(1, SERVICE_NAME, client, commandlineClient, readinessCheck);
   }

   @InSequence(4)
   @Test
   public void use_named_cache() throws Exception {
      URL hotRodService = handle.getServiceWithName(SERVICE_NAME + "-hotrod");
      HotRodTester hotRodTester = new HotRodTester(SERVICE_NAME, hotRodService, client);

      hotRodTester.namedCachePutGetTest("custom");
   }

}
