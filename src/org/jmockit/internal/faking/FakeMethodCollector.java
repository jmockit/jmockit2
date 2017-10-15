package org.jmockit.internal.faking;

import org.jmockit.*;
import org.jmockit.external.asm.*;
import static org.jmockit.external.asm.ClassReader.*;
import static org.jmockit.external.asm.Opcodes.*;
import org.jmockit.internal.*;
import org.jmockit.internal.faking.FakeMethods.*;
import org.jmockit.internal.util.*;

import javax.annotation.*;

/**
 * Responsible for collecting the signatures of all methods defined in a given fake class which are public.
 */
final class FakeMethodCollector extends ClassVisitor
{
   private static final int INVALID_METHOD_ACCESSES =
       ACC_PRIVATE + ACC_PROTECTED + ACC_BRIDGE + ACC_SYNTHETIC + ACC_ABSTRACT + ACC_NATIVE;

   @Nonnull private final FakeMethods fakeMethods;

   private boolean collectingFromSuperClass;
   @Nullable private String enclosingClassDescriptor;

   FakeMethodCollector(@Nonnull FakeMethods fakeMethods) { this.fakeMethods = fakeMethods; }

   void collectFakeMethods(@Nonnull Class<?> fakeClass) {
      ClassLoad.registerLoadedClass(fakeClass);

      Class<?> classToCollectFakesFrom = fakeClass;

      do {
         ClassReader mcReader = ClassFile.readFromFile(classToCollectFakesFrom);
         mcReader.accept(this, SKIP_CODE + SKIP_FRAMES);
         classToCollectFakesFrom = classToCollectFakesFrom.getSuperclass();
         collectingFromSuperClass = true;
      }
      while (classToCollectFakesFrom != Object.class && classToCollectFakesFrom != Fake.class);
   }

   @Override
   public void visit(
      int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName,
      @Nullable String[] interfaces
   ) {
      if (!collectingFromSuperClass) {
         fakeMethods.setFakeClassInternalName(name);

         int p = name.lastIndexOf('$');

         if (p > 0) {
            enclosingClassDescriptor = "(L" + name.substring(0, p) + ";)V";
         }
      }
   }

   /**
    * Adds the method specified to the set of fake methods, as long as it's annotated with {@code @Mock}.
    *
    * @param methodSignature generic signature for a Java 5 generic method, ignored since redefinition only needs to
    *                        consider the "erased" signature
    * @param exceptions zero or more thrown exceptions in the method "throws" clause, also ignored
    */
   @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
   @Nullable @Override
   public MethodVisitor visitMethod(
      int access, @Nonnull String methodName, @Nonnull String methodDesc, String methodSignature, String[] exceptions
   ) {
      if ((access & INVALID_METHOD_ACCESSES) != 0 || (access & ACC_PUBLIC) == 0) {
         return null;
      }

      if ("<init>".equals(methodName)) {
         if (!collectingFromSuperClass && methodDesc.equals(enclosingClassDescriptor)) {
            enclosingClassDescriptor = null;
         }

         return null;
      }

      FakeMethod fakeMethod = fakeMethods.addMethod(collectingFromSuperClass, access, methodName, methodDesc);

      if (fakeMethod != null && fakeMethod.requiresFakeState()) {
         FakeState fakeState = new FakeState(fakeMethod);
         fakeMethods.addFakeState(fakeState);
      }

      return null;
   }
}
