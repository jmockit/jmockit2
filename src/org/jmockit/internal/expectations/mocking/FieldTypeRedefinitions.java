package org.jmockit.internal.expectations.mocking;

import static java.lang.reflect.Modifier.*;
import static org.jmockit.external.asm.Opcodes.*;
import org.jmockit.internal.reflection.*;
import org.jmockit.internal.state.*;
import org.jmockit.internal.util.*;

import javax.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;

@SuppressWarnings("UnnecessaryFullyQualifiedName")
public final class FieldTypeRedefinitions extends TypeRedefinitions
{
   private static final int FIELD_ACCESS_MASK = ACC_SYNTHETIC + ACC_STATIC;

   @Nonnull private final Map<MockedType, InstanceFactory> mockInstanceFactories;
   @Nonnull private final List<MockedType> mockFieldsNotSet;

   public FieldTypeRedefinitions(@Nonnull Class<?> testClass) {
      mockInstanceFactories = new HashMap<>();
      mockFieldsNotSet = new ArrayList<>();

      TestRun.enterNoMockingZone();

      try {
         redefineFieldTypes(testClass);
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }

   private void redefineFieldTypes(@Nonnull Class<?> classWithMockFields) {
      Class<?> superClass = classWithMockFields.getSuperclass();

      if (superClass != null && superClass != Object.class && superClass != org.jmockit.Expectations.class) {
         redefineFieldTypes(superClass);
      }

      Field[] fields = classWithMockFields.getDeclaredFields();

      for (Field candidateField : fields) {
         int fieldModifiers = candidateField.getModifiers();

         if ((fieldModifiers & FIELD_ACCESS_MASK) == 0) {
            redefineFieldType(candidateField, fieldModifiers);
         }
      }
   }

   private void redefineFieldType(@Nonnull Field field, int modifiers) {
      MockedType mockedType = new MockedType(field);

      if (mockedType.isMockableType()) {
         boolean needsValueToSet = !isFinal(modifiers);
         redefineFieldType(mockedType, needsValueToSet);
      }
   }

   private void redefineFieldType(@Nonnull MockedType mockedType, boolean needsValueToSet) {
      FieldTypeRedefinition typeRedefinition = new FieldTypeRedefinition(mockedType);
      boolean redefined;

      if (needsValueToSet) {
         InstanceFactory factory = typeRedefinition.redefineType();
         redefined = factory != null;

         if (redefined) {
            mockInstanceFactories.put(mockedType, factory);
         }
      }
      else {
         redefined = typeRedefinition.redefineTypeForFinalField();

         if (redefined) {
            mockFieldsNotSet.add(mockedType);
         }
      }

      if (redefined) {
         addTargetClass(mockedType);
      }
   }

   public void assignNewInstancesToMockFields(@Nonnull Object target) {
      TestRun.getExecutingTest().clearInjectableAndNonStrictMocks();
      createAndAssignNewInstances(target);
      obtainAndRegisterInstancesOfFieldsNotSet(target);
   }

   private void createAndAssignNewInstances(@Nonnull Object target) {
      for (Entry<MockedType, InstanceFactory> metadataAndFactory : mockInstanceFactories.entrySet()) {
         MockedType mockedType = metadataAndFactory.getKey();
         InstanceFactory instanceFactory = metadataAndFactory.getValue();

         Object mock = assignNewInstanceToMockField(target, mockedType, instanceFactory);
         registerMock(mockedType, mock);
      }
   }

   @Nonnull
   private Object assignNewInstanceToMockField(
      @Nonnull Object target, @Nonnull MockedType mockedType, @Nonnull InstanceFactory instanceFactory
   ) {
      Field mockField = mockedType.field;
      assert mockField != null;
      Object mock = FieldReflection.getFieldValue(mockField, target);

      if (mock == null) {
         try {
            mock = instanceFactory.create();
         }
         catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            StackTrace.filterStackTrace(e);
            e.printStackTrace();
            throw e;
         }

         FieldReflection.setFieldValue(mockField, target, mock);
      }

      return mock;
   }

   private void obtainAndRegisterInstancesOfFieldsNotSet(@Nonnull Object target) {
      for (MockedType metadata : mockFieldsNotSet) {
         assert metadata.field != null;
         Object mock = FieldReflection.getFieldValue(metadata.field, target);

         if (mock != null) {
            registerMock(metadata, mock);
         }
      }
   }

   /**
    * Returns true iff the mock instance concrete class is not mocked in some test, ie it's a class
    * which only appears in the code under test.
    */
   public boolean captureNewInstanceForApplicableMockField(@Nonnull Object mock) { return false; }

   @Override
   public void cleanUp() {
      TestRun.getExecutingTest().getCascadingTypes().clear();
      super.cleanUp();
   }
}
