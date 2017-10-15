package org.jmockit.integration.junit4.internal;

import org.jmockit.*;
import org.jmockit.internal.faking.*;
import org.jmockit.internal.util.*;

import org.junit.runners.model.*;

import javax.annotation.*;
import java.lang.annotation.*;
import java.util.*;

/**
 * Startup fake that modifies the JUnit 4.5+ test runner so that it calls back to JMockit immediately after every test
 * executes.
 * When that happens, JMockit will assert any expectations recorded during the test in {@link Expectations} subclasses.
 * <p/>
 * This class is not supposed to be accessed from user code. JMockit will automatically load it at startup.
 */
public final class FakeFrameworkMethod extends Fake<FrameworkMethod>
{
   public static boolean hasDependenciesInClasspath()
   {
      return ClassLoad.searchTypeInClasspath("org.junit.runners.model.FrameworkMethod", true) != null;
   }

   @Nonnull private final JUnit4TestRunnerDecorator decorator = new JUnit4TestRunnerDecorator();

   @Nullable
   public Object invokeExplosively(@Nonnull Invocation invocation, Object target, Object... params) throws Throwable
   {
      return decorator.invokeExplosively((FakeInvocation) invocation, target, params);
   }

   public static void validatePublicVoidNoArg(@Nonnull Invocation invocation, boolean isStatic, List<Throwable> errors)
   {
      FrameworkMethod it = invocation.getInvokedInstance();
      int previousErrorCount = errors.size();

      if (!isStatic && eachParameterContainsAKnownAnnotation(it.getMethod().getParameterAnnotations())) {
         it.validatePublicVoid(false, errors);
      }
      else {
         ((FakeInvocation) invocation).prepareToProceedFromNonRecursiveMock();
         it.validatePublicVoidNoArg(isStatic, errors);
      }

      int errorCount = errors.size();

      for (int i = previousErrorCount; i < errorCount; i++) {
         Throwable errorAdded = errors.get(i);
         StackTrace.filterStackTrace(errorAdded);
      }
   }

   private static boolean eachParameterContainsAKnownAnnotation(@Nonnull Annotation[][] parametersAndTheirAnnotations)
   {
      if (parametersAndTheirAnnotations.length == 0) {
         return false;
      }

      for (Annotation[] parameterAnnotations : parametersAndTheirAnnotations) {
         if (!containsAKnownAnnotation(parameterAnnotations)) {
            return false;
         }
      }

      return true;
   }

   private static boolean containsAKnownAnnotation(@Nonnull Annotation[] parameterAnnotations)
   {
      if (parameterAnnotations.length == 0) {
         return false;
      }

      for (Annotation parameterAnnotation : parameterAnnotations) {
         String annotationTypeName = parameterAnnotation.annotationType().getName();

         if (!"mockit.Tested mockit.Mocked mockit.Injectable mockit.Capturing".contains(annotationTypeName)) {
            return false;
         }
      }

      return true;
   }
}
