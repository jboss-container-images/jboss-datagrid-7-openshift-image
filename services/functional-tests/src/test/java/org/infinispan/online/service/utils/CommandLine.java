package org.infinispan.online.service.utils;

import org.infinispan.commons.util.Either;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Function;

public final class CommandLine {

   private CommandLine() {
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
