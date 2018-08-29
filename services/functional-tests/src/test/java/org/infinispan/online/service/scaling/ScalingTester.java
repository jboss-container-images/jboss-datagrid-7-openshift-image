package org.infinispan.online.service.scaling;

import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.utils.OpenShiftCommandlineClient;
import org.infinispan.online.service.utils.ReadinessCheck;
import org.infinispan.online.service.utils.Waiter;

import io.fabric8.openshift.client.OpenShiftClient;

public class ScalingTester {

   public void scaleUpStatefulSet(int numReplicas, String statefulSetName, OpenShiftClient client, OpenShiftCommandlineClient commandlineClient, ReadinessCheck readinessCheck) {
      commandlineClient.scaleStatefulSet(statefulSetName, numReplicas);
      readinessCheck.waitUntilTargetNumberOfReplicasAreReady(statefulSetName, numReplicas, client);
   }

   public void waitForClusterToForm(HotRodTester hotRodTester) {
      Waiter waiter = new Waiter();
      //Even though the Pods are ready, there might be bad timing in the discovery protocol
      //and Pods didn't manage to form a cluster yet.
      //We need to wait in a loop to see it that really happened.
      waiter.waitFor(() -> hotRodTester.getNumberOfNodesInTheCluster() == 2);
   }

   public void scaleDownStatefulSet(int numReplicas, String statefulSetName, OpenShiftClient client, OpenShiftCommandlineClient commandlineClient, ReadinessCheck readinessCheck) {
      commandlineClient.scaleStatefulSet(statefulSetName, numReplicas);
      readinessCheck.waitUntilTargetNumberOfReplicasAreReady(statefulSetName, 1, client);
   }
}
