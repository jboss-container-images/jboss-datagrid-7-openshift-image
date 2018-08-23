package org.infinispan.online.service.utils;

public class OpenShiftCommandlineClient {

   private CommandlineClient commandlineClient = new CommandlineClient();

   public void scaleStatefulSet(String statefulSetName, int replicas) {
      commandlineClient.invoke("oc scale statefulset " + statefulSetName + " --replicas=" + replicas);
   }

}
