package org.infinispan.online.service.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.openshift.client.OpenShiftClient;

public class OpenShiftHandle {

   private static final OpenShiftClient OPENSHIFT_CLIENT = OpenShiftClientCreator.getClient();
   private static final String HTTP_PROTOCOL = "https";

   public URL getServiceWithName(String resourceName) throws MalformedURLException {
      ServiceSpec spec = OPENSHIFT_CLIENT.services().withName(resourceName).get().getSpec();

      if (spec != null) {
         String host;
         if (spec.getExternalName() != null && !spec.getExternalName().isEmpty()) {
            host = spec.getExternalName();
         } else {
            host = spec.getClusterIP();
         }
         //use HTTP as the protocol is unimportant and we don't want to register custom handlers
         return new URL(HTTP_PROTOCOL, host, 8443, "/");
      }
      return null;
   }

   public List<Pod> getPodsWithLabel(String labelKey, String labelValue) throws MalformedURLException {
      return OPENSHIFT_CLIENT.pods().withLabel(labelKey, labelValue).list().getItems();
   }

   public String getVolumeTemplateSize(String claimName) {
      return OPENSHIFT_CLIENT.persistentVolumeClaims().withName(claimName).get().getSpec().getResources().getRequests().get("storage").getAmount();
   }
}
