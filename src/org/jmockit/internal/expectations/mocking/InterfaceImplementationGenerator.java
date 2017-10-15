package org.jmockit.internal.expectations.mocking;

import org.jmockit.external.asm.*;
import static org.jmockit.external.asm.Opcodes.*;
import org.jmockit.internal.classGeneration.*;
import static org.jmockit.internal.expectations.mocking.MockedTypeModifier.*;

import javax.annotation.*;
import java.lang.reflect.Type;

final class InterfaceImplementationGenerator extends BaseImplementationGenerator
{
   @Nonnull private final MockedTypeInfo mockedTypeInfo;
   private String interfaceName;

   InterfaceImplementationGenerator(
       @Nonnull ClassReader classReader, @Nonnull Type mockedType, @Nonnull String implementationClassName)
   {
      super(classReader, implementationClassName);
      mockedTypeInfo = new MockedTypeInfo(mockedType);
   }

   @Override
   public void visit(
      int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName,
      @Nullable String[] interfaces)
   {
      interfaceName = name;
      String classSignature = signature == null ? null : signature + mockedTypeInfo.implementationSignature;
      super.visit(version, access, name, classSignature, superName, interfaces);
   }

   @Override
   protected void generateMethodBody(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      mw = cw.visitMethod(ACC_PUBLIC, name, desc, signature, exceptions);

      String className = null;

      if (signature != null) {
         String subInterfaceOverride = getSubInterfaceOverride(mockedTypeInfo.genericTypeMap, name, signature);

         if (subInterfaceOverride != null) {
            className = interfaceName;
            desc = subInterfaceOverride.substring(name.length());
            signature = null;
         }
      }

      if (className == null) {
         className = isOverrideOfMethodFromSuperInterface(name, desc) ? interfaceName : methodOwner;
      }

      generateDirectCallToHandler(mw, className, access, name, desc, signature);
      generateReturnWithObjectAtTopOfTheStack(desc);
      mw.visitMaxs(1, 0);
   }
}
