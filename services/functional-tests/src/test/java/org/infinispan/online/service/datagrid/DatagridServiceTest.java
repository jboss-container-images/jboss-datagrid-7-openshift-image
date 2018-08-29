package org.infinispan.online.service.datagrid;

import io.fabric8.openshift.client.OpenShiftClient;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.endpoint.RESTTester;
import org.infinispan.online.service.scaling.ScalingTester;
import org.infinispan.online.service.utils.DeploymentHelper;
import org.infinispan.online.service.utils.OpenShiftClientCreator;
import org.infinispan.online.service.utils.OpenShiftHandle;
import org.infinispan.online.service.utils.ReadinessCheck;
import org.jboss.arquillian.container.test.api.Deployment;
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
public class DatagridServiceTest {

   private static final String SERVICE_NAME = "datagrid-service";

   private URL restService;
   private HotRodTester hotRodTester;
   private OpenShiftClient client = OpenShiftClientCreator.getClient();
   private RESTTester restTester = new RESTTester(SERVICE_NAME, client);

   private ReadinessCheck readinessCheck = new ReadinessCheck();
   private OpenShiftHandle handle = new OpenShiftHandle(client);

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
      restService = handle.getServiceWithName(SERVICE_NAME + "-https");
      URL hotRodService = handle.getServiceWithName(SERVICE_NAME + "-hotrod");
      hotRodTester = new HotRodTester(SERVICE_NAME, hotRodService, client);
   }

   @Test
   public void should_read_and_write_through_hotrod_endpoint() {
      hotRodTester.putGetTest();
   }

   @Test
   public void should_read_and_write_through_rest_endpoint() {
      restTester.putGetRemoveTest(restService);
   }

}
