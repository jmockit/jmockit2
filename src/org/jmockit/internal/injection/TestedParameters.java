package org.jmockit.internal.injection;

import org.jmockit.*;
import org.jmockit.internal.expectations.mocking.*;
import org.jmockit.internal.state.*;
import org.jmockit.internal.util.*;

import javax.annotation.*;
import java.lang.annotation.*;

public final class TestedParameters
{
   @Nonnull private final TestMethod testMethod;
   @Nonnull private final InjectionState injectionState;

   public TestedParameters(@Nonnull TestMethod testMethod)
   {
      this.testMethod = testMethod;

      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();
      injectionState = testedClasses == null ? new InjectionState() : testedClasses.injectionState;
   }

   public void createTestedParameters(
      @Nonnull Object testClassInstance, @Nonnull ParameterTypeRedefinitions paramTypeRedefs)
   {
      injectionState.buildListsOfInjectables(testClassInstance, paramTypeRedefs);

      for (int n = testMethod.getParameterCount(), i = 0; i < n; i++) {
         TestedParameter testedParameter = createTestedParameterIfApplicable(i);

         if (testedParameter != null) {
            instantiateTestedObject(testClassInstance, testedParameter);
         }
      }
   }

   @Nullable
   private TestedParameter createTestedParameterIfApplicable(@Nonnegative int parameterIndex)
   {
      Annotation[] parameterAnnotations = testMethod.getParameterAnnotations(parameterIndex);

      for (Annotation parameterAnnotation : parameterAnnotations) {
         Tested testedMetadata = TestedObject.getTestedAnnotationIfPresent(parameterAnnotation);

         if (testedMetadata != null) {
            return new TestedParameter(injectionState, testMethod, parameterIndex, testedMetadata);
         }
      }

      return null;
   }

   private void instantiateTestedObject(@Nonnull Object testClassInstance, @Nonnull TestedParameter testedObject)
   {
      try {
         testedObject.instantiateWithInjectableValues(testClassInstance);
      }
      finally {
         injectionState.resetConsumedInjectionProviders();
      }
   }
}
