package org.jmockit.internal.expectations.mocking;

import org.jmockit.internal.state.*;

import javax.annotation.*;
import java.lang.reflect.*;

final class FieldTypeRedefinition extends TypeRedefinition
{
   FieldTypeRedefinition(@Nonnull MockedType typeMetadata) { super(typeMetadata); }

   @SuppressWarnings("ConstantConditions")
   boolean redefineTypeForFinalField() {
      if (targetClass == TypeVariable.class || !typeMetadata.isInjectable() && targetClass.isInterface()) {
         String mockFieldName = typeMetadata.getName();
         throw new IllegalArgumentException("Final mock field \"" + mockFieldName + "\" must be of a class type");
      }

      return redefineTypeForFieldNotSet();
   }

   private boolean redefineTypeForFieldNotSet() {
      boolean redefined = redefineMethodsAndConstructorsInTargetType();

      if (redefined) {
         TestRun.mockFixture().registerMockedClass(targetClass);
      }

      return redefined;
   }
}
