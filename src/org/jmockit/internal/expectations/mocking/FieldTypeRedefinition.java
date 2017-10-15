package org.jmockit.internal.expectations.mocking;

import org.jmockit.internal.state.*;

import javax.annotation.*;
import java.lang.reflect.*;

final class FieldTypeRedefinition extends TypeRedefinition
{
   private boolean usePartialMocking;

   FieldTypeRedefinition(@Nonnull MockedType typeMetadata) { super(typeMetadata); }

   boolean redefineTypeForTestedField()
   {
      usePartialMocking = true;
      return redefineTypeForFieldNotSet();
   }

   @Override
   void configureClassModifier(@Nonnull MockedClassModifier modifier)
   {
      if (usePartialMocking) {
         modifier.useDynamicMocking(true);
      }
   }

   @SuppressWarnings("ConstantConditions")
   boolean redefineTypeForFinalField()
   {
      if (targetClass == TypeVariable.class || !typeMetadata.injectable && targetClass.isInterface()) {
         String mockFieldName = typeMetadata.getName();
         throw new IllegalArgumentException("Final mock field \"" + mockFieldName + "\" must be of a class type");
      }

      return redefineTypeForFieldNotSet();
   }

   private boolean redefineTypeForFieldNotSet()
   {
      boolean redefined = redefineMethodsAndConstructorsInTargetType();

      if (redefined) {
         TestRun.mockFixture().registerMockedClass(targetClass);
      }

      return redefined;
   }
}
