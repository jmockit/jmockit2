package org.jmockit.internal.injection;

import static java.lang.reflect.Modifier.*;
import org.jmockit.*;
import static org.jmockit.internal.reflection.FieldReflection.*;
import org.jmockit.internal.util.*;

import javax.annotation.*;
import java.lang.reflect.*;

final class TestedField extends TestedObject
{
   @Nonnull private final Field testedField;

   TestedField(@Nonnull InjectionState injectionState, @Nonnull Field field, @Nonnull Tested metadata)
   {
      super(injectionState, metadata, field.getName(), field.getGenericType(), field.getType());
      testedField = field;
   }

   @Override
   boolean alreadyInstantiated(@Nonnull Object testClassInstance)
   {
      return isAvailableDuringSetup() && getFieldValue(testedField, testClassInstance) != null;
   }

   @Nullable @Override
   Object getExistingTestedInstanceIfApplicable(@Nonnull Object testClassInstance)
   {
      Object testedObject = null;

      if (!createAutomatically) {
         Class<?> targetClass = testedField.getType();
         testedObject = getFieldValue(testedField, testClassInstance);

         if (testedObject == null || isNonInstantiableType(targetClass, testedObject)) {
            String providedValue = metadata.value();

            if (!providedValue.isEmpty()) {
               testedObject = Utilities.convertFromString(targetClass, providedValue);
            }

            createAutomatically = testedObject == null && !isFinal(testedField.getModifiers());
         }
      }

      return testedObject;
   }

   @Override
   void setInstance(@Nonnull Object testClassInstance, @Nullable Object testedInstance)
   {
      setFieldValue(testedField, testClassInstance, testedInstance);
   }
}
