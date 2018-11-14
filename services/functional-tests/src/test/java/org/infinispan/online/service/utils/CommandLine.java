package org.infinispan.online.service.utils;

import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.commons.util.Either;
import org.jboss.dmr.ModelNode;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public final class CommandLine {

   private static final Log log = LogFactory.getLog(CommandLine.class);

   private CommandLine() {
      // Avoid instantiation
   }

   public static final class Ok {

      final String output;

      Ok(String output) {
         this.output = output;
      }

   }

   public static final class Err {

      final int errorCode;
      final RuntimeException error;

      Err(int errorCode, RuntimeException error) {
         this.errorCode = errorCode;
         this.error = error;
      }

   }

   public static Function<String, Either<Err, Ok>> invoke() {
      return cmd -> {
         try {
            log.infof("Invoking on command line: '%s'", cmd);

            ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
            Process p = pb.start();
            int resultCode = p.waitFor();
            String output = getStream(p.getInputStream());
            String error = getStream(p.getErrorStream());

            return resultCode != 0
               ? Either.newLeft(new Err(resultCode, new RuntimeException(error)))
               : Either.newRight(new Ok(output));
         } catch (Exception e) {
            return Either.newLeft(new Err(-1, new RuntimeException(e)));
         }
      };
   }

   public static Function<Either<Err, Ok>, Ok> throwIfError() {
      return result -> {
         switch (result.type()) {
            case LEFT:
               throw result.left().error;
            case RIGHT:
               return result.right();
            default:
               throw new IllegalStateException("No other possible type: " + result.type());
         }
      };
   }

   public static Function<Ok, ModelNode> toMgmtResult() {
      return ok -> {
         log.infof("Received result:%n%s", ok.output);

         final ModelNodeResult result =
            new ModelNodeResult(ModelNode.fromString(ok.output));
         result.assertDefinedValue();
         return result.value();
      };
   }

   private static Function<ModelNode, List<String>> toStringList() {
      return result ->
         result.asList().stream()
            .map(ModelNode::asString)
            .collect(Collectors.toList());
   }

   public static void scaleStatefulSet(String statefulSetName, int replicas) {
      CommandLine.invoke()
         .andThen(CommandLine.throwIfError())
         .apply(
            String.format(
               "oc scale statefulset %s --replicas=%s"
               , statefulSetName, replicas
            )
         );
   }

   public static int numOwners(String svcName) {
      return CommandLine.invoke()
         .andThen(CommandLine.throwIfError())
         .andThen(CommandLine.toMgmtResult())
         .andThen(ModelNode::asInt)
         .apply(
            String.format(
               "oc exec " +
                  "-it %s " +
                  "-- /opt/datagrid/bin/cli.sh " +
                  "--connect " +
                  "--commands=/subsystem=datagrid-infinispan/cache-container=clustered/configurations=CONFIGURATIONS/distributed-cache-configuration=default:read-attribute(name=owners)"
               , svcName
            )
         );
   }

   public static String evictionStrategy(String svcName) {
      return CommandLine.invoke()
         .andThen(CommandLine.throwIfError())
         .andThen(CommandLine.toMgmtResult())
         .andThen(ModelNode::asString)
         .apply(
            String.format(
               "oc exec " +
                  "-it %s " +
                  "-- /opt/datagrid/bin/cli.sh " +
                  "--connect " +
                  "--commands=/subsystem=datagrid-infinispan/cache-container=clustered/configurations=CONFIGURATIONS/distributed-cache-configuration=default/memory=OFF-HEAP/:read-attribute(name=strategy)"
               , svcName
            )
         );
   }

   public static String memory(String svcName) {
      return CommandLine.invoke()
         .andThen(CommandLine.throwIfError())
         .andThen(CommandLine.toMgmtResult())
         .andThen(CommandLine.toStringList())
         .andThen(results -> {
            assertEquals(1, results.size());
            return results.get(0);
         })
         .apply(
            String.format(
               "oc exec " +
                  "-it %s " +
                  "-- /opt/datagrid/bin/cli.sh " +
                  "--connect " +
                  "--commands=/subsystem=datagrid-infinispan/cache-container=clustered/configurations=CONFIGURATIONS/distributed-cache-configuration=default:read-children-names(child-type=memory)"
               , svcName
            )
         );
   }

   public static void newApp(String appName, String params, String templateName, String imageName) {
      CommandLine.invoke()
         .andThen(CommandLine.throwIfError())
         .apply(
            String.format(
               "oc new-app %s " +
                  "-p APPLICATION_NAME=%s " +
                  "-p APPLICATION_USER=test " +
                  "-p APPLICATION_PASSWORD=test " +
                  "-p IMAGE=%s " +
                  "%s"
               , templateName, appName, imageName, params
            )
         );
   }

   public static void deleteApp(String appName) {
      CommandLine.invoke()
         .andThen(CommandLine.throwIfError())
         .apply(
            String.format(
               "oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts " +
                  "--selector=application=%s"
               , appName
            )
         );
   }

   private static String getStream(InputStream stream) throws IOException {
      final BufferedReader br = new BufferedReader(new InputStreamReader(stream));
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
         sb.append(line).append("\n");
      }
      return sb.toString();
   }

}