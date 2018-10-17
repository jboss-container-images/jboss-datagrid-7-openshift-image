package org.infinispan.online.service.common;

import java.net.MalformedURLException;
import java.net.URL;

import org.infinispan.online.service.caching.CachingServiceTest;
import org.infinispan.online.service.datagrid.DatagridServiceTest;
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

import io.fabric8.openshift.client.OpenShiftClient;

public abstract class BasePermanentCacheTest {

   protected final String serviceName;
   protected final OpenShiftClient client = OpenShiftClientCreator.getClient();
   protected final ReadinessCheck readinessCheck = new ReadinessCheck();
   protected final OpenShiftHandle handle = new OpenShiftHandle(client);
   protected final ScalingTester scalingTester = new ScalingTester();
   protected final OpenShiftCommandlineClient commandlineClient = new OpenShiftCommandlineClient();

   URL hotRodService;
   URL restService;

   public BasePermanentCacheTest(String serviceName) {
      this.serviceName = serviceName;
   }

   @Deployment
   public static Archive<?> deploymentApp() {
      return ShrinkWrap
         .create(WebArchive.class, "test.war")
         .addAsLibraries(DeploymentHelper.testLibs())
         .addPackage(DatagridServiceTest.class.getPackage())
         .addPackage(CachingServiceTest.class.getPackage())
         .addPackage(ReadinessCheck.class.getPackage())
         .addPackage(ScalingTester.class.getPackage())
         .addPackage(HotRodTester.class.getPackage())
         .addPackage(BasePermanentCacheTest.class.getPackage());
   }

   @Before
   public void before() throws MalformedURLException {
      readinessCheck.waitUntilAllPodsAreReady(client);
      hotRodService = handle.getServiceWithName(serviceName + "-hotrod");
      restService = handle.getServiceWithName(serviceName + "-https");
   }

   @InSequence(1)
   @Test
   public abstract void create_named_cache() throws Exception;

   @RunAsClient
   @InSequence(2) //must be run from the client where "oc" is installed
   @Test
   public void scale_down() {
      System.out.printf("scale_down('%s')%n", serviceName);

      scalingTester.scaleDownStatefulSet(0, serviceName, client, commandlineClient, readinessCheck);
   }

   @RunAsClient
   @InSequence(3) //must be run from the client where "oc" is installed
   @Test
   public void scale_up() {
      System.out.printf("scale_up('%s')%n", serviceName);

      scalingTester.scaleUpStatefulSet(1, serviceName, client, commandlineClient, readinessCheck);
   }

   @InSequence(4)
   @Test
   public void use_named_cache() throws Exception {
      System.out.printf("use_named_cache('%s')%n", serviceName);

      URL hotRodService = handle.getServiceWithName(serviceName + "-hotrod");
      HotRodTester hotRodTester = new HotRodTester(serviceName, hotRodService, client);

      String cacheName = getCacheName();

      hotRodTester.namedCachePutGetTest(cacheName);
   }

   protected String getCacheName() {
      return "custom-" + serviceName;
   }
}
