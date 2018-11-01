package org.infinispan.online.service.utils;

import org.infinispan.commons.util.Either;
import org.infinispan.online.service.utils.CommandLine.Err;
import org.infinispan.online.service.utils.CommandLine.Ok;
import org.jboss.dmr.ModelNode;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;

public class CommandLines {

   private CommandLines() {
   }

   public static void scaleStatefulSet(String statefulSetName, int replicas) {
      CommandLine.invoke().apply(
         String.format(
            "oc scale statefulset %s --replicas=%s"
            , statefulSetName, replicas
         )
      );
   }

   public static int numOwners(String svcName) {
      final Either<Err, Ok> result = CommandLine.invoke().apply(
         String.format(
            "oc exec " +
               "-it %s " +
               "-- /opt/datagrid/bin/cli.sh " +
               "--connect " +
               "--commands=/subsystem=datagrid-infinispan/cache-container=clustered/configurations=CONFIGURATIONS/distributed-cache-configuration=default:read-attribute(name=owners)"
            , svcName
         )
      );

      switch (result.type()) {
         case LEFT:
            throw result.left().error;
         case RIGHT:
            final ModelNodeResult numOwners = new ModelNodeResult(ModelNode.fromString(result.right().output));
            numOwners.assertDefinedValue();
            return Integer.parseInt(numOwners.stringValue());
         default:
            throw new IllegalStateException("No other possible type: " + result.type());
      }
   }

}
