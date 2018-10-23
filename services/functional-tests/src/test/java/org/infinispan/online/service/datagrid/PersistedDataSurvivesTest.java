package org.infinispan.online.service.datagrid;

import io.fabric8.openshift.client.OpenShiftClient;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.commons.configuration.XMLStringConfiguration;
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
public class PersistedDataSurvivesTest {

   private static final String SERVICE_NAME = "datagrid-service";
   private static final String CACHE_NAME = "custom-persistent";

   private OpenShiftClient client = OpenShiftClientCreator.getClient();

   private ReadinessCheck readinessCheck = new ReadinessCheck();
   private OpenShiftHandle handle = new OpenShiftHandle(client);

   private ScalingTester scalingTester = new ScalingTester();
   private OpenShiftCommandlineClient commandlineClient = new OpenShiftCommandlineClient();

   @Deployment
   public static Archive<?> deploymentApp() {
      return ShrinkWrap
         .create(WebArchive.class, "test.war")
         .addAsLibraries(DeploymentHelper.testLibs())
         .addPackage(ReadinessCheck.class.getPackage())
         .addPackage(ScalingTester.class.getPackage())
         .addPackage(HotRodTester.class.getPackage());
   }

   @Before
   public void before() {
      readinessCheck.waitUntilAllPodsAreReady(client);
   }

   @InSequence(1)
   @Test
   public void put_on_persisted_cache() throws Exception {
      URL hotRodService = handle.getServiceWithName(SERVICE_NAME + "-hotrod");
      HotRodTester hotRodTester = new HotRodTester(SERVICE_NAME, hotRodService, client);

      String cacheCfg = String.format(
         "<infinispan>" +
            "<cache-container>" +
               "<distributed-cache name=\"%1$s\">" +
                  "<persistence passivation=\"false\">" +
                     "<file-store " +
                        "shared=\"false\" " +
                        "fetch-state=\"true\" " +
                        "path=\"${jboss.server.data.dir}/datagrid-infinispan/%1$s\"" +
                     "/>" +
                  "</persistence>" +
               "</distributed-cache>" +
            "</cache-container>" +
         "</infinispan>",
         CACHE_NAME
      );

      hotRodTester.createNamedCache("custom-persistent", new XMLStringConfiguration(cacheCfg));
      hotRodTester.namedCachePutGetTest("custom-persistent");
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
   public void get_on_persisted_cache() throws Exception {
      URL hotRodService = handle.getServiceWithName(SERVICE_NAME + "-hotrod");
      HotRodTester hotRodTester = new HotRodTester(SERVICE_NAME, hotRodService, client);

      hotRodTester.namedCacheGetTest("custom-persistent");
   }

}
