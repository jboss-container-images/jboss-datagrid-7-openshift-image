package org.infinispan.online.service.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CommandlineClient {

   class Result {
      private final int errorCode;
      private final String output;

      public Result(int errorCode, String output) {
         this.errorCode = errorCode;
         this.output = output;
      }

      public int getErrorCode() {
         return errorCode;
      }

      public String getOutput() {
         return output;
      }
   }

   public Result invokeAndReturnResult(String command) {
      try {
         ProcessBuilder pb = new ProcessBuilder(command.split(" "));
         Process p = pb.start();
         int resultCode = p.waitFor();
         BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

         StringBuilder sb = new StringBuilder();
         String line;
         while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
         }
         String output = sb.toString();
         return new Result(resultCode, output);
      } catch (Exception e) {
         throw new IllegalStateException("Command " + command + " failed.", e);
      }
   }

   public void invoke(String command) {
      Result result = invokeAndReturnResult(command);
      if (result.errorCode != 0) {
         throw new IllegalStateException("Expected error code 0 but got " + result.errorCode + ". Output: " + result.output);
      }
   }
}
