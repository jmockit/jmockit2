package org.jmockit.internal.expectations.mocking;

import org.jmockit.external.asm.*;
import static org.jmockit.external.asm.Opcodes.*;
import org.jmockit.internal.classGeneration.*;
import static org.jmockit.internal.expectations.mocking.MockedTypeModifier.*;
import org.jmockit.internal.util.*;

import javax.annotation.*;
import java.lang.reflect.Type;

public final class SubclassGenerationModifier extends BaseSubclassGenerator
{
   public SubclassGenerationModifier(
      @Nonnull Class<?> baseClass, @Nonnull Type mockedType,
      @Nonnull ClassReader classReader, @Nonnull String subclassName, boolean copyConstructors)
   {
      super(baseClass, classReader, mockedType, subclassName, copyConstructors);
   }

   @Override
   @SuppressWarnings("AssignmentToMethodParameter")
   protected void generateMethodImplementation(
      @Nonnull String className, int access, @Nonnull String name, @Nonnull String desc,
      @Nullable String signature, @Nullable String[] exceptions)
   {
      if (signature != null && mockedTypeInfo != null) {
         signature = mockedTypeInfo.genericTypeMap.resolveSignature(className, signature);
      }

      mw = cw.visitMethod(ACC_PUBLIC, name, desc, signature, exceptions);

      if (ObjectMethods.isMethodFromObject(name, desc)) {
         generateEmptyImplementation(desc);
      }
      else {
         generateDirectCallToHandler(mw, className, access, name, desc, signature);
         generateReturnWithObjectAtTopOfTheStack(desc);
         mw.visitMaxs(1, 0);
      }
   }
}
