package org.infinispan.online.service.scaling;

import org.infinispan.online.service.utils.OpenShiftCommandlineClient;
import org.infinispan.online.service.utils.ReadinessCheck;

public class ScalingTester {

   private static final OpenShiftCommandlineClient OPENSHIFT_CMD_LINE_CLIENT = new OpenShiftCommandlineClient();
   private static final ReadinessCheck READINESS_CHECK = new ReadinessCheck();

   public void scaleUpStatefulSet(int numReplicas, String statefulSetName) {
      OPENSHIFT_CMD_LINE_CLIENT.scaleStatefulSet(statefulSetName, numReplicas);
      READINESS_CHECK.waitUntilTargetNumberOfReplicasAreReady(statefulSetName, numReplicas);
   }

   public void scaleDownStatefulSet(int numReplicas, String statefulSetName) {
      OPENSHIFT_CMD_LINE_CLIENT.scaleStatefulSet(statefulSetName, numReplicas);
      READINESS_CHECK.waitUntilTargetNumberOfReplicasAreReady(statefulSetName, 1);
   }

}
