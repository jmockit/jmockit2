package org.jmockit.internal.startup;

import org.jmockit.*;
import org.jmockit.integration.junit4.internal.*;
import org.jmockit.internal.reflection.*;
import org.jmockit.internal.util.*;

import javax.annotation.*;
import java.lang.instrument.*;
import java.util.*;

final class JMockitInitialization
{
   @Nonnull private final StartupConfiguration config;

   JMockitInitialization() { config = new StartupConfiguration(); }

   void initialize(@Nonnull Instrumentation inst)
   {
      preventEventualClassLoadingConflicts();
      applyInternalStartupFakesAsNeeded();
      applyUserSpecifiedStartupFakesIfAny();
   }

   @SuppressWarnings("ResultOfMethodCallIgnored")
   private static void preventEventualClassLoadingConflicts()
   {
      // Ensure the proper loading of data files by the JRE, whose names depend on calls to the System class,
      // which may get @Mocked.
      TimeZone.getDefault();
      Locale.getDefault();
      Currency.getInstance(Locale.CANADA);

      DefaultValues.computeForReturnType("()J");
      Utilities.calledFromSpecialThread();
   }

   private void applyInternalStartupFakesAsNeeded()
   {
      if (FakeFrameworkMethod.hasDependenciesInClasspath()) {
         new RunNotifierDecorator();
         new FakeFrameworkMethod();
      }
   }

   private void applyUserSpecifiedStartupFakesIfAny()
   {
      for (String fakeClassName : config.fakeClasses) {
         applyStartupFake(fakeClassName);
      }
   }

   private static void applyStartupFake(@Nonnull String fakeClassName)
   {
      String argument = null;
      int p = fakeClassName.indexOf('=');

      if (p > 0) {
         argument = fakeClassName.substring(p + 1);
         fakeClassName = fakeClassName.substring(0, p);
      }

      try {
         Class<?> fakeClass = ClassLoad.loadClassAtStartup(fakeClassName);

         if (Fake.class.isAssignableFrom(fakeClass)) {
            if (argument == null) {
               ConstructorReflection.newInstanceUsingDefaultConstructor(fakeClass);
            }
            else {
               ConstructorReflection.newInstance(fakeClass, argument);
            }
         }
      }
      catch (UnsupportedOperationException ignored) {}
      catch (Throwable unexpectedFailure) {
         StackTrace.filterStackTrace(unexpectedFailure);
         unexpectedFailure.printStackTrace();
      }
   }
}
