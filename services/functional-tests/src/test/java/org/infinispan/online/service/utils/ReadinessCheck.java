package org.infinispan.online.service.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.client.OpenShiftClient;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;

public class ReadinessCheck {

   private static final Log log = LogFactory.getLog(ReadinessCheck.class);

   private static final OpenShiftClient OPENSHIFT_CLIENT = OpenShiftClientCreator.getClient();

   private Waiter waiter = new Waiter();

   public void waitUntilTargetNumberOfReplicasAreReady(String podPartialName, int replicas) {
      waiter.waitFor(() -> {
         final int numPodsWithName = getPodsWithName(podPartialName).size();
         log.infof("Found %d pods with name '%s', required %d pods", numPodsWithName, podPartialName, replicas);
         return numPodsWithName == replicas;
      });
      waitUntilAllPodsAreReady();
      log.info("All Pods are ready and target replicas looks good.");
   }

   public void waitUntilAllPodsAreReady() {
      waiter.waitFor(() -> {
         Set<Pod> allPods = getAllPods();
         Set<Pod> notReadyPods = getNotReadyPods(allPods);
         Set<Pod> readyPods = getReadyPods(allPods);
         Set<Pod> unknownPods = getUnknownPods(allPods);

         final int numNotReady = notReadyPods.size();
         final int numReady = readyPods.size();
         final int numAll = allPods.size();
         final int numUnknown = unknownPods.size();

         if (numReady + numNotReady == numAll) {
            log.infof("Expected %d pods to be ready, but %d are not ready (all pods are %d)", numReady, numNotReady, numAll);
            log.infof("Not ready: %s", notReadyPods);
            return notReadyPods.isEmpty();
         }

         log.infof("There is a mismatch between ready and not ready Pods. numberReadyPods=%d, numberNotReadyPods=%d, numUnknown=%d, numberAllPods=%d", numReady, numNotReady, numUnknown, numAll);
         log.infof("Unknown Pods: %s", unknownPods);
         return false;
      }, Waiter.DEFAULT_TIMEOUT, Waiter.DEFAULT_TIMEOUT_UNIT, 1);
   }

   public void waitUntilAllPodsAreReady(int expectedNumPods) {
      waiter.waitFor(() -> {
         Set<Pod> allPods = getAllPods();
         Set<Pod> notReadyPods = getNotReadyPods(allPods);
         Set<Pod> readyPods = getReadyPods(allPods);
         Set<Pod> unknownPods = getUnknownPods(allPods);

         final int numNotReady = notReadyPods.size();
         final int numReady = readyPods.size();
         final int numAll = allPods.size();
         final int numUnknown = unknownPods.size();

         if (numReady != expectedNumPods) {
            log.infof("Expected %d pods to be ready: ready=%d,notReady=%d,all=%d", expectedNumPods, numReady, numNotReady, numAll);
            log.infof("Not ready: %s", notReadyPods);

            if (numReady + numNotReady != expectedNumPods) {
               log.infof("There is a mismatch between ready and not ready Pods. numberReadyPods=%d, numberNotReadyPods=%d, numUnknown=%d, numberAllPods=%d", numReady, numNotReady, numUnknown, numAll);
               log.infof("Unknown Pods: %s", unknownPods);
            }
            return false;
         }

         return true;
      }, Waiter.DEFAULT_TIMEOUT, Waiter.DEFAULT_TIMEOUT_UNIT, 1);
   }

   public int getAllPodsSize() {
      return getAllPods().size();
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

   private Set<Pod> getUnknownPods(Set<Pod> allPods) {
      return allPods.stream().filter(not(
         pod -> pod.getStatus().getConditions().stream().anyMatch(
            condition -> "Ready".equals(condition.getType())
         ))).collect(Collectors.toSet());
   }

   private Set<Pod> getPodsWithName(String name) {
      return OPENSHIFT_CLIENT.pods().list().getItems().stream()
            .filter(pod -> pod.getMetadata().getName().contains(name))
            .collect(Collectors.toSet());
   }

   public static <T> Predicate<T> not(Predicate<T> t) {
    return t.negate();
   }

}
