package org.infinispan.online.service.utils;

import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

public class Waiter {

   private static final Log log = LogFactory.getLog(Waiter.class);

   public static final int MAX_NUMBER_OF_BACKOFFS = 10;
   public static final int DEFAULT_TIMEOUT = 240;
   public static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

   public void waitFor(BooleanSupplier condition) {
      waitFor(condition, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT, 1);
   }

   public void waitFor(BooleanSupplier condition, long timeout, TimeUnit unit, int numberOfSuccessfulChecks) {
      long endTime = TimeUnit.MILLISECONDS.convert(timeout, unit) + System.currentTimeMillis();
      long backoffCounter = 0;
      int successfulChecks = 0;
      while (notExpired(endTime)) {
         if (condition.getAsBoolean()) {
            if (++successfulChecks >= numberOfSuccessfulChecks) {
               break;
            }
         } else {
            successfulChecks = 0;
         }
         backoffCounter = backoffCounter + 1 > MAX_NUMBER_OF_BACKOFFS ? MAX_NUMBER_OF_BACKOFFS : backoffCounter + 1;
         LockSupport.parkNanos(++backoffCounter * 100_000_000);
      }
      assertTrue("Timed out waiting for a condition!", condition.getAsBoolean());
   }

   private boolean notExpired(long endTime) {
      final long currentTimeMillis = System.currentTimeMillis();
      final boolean notExpired = currentTimeMillis - endTime < 0;

      log.infof("Current time: %d", currentTimeMillis);
      log.infof("End time: %d", endTime);
      log.infof("Not expired? %b", notExpired);

      return notExpired;
   }

}
