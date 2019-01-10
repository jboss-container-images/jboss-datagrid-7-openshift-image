package org.infinispan.online.service.utils;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

public class OpenShiftClientCreator {

   private static final OpenShiftClient CLIENT = new DefaultOpenShiftClient();

   private OpenShiftClientCreator() {
   }

   public static OpenShiftClient getClient() {
      return CLIENT;
   }
}
