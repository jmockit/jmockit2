package org.jmockit.internal;

import org.jmockit.external.asm.*;
import static org.jmockit.external.asm.ClassReader.*;
import org.jmockit.internal.util.*;

import javax.annotation.*;
import java.lang.reflect.*;
import java.util.*;

@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
final class SuperConstructorCollector extends ClassVisitor
{
   @Nonnull static final SuperConstructorCollector INSTANCE = new SuperConstructorCollector();

   @Nonnull private final Map<String, String> cache = new HashMap<>();
   @Nullable private String constructorDesc;
   private boolean samePackage;

   private SuperConstructorCollector() {}

   @Nonnull
   synchronized String findConstructor(@Nonnull String classDesc, @Nonnull String superClassDesc)
   {
      constructorDesc = cache.get(superClassDesc);

      if (constructorDesc != null) {
         return constructorDesc;
      }

      findIfBothClassesAreInSamePackage(classDesc, superClassDesc);

      ClassReader cr = ClassFile.readFromFile(superClassDesc);
      try { cr.accept(this, SKIP_DEBUG + SKIP_FRAMES); } catch (VisitInterruptedException ignore) {}

      cache.put(superClassDesc, constructorDesc);
      
      return constructorDesc;
   }

   private void findIfBothClassesAreInSamePackage(@Nonnull String classDesc, @Nonnull String superClassDesc)
   {
      int p1 = classDesc.lastIndexOf('/');
      int p2 = superClassDesc.lastIndexOf('/');
      samePackage = p1 == p2 && (p1 < 0 || classDesc.substring(0, p1).equals(superClassDesc.substring(0, p2)));
   }

   @Override @Nullable
   public MethodVisitor visitMethod(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      if (isAccessible(access) && "<init>".equals(name)) {
         constructorDesc = desc;
         throw VisitInterruptedException.INSTANCE;
      }

      return null;
   }

   private boolean isAccessible(int access) { return access != Modifier.PRIVATE && (access != 0 || samePackage); }
}
