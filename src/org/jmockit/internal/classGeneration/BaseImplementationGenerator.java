package org.jmockit.internal.classGeneration;

import static java.lang.reflect.Modifier.*;
import org.jmockit.external.asm.*;
import static org.jmockit.external.asm.Opcodes.*;
import org.jmockit.internal.*;
import org.jmockit.internal.reflection.*;
import org.jmockit.internal.reflection.GenericTypeReflection.*;

import javax.annotation.*;
import java.util.*;

@SuppressWarnings("AbstractClassExtendsConcreteClass")
public abstract class BaseImplementationGenerator extends BaseClassModifier
{
   private static final int CLASS_ACCESS = ACC_PUBLIC + ACC_FINAL;

   @Nonnull private final List<String> implementedMethods;
   @Nonnull private final String implementationClassDesc;
   @Nullable private String[] initialSuperInterfaces;
   protected String methodOwner;

   protected BaseImplementationGenerator(@Nonnull ClassReader classReader, @Nonnull String implementationClassName)
   {
      super(classReader);
      implementedMethods = new ArrayList<>();
      implementationClassDesc = implementationClassName.replace('.', '/');
   }

   @Override
   public void visit(
      int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName,
      @Nullable String[] interfaces)
   {
      methodOwner = name;
      initialSuperInterfaces = interfaces;

      String[] implementedInterfaces = {name};
      super.visit(version, CLASS_ACCESS, implementationClassDesc, signature, superName, implementedInterfaces);

      generateNoArgsConstructor();
   }

   private void generateNoArgsConstructor()
   {
      mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
      mw.visitVarInsn(ALOAD, 0);
      mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
      generateEmptyImplementation();
   }

   @Override
   public final AnnotationVisitor visitAnnotation(String desc, boolean visible) { return null; }

   @Override
   public final void visitInnerClass(String name, String outerName, String innerName, int access) {}

   @Override
   public final void visitOuterClass(String owner, @Nullable String name, @Nullable String desc) {}

   @Override
   public final void visitAttribute(Attribute attr) {}

   @Override
   public final void visitSource(@Nullable String source, @Nullable String debug) {}

   @Nullable @Override
   public final FieldVisitor visitField(
      int access, String name, String desc, @Nullable String signature, @Nullable Object value)
   { return null; }

   @Nullable @Override
   public final MethodVisitor visitMethod(
      int access, String name, String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      generateMethodImplementation(access, name, desc, signature, exceptions);
      return null;
   }

   @Override
   public final void visitEnd()
   {
      assert initialSuperInterfaces != null;

      for (String superInterface : initialSuperInterfaces) {
         new MethodGeneratorForImplementedSuperInterface(superInterface);
      }
   }

   protected final void generateMethodImplementation(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      if (!isStatic(access)) {
         String methodNameAndDesc = name + desc;

         if (!implementedMethods.contains(methodNameAndDesc)) {
            generateMethodBody(access, name, desc, signature, exceptions);
            implementedMethods.add(methodNameAndDesc);
         }
      }
   }

   protected abstract void generateMethodBody(
      int access, @Nonnull String name, @Nonnull String desc,
      @Nullable String signature, @Nullable String[] exceptions);

   protected final boolean isOverrideOfMethodFromSuperInterface(@Nonnull String name, @Nonnull String desc)
   {
      if (!implementedMethods.isEmpty()) {
         int p = desc.lastIndexOf(')');
         String descNoReturnType = desc.substring(0, p + 1);

         for (String implementedMethod : implementedMethods) {
            if (sameMethodName(implementedMethod, name) && implementedMethod.contains(descNoReturnType)) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean sameMethodName(@Nonnull String implementedMethod, @Nonnull String name)
   {
      return implementedMethod.startsWith(name) && implementedMethod.charAt(name.length()) == '(';
   }

   @Nullable
   protected final String getSubInterfaceOverride(
      @Nonnull GenericTypeReflection genericTypeMap, @Nonnull String name, @Nonnull String genericSignature)
   {
      if (!implementedMethods.isEmpty()) {
         GenericSignature parsedSignature = genericTypeMap.parseSignature(genericSignature);

         for (String implementedMethod : implementedMethods) {
            if (sameMethodName(implementedMethod, name) && parsedSignature.satisfiesSignature(implementedMethod)) {
               return implementedMethod;
            }
         }
      }

      return null;
   }

   private final class MethodGeneratorForImplementedSuperInterface extends ClassVisitor
   {
      private String[] superInterfaces;

      MethodGeneratorForImplementedSuperInterface(@Nonnull String interfaceName)
      {
         ClassFile.visitClass(interfaceName, this);
      }

      @Override
      public void visit(
         int version, int access, String name, @Nullable String signature, @Nullable String superName,
         @Nullable String[] interfaces)
      {
         methodOwner = name;
         superInterfaces = interfaces;
      }

      @Nullable @Override
      public FieldVisitor visitField(
         int access, String name, String desc, @Nullable String signature, @Nullable Object value)
      { return null; }

      @Nullable @Override
      public MethodVisitor visitMethod(
         int access, String name, String desc, @Nullable String signature, @Nullable String[] exceptions)
      {
         generateMethodImplementation(access, name, desc, signature, exceptions);
         return null;
      }

      @Override
      public void visitEnd()
      {
         for (String superInterface : superInterfaces) {
            new MethodGeneratorForImplementedSuperInterface(superInterface);
         }
      }
   }
}
