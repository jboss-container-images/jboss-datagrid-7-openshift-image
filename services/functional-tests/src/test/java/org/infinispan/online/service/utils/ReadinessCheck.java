package org.infinispan.online.service.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.client.OpenShiftClient;

public class ReadinessCheck {

   private static final OpenShiftClient OPENSHIFT_CLIENT = OpenShiftClientCreator.getClient();

   private Waiter waiter = new Waiter();

   public void waitUntilTargetNumberOfReplicasAreReady(String podPartialName, int replicas) {
      waiter.waitFor(() -> getPodsWithName(podPartialName).size() == replicas);
      waitUntilAllPodsAreReady();
      System.out.println("All Pods are ready and target replicas looks good.");
   }

   public void waitUntilAllPodsAreReady() {
      waiter.waitFor(() -> {
         Set<Pod> allPods = getAllPods();
         Set<Pod> notReadyPods = getNotReadyPods(allPods);
         Set<Pod> readyPods = getReadyPods(allPods);

         if (readyPods.size() + notReadyPods.size() == allPods.size()) {
            return notReadyPods.isEmpty();
         }

         System.out.println("There is a mismatch between ready and not ready Pods. Skipping ready check.");
         System.out.println("All Pods: " + allPods);
         System.out.println("Not ready Pods: " + notReadyPods);
         System.out.println("Ready Pods: " + readyPods);
         return false;
      }, Waiter.DEFAULT_TIMEOUT, Waiter.DEFAULT_TIMEOUT_UNIT, 2);
   }

   private Set<Pod> getAllPods() {
      return new HashSet<>(OPENSHIFT_CLIENT.pods().list().getItems());
   }

   private Set<Pod> getNotReadyPods(Set<Pod> allPods) {
      return allPods.stream()
            .filter(pod -> pod.getStatus().getConditions().stream()
                  .filter(condition -> "Ready".equals(condition.getType()))
                  .findFirst()
                  .map(readyCondition -> "False".equals(readyCondition.getStatus()))
                  .orElse(false)
            ).collect(Collectors.toSet());
   }

   private Set<Pod> getReadyPods(Set<Pod> allPods) {
      return allPods.stream()
            .filter(pod -> pod.getStatus().getConditions().stream()
                  .filter(condition -> "Ready".equals(condition.getType()))
                  .findFirst()
                  .map(readyCondition -> "True".equals(readyCondition.getStatus()))
                  .orElse(false)
            ).collect(Collectors.toSet());
   }

   private Set<Pod> getPodsWithName(String name) {
      return OPENSHIFT_CLIENT.pods().list().getItems().stream()
            .filter(pod -> pod.getMetadata().getName().contains(name))
            .collect(Collectors.toSet());
   }
}
