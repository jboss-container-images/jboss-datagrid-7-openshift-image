package org.infinispan.online.service.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.openshift.client.OpenShiftClient;

public class OpenShiftHandle {

   private static final String HTTP_PROTOCOL = "https";

   private OpenShiftClient client;

   public OpenShiftHandle(OpenShiftClient client) {
      this.client = client;
   }

   public URL getServiceWithName(String resourceName) throws MalformedURLException {
      ServiceSpec spec = client.services().withName(resourceName).get().getSpec();

      if (spec != null) {
         String host;
         if (spec.getExternalName() != null && !spec.getExternalName().isEmpty()) {
            host = spec.getExternalName();
         } else {
            host = spec.getClusterIP();
         }
         //use HTTP as the protocol is unimportant and we don't want to register custom handlers
         return new URL(HTTP_PROTOCOL, host, spec.getPorts().get(0).getPort(), "/");
      }
      return null;
   }

   public List<Pod> getPodsWithLabel(String labelKey, String labelValue) throws MalformedURLException {
      return client.pods().withLabel(labelKey, labelValue).list().getItems();
   }
}
