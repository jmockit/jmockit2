package org.jmockit.integration.junit4.internal;

import org.jmockit.*;
import org.jmockit.integration.internal.*;
import org.jmockit.internal.faking.*;
import org.jmockit.internal.state.*;

import org.junit.runner.*;
import org.junit.runner.notification.*;

import javax.annotation.*;

/**
 * Startup mock which works in conjunction with {@link JUnit4TestRunnerDecorator} to provide JUnit 4.5+ integration.
 * <p/>
 * This class is not supposed to be accessed from user code. JMockit will automatically load it at startup.
 */
public final class RunNotifierDecorator extends Fake<RunNotifier>
{
   public static void fireTestRunStarted(Invocation invocation, Description description)
   {
      RunNotifier it = invocation.getInvokedInstance();
      prepareToProceed(invocation);
      it.fireTestRunStarted(description);
   }

   private static void prepareToProceed(@Nonnull Invocation invocation)
   {
      ((FakeInvocation) invocation).prepareToProceedFromNonRecursiveMock();
   }

   public void fireTestStarted(Invocation invocation, Description description)
   {
      Class<?> currentTestClass = TestRun.getCurrentTestClass();

      if (currentTestClass != null) {
         Class<?> newTestClass = description.getTestClass();

         if (!currentTestClass.isAssignableFrom(newTestClass)) {
            TestRunnerDecorator.cleanUpMocksFromPreviousTestClass();
         }
      }

      prepareToProceed(invocation);

      RunNotifier it = invocation.getInvokedInstance();
      it.fireTestStarted(description);
   }

   public static void fireTestRunFinished(Invocation invocation, Result result)
   {
      TestRun.enterNoMockingZone();

      try {
         TestRunnerDecorator.cleanUpAllMocks();

         prepareToProceed(invocation);

         RunNotifier it = invocation.getInvokedInstance();
         it.fireTestRunFinished(result);
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }
}
