package org.jmockit.internal.injection;

import org.jmockit.*;
import org.jmockit.internal.state.*;
import org.jmockit.internal.util.*;

import javax.annotation.*;

final class TestedParameter extends TestedObject
{
   @Nonnull private final TestMethod testMethod;
   @Nonnegative private final int parameterIndex;

   TestedParameter(
      @Nonnull InjectionState injectionState, @Nonnull TestMethod testMethod, @Nonnegative int parameterIndex,
      @Nonnull Tested metadata)
   {
      super(
         injectionState, metadata, ParameterNames.getName(testMethod, parameterIndex),
         testMethod.getParameterType(parameterIndex), testMethod.getParameterClass(parameterIndex));
      this.testMethod = testMethod;
      this.parameterIndex = parameterIndex;
   }

   @Nullable @Override
   Object getExistingTestedInstanceIfApplicable(@Nonnull Object testClassInstance)
   {
      Object testedObject = null;

      if (!createAutomatically) {
         String providedValue = metadata.value();

         if (!providedValue.isEmpty()) {
            Class<?> parameterClass = testMethod.getParameterClass(parameterIndex);
            testedObject = Utilities.convertFromString(parameterClass, providedValue);

            if (testedObject != null) {
               testMethod.setParameterValue(parameterIndex, testedObject);
            }
         }

         createAutomatically = testedObject == null;
      }

      return testedObject;
   }

   @Override
   void setInstance(@Nonnull Object testClassInstance, @Nullable Object testedInstance)
   {
      testMethod.setParameterValue(parameterIndex, testedInstance);
   }
}
