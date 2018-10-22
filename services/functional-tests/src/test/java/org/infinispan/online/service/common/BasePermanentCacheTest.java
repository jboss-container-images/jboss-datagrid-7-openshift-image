package org.infinispan.online.service.common;

import io.fabric8.openshift.client.OpenShiftClient;
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

import java.net.MalformedURLException;
import java.net.URL;

public abstract class BasePermanentCacheTest {

   private final String serviceName;

   URL hotRodService;
   URL restService;
   OpenShiftClient client = OpenShiftClientCreator.getClient();

   ReadinessCheck readinessCheck = new ReadinessCheck();
   OpenShiftHandle handle = new OpenShiftHandle(client);

   ScalingTester scalingTester = new ScalingTester();
   OpenShiftCommandlineClient commandlineClient = new OpenShiftCommandlineClient();

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
   public void create_named_cache() throws Exception {
      System.out.printf("create_named_cache('%s')%n", serviceName);

      URL hotRodService = handle.getServiceWithName(serviceName + "-hotrod");
      HotRodTester hotRodTester = new HotRodTester(serviceName, hotRodService, client);

      String cacheName = getCacheName(serviceName);

      hotRodTester.createNamedCache(cacheName, "replicated");
      hotRodTester.namedCachePutGetTest(cacheName);
   }

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

      String cacheName = getCacheName(serviceName);

      hotRodTester.namedCachePutGetTest(cacheName);
   }

   private static String getCacheName(String serviceName) {
      return "custom-" + serviceName;
   }

}
