package org.jmockit.internal.expectations.transformation;

import org.jmockit.external.asm.*;

import javax.annotation.*;

final class EndOfBlockModifier extends ClassVisitor
{
   @Nonnull private final ClassWriter cw;
   @Nonnull private String classDesc;

   EndOfBlockModifier(@Nonnull ClassReader cr) {
      super(new ClassWriter(cr));
      assert cv != null;
      cw = (ClassWriter) cv;
      classDesc = "";
   }

   @Override
   public void visit(
      int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName,
      @Nullable String[] interfaces
   ) {
      cw.visit(version, access, name, signature, superName, interfaces);
      classDesc = name;
   }

   @Override
   public MethodVisitor visitMethod(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions
   ) {
      MethodWriter mw = cw.visitMethod(access, name, desc, signature, exceptions);
      boolean callEndInvocations = "<init>".equals(name);
      return new InvocationBlockModifier(mw, classDesc, callEndInvocations);
   }
}
