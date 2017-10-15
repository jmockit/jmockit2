package org.jmockit.internal.expectations.mocking;

import static java.lang.reflect.Modifier.*;
import org.jmockit.external.asm.*;
import static org.jmockit.external.asm.Opcodes.*;
import org.jmockit.internal.*;
import static org.jmockit.internal.MockingFilters.*;
import org.jmockit.internal.expectations.*;
import static org.jmockit.internal.expectations.mocking.MockedTypeModifier.*;
import org.jmockit.internal.util.*;
import static org.jmockit.internal.util.ObjectMethods.*;
import static org.jmockit.internal.util.Utilities.*;

import javax.annotation.*;
import java.util.*;

final class MockedClassModifier extends BaseClassModifier
{
   private static final boolean NATIVE_UNSUPPORTED = !HOTSPOT_VM;
   private static final int METHOD_ACCESS_MASK = ACC_PRIVATE + ACC_SYNTHETIC + ACC_ABSTRACT;
   private static final int PUBLIC_OR_PROTECTED = ACC_PUBLIC + ACC_PROTECTED;

   @Nullable private final MockedType mockedType;
   private final boolean classFromNonBootstrapClassLoader;
   private String className;
   private boolean ignoreConstructors;
   private ExecutionMode executionMode;
   private boolean isProxy;
   @Nullable private String defaultFilters;
   @Nullable List<String> enumSubclasses;

   MockedClassModifier(
      @Nullable ClassLoader classLoader, @Nonnull ClassReader classReader, @Nullable MockedType typeMetadata)
   {
      super(classReader);
      mockedType = typeMetadata;
      classFromNonBootstrapClassLoader = classLoader != null;
      setUseClassLoadingBridge(classLoader);
      executionMode = ExecutionMode.Regular;
      useInstanceBasedMockingIfApplicable();
   }

   private void useInstanceBasedMockingIfApplicable()
   {
      if (mockedType != null && mockedType.injectable) {
         ignoreConstructors = true;
         executionMode = ExecutionMode.PerInstance;
      }
   }

   public void useDynamicMocking(boolean methodsOnly)
   {
      ignoreConstructors = methodsOnly;
      executionMode = ExecutionMode.Partial;
   }

   @Override
   public void visit(
      int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName,
      @Nullable String[] interfaces)
   {
      validateMockingOfJREClass(name);

      super.visit(version, access, name, signature, superName, interfaces);
      isProxy = "java/lang/reflect/Proxy".equals(superName);

      if (isProxy) {
         assert interfaces != null;
         className = interfaces[0];
         defaultFilters = null;
      }
      else {
         className = name;
         defaultFilters = filtersForClass(name);

         if (defaultFilters != null && defaultFilters.isEmpty()) {
            throw VisitInterruptedException.INSTANCE;
         }
      }
   }

   private void validateMockingOfJREClass(@Nonnull String internalName)
   {
      if (internalName.startsWith("java/")) {
         if (isUnmockable(internalName)) {
            throw new IllegalArgumentException("Class " + internalName.replace('/', '.') + " is not mockable");
         }

         if (executionMode == ExecutionMode.Regular && mockedType != null && isFullMockingDisallowed(internalName)) {
            String modifyingClassName = internalName.replace('/', '.');

            if (modifyingClassName.equals(mockedType.getClassType().getName())) {
               throw new IllegalArgumentException(
                  "Class " + internalName.replace('/', '.') + " cannot be @Mocked fully; " +
                  "instead, use @Injectable or partial mocking");
            }
         }
      }
   }

   @Override
   public void visitInnerClass(@Nonnull String name, @Nullable String outerName, @Nullable String innerName, int access)
   {
      cw.visitInnerClass(name, outerName, innerName, access);

      if (access == ACC_ENUM + ACC_STATIC) {
         if (enumSubclasses == null) {
            enumSubclasses = new ArrayList<>();
         }

         enumSubclasses.add(name);
      }
   }

   @Nullable @Override
   public MethodVisitor visitMethod(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      if ((access & METHOD_ACCESS_MASK) != 0) {
         return unmodifiedBytecode(access, name, desc, signature, exceptions);
      }

      boolean visitingConstructor = "<init>".equals(name);
      String internalClassName = className;

      if (visitingConstructor) {
         if (isConstructorNotAllowedByMockingFilters(name)) {
            return unmodifiedBytecode(access, name, desc, signature, exceptions);
         }

         startModifiedMethodVersion(access, name, desc, signature, exceptions);
         generateCallToSuperConstructor();
      }
      else {
         if (isMethodNotToBeMocked(access, name, desc)) {
            return unmodifiedBytecode(access, name, desc, signature, exceptions);
         }

         if ("<clinit>".equals(name)) {
            return stubOutClassInitializationIfApplicable(access);
         }

         if (stubOutFinalizeMethod(access, name, desc)) {
            return null;
         }

         startModifiedMethodVersion(access, name, desc, signature, exceptions);
      }

      ExecutionMode actualExecutionMode = determineAppropriateExecutionMode(visitingConstructor);

      if (useClassLoadingBridge) {
         return generateCallToHandlerThroughMockingBridge(visitingConstructor);
      }

      generateDirectCallToHandler(mw, internalClassName, access, name, desc, signature, actualExecutionMode);
      generateDecisionBetweenReturningOrContinuingToRealImplementation();

      // Constructors of non-JRE classes can't be modified (unless running with "-noverify") in a way that
      // "super(...)/this(...)" get called twice, so we disregard such calls when copying the original bytecode.
      return copyOriginalImplementationCode(visitingConstructor);
   }

   private boolean isConstructorNotAllowedByMockingFilters(@Nonnull String name)
   {
      return isProxy || ignoreConstructors || isUnmockableInvocation(defaultFilters, name);
   }

   private boolean isMethodNotToBeMocked(int access, @Nonnull String name, @Nonnull String desc)
   {
      return
         isNative(access) && (NATIVE_UNSUPPORTED || (access & PUBLIC_OR_PROTECTED) == 0) ||
         (isProxy || executionMode == ExecutionMode.Partial) && (
            isMethodFromObject(name, desc) || "annotationType".equals(name) && "()Ljava/lang/Class;".equals(desc)
         );
   }

   @Nonnull
   private MethodVisitor unmodifiedBytecode(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      return cw.visitMethod(access, name, desc, signature, exceptions);
   }

   @Nullable
   private MethodVisitor stubOutClassInitializationIfApplicable(int access)
   {
      startModifiedMethodVersion(access, "<clinit>", "()V", null, null);
      return mw;
   }

   private boolean stubOutFinalizeMethod(int access, @Nonnull String name, @Nonnull String desc)
   {
      if ("finalize".equals(name) && "()V".equals(desc)) {
         startModifiedMethodVersion(access, name, desc, null, null);
         generateEmptyImplementation();
         return true;
      }

      return false;
   }

   @Nonnull
   private ExecutionMode determineAppropriateExecutionMode(boolean visitingConstructor)
   {
      if (executionMode == ExecutionMode.PerInstance) {
         if (visitingConstructor) {
            return ignoreConstructors ? ExecutionMode.Regular : ExecutionMode.Partial;
         }

         if (isStatic(methodAccess)) {
            return ExecutionMode.Partial;
         }
      }

      return executionMode;
   }

   @Nonnull
   private MethodVisitor generateCallToHandlerThroughMockingBridge(boolean visitingConstructor)
   {
      // Copies the entire original implementation even for a constructor, in which case the complete bytecode inside
      // the constructor fails the strict verification activated by "-Xfuture". However, this is necessary to allow the
      // full execution of a bootstrap class constructor when the call was not meant to be mocked.
      return copyOriginalImplementationCode(visitingConstructor && classFromNonBootstrapClassLoader);
   }

   private void generateDecisionBetweenReturningOrContinuingToRealImplementation()
   {
      Label startOfRealImplementation = new Label();
      mw.visitInsn(DUP);
      mw.visitLdcInsn(VOID_TYPE);
      mw.visitJumpInsn(IF_ACMPEQ, startOfRealImplementation);
      generateReturnWithObjectAtTopOfTheStack(methodDesc);
      mw.visitLabel(startOfRealImplementation);
      mw.visitInsn(POP);
   }
}
