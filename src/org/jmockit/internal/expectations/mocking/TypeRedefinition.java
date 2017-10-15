package org.jmockit.internal.expectations.mocking;

import org.jmockit.internal.*;

import javax.annotation.*;
import java.lang.reflect.*;

class TypeRedefinition extends BaseTypeRedefinition
{
   TypeRedefinition(@Nonnull MockedType typeMetadata) { super(typeMetadata); }

   @Nullable
   final InstanceFactory redefineType()
   {
      //noinspection ConstantConditions
      Class<?> classToMock = typeMetadata.getClassType();

      if (MockingFilters.isSubclassOfUnmockable(classToMock)) {
         String mockSource = typeMetadata.field == null ? "mock parameter" : "mock field";
         throw new IllegalArgumentException(
            classToMock + " is not mockable (" + mockSource + " \"" + typeMetadata.getName() + "\")");
      }

      Type declaredType = typeMetadata.getDeclaredType();
      return redefineType(declaredType);
   }
}
