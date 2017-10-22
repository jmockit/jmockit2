package org.jmockit.internal.expectations.transformation;

import org.jmockit.external.asm.*;
import static org.jmockit.external.asm.ClassReader.*;
import static org.jmockit.internal.util.ClassNaming.*;

import javax.annotation.*;
import java.lang.instrument.*;
import java.security.*;

public final class ExpectationsTransformer implements ClassFileTransformer
{
   @Nullable @Override
   public byte[] transform(
      @Nullable ClassLoader loader, @Nonnull String className, @Nullable Class<?> classBeingRedefined,
      @Nullable ProtectionDomain protectionDomain, @Nonnull byte[] classfileBuffer
   ) {
      if (classBeingRedefined == null && protectionDomain != null && isAnonymousClass(className)) {
         ClassReader cr = new ClassReader(classfileBuffer);
         String superClassName = cr.getSuperName();

         if ("org/jmockit/Expectations".equals(superClassName) || "org/jmockit/Verifications".equals(superClassName)) {
            ClassVisitor modifier = new EndOfBlockModifier(cr);

            try {
               cr.accept(modifier, SKIP_FRAMES);
               return modifier.toByteArray();
            }
            catch (Throwable e) { e.printStackTrace(); }

            return null;
         }
      }

      return null;
   }
}
