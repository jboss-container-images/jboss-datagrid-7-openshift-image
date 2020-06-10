package org.infinispan.online.service.scaling;

import org.infinispan.online.service.utils.CommandLine;
import org.infinispan.online.service.utils.ReadinessCheck;

public class ScalingTester {

   private static final ReadinessCheck READINESS_CHECK = new ReadinessCheck();

   public void scaleUpStatefulSet(int numReplicas, String statefulSetName) {
      CommandLine.scaleStatefulSet(statefulSetName, numReplicas);
      READINESS_CHECK.waitUntilTargetNumberOfReplicasAreReady(statefulSetName, numReplicas);
   }

   public void scaleDownStatefulSet(int numReplicas, String statefulSetName) {
      CommandLine.scaleStatefulSet(statefulSetName, numReplicas);
      READINESS_CHECK.waitUntilTargetNumberOfReplicasAreReady(statefulSetName, numReplicas);
   }

}
