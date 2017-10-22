package org.jmockit.internal.expectations.mocking;

import org.jmockit.internal.util.*;

import javax.annotation.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

public final class ParameterTypeRedefinitions extends TypeRedefinitions
{
   @Nonnull private final TestMethod testMethod;
   @Nonnull private final MockedType[] mockParameters;
   @Nonnull private final List<MockedType> injectableParameters;

   public ParameterTypeRedefinitions(@Nonnull TestMethod testMethod, @Nonnull Object[] parameterValues) {
      this.testMethod = testMethod;
      int n = testMethod.getParameterCount();
      mockParameters = new MockedType[n];
      injectableParameters = new ArrayList<>(n);

      for (int i = 0; i < n; i++) {
         Object mock = parameterValues[i];
         createMockedTypeFromMockParameterDeclaration(i, mock);
      }

      InstanceFactory[] instanceFactories = redefineMockedTypes();
      instantiateMockedTypes(instanceFactories);
   }

   private void createMockedTypeFromMockParameterDeclaration(@Nonnegative int parameterIndex, @Nullable Object mock) {
      Type parameterType = testMethod.getParameterType(parameterIndex);
      Annotation[] annotationsOnParameter = testMethod.getParameterAnnotations(parameterIndex);
      Class<?> parameterImplementationClass = mock == null ? null : mock.getClass();
      MockedType mockedType = new MockedType(
         testMethod, parameterIndex, parameterType, annotationsOnParameter, parameterImplementationClass);

      if (mockedType.isMockableType()) {
         mockParameters[parameterIndex] = mockedType;
      }

      if (mockedType.isInjectable()) {
         injectableParameters.add(mockedType);
         testMethod.setParameterValue(parameterIndex, mockedType.providedValue);
      }
   }

   @Nonnull
   private InstanceFactory[] redefineMockedTypes() {
      int n = mockParameters.length;
      InstanceFactory[] instanceFactories = new InstanceFactory[n];

      for (int i = 0; i < n; i++) {
         MockedType mockedType = mockParameters[i];

         if (mockedType != null) {
            instanceFactories[i] = new InstanceFactory.ClassInstanceFactory(mockedType.getClassType());
         }
      }

      return instanceFactories;
   }

   private void instantiateMockedTypes(@Nonnull InstanceFactory[] instanceFactories) {
      for (int paramIndex = 0; paramIndex < instanceFactories.length; paramIndex++) {
         InstanceFactory instanceFactory = instanceFactories[paramIndex];

         if (instanceFactory != null) {
            MockedType mockedType = mockParameters[paramIndex];
            @Nonnull Object mockedInstance = instantiateMockedType(mockedType, instanceFactory, paramIndex);
            testMethod.setParameterValue(paramIndex, mockedInstance);
            mockedType.providedValue = mockedInstance;
         }
      }
   }

   @Nonnull
   private Object instantiateMockedType(
      @Nonnull MockedType mockedType, @Nonnull InstanceFactory instanceFactory, @Nonnegative int paramIndex
   ) {
      Object mock = testMethod.getParameterValue(paramIndex);

      if (mock == null) {
         mock = instanceFactory.create();
      }

      registerMock(mockedType, mock);
      return mock;
   }

   @Nonnull public List<MockedType> getInjectableParameters() { return injectableParameters; }
}
