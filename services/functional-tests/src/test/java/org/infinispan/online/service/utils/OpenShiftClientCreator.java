package org.infinispan.online.service.utils;

import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

public class OpenShiftClientCreator {

   //TODO: Get namespace and username from env properties
   private static final OpenShiftClient CLIENT = new DefaultOpenShiftClient(new ConfigBuilder()
         .withNamespace("myproject")
         .withUsername("developer")  //auth token is read from environment property KUBERNETES_AUTH_TOKEN
         .build());

   private OpenShiftClientCreator() {
   }

   public static OpenShiftClient getClient() {
      return CLIENT;
   }
}
