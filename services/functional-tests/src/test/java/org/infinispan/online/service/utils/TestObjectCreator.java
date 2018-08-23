package org.infinispan.online.service.utils;


import java.util.Random;

public class TestObjectCreator {

   private static final Random random = new Random();

   public static byte[] generateConstBytes(int size) {
      byte[] array = new byte[size];
      for (int i = 0; i != array.length; i++) {
         array[i] = 'x';
      }
      return array;
   }

   public String getRandomString(int length) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < length; i++) {
         char c = (char) (random.nextInt((int) (Character.MAX_VALUE)));
         sb.append(c);
      }
      return sb.toString();
   }

}
