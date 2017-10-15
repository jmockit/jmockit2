package org.jmockit.internal.expectations.transformation;

import org.jmockit.external.asm.*;
import static org.jmockit.external.asm.Opcodes.*;
import static org.jmockit.internal.util.TypeConversion.*;

import javax.annotation.*;

public final class InvocationBlockModifier extends MethodVisitor
{
   private static final String CLASS_DESC = "org/jmockit/internal/expectations/ActiveInvocations";

   @Nonnull private final MethodWriter mw;

   // Input data:
   @Nonnull private final String blockOwner;
   private final boolean callEndInvocations;

   // Keeps track of the current stack size (after each bytecode instruction) within the invocation block:
   @Nonnegative private int stackSize;

   // Handle withCapture()/anyXyz/withXyz matchers, if any:
   @Nonnull final ArgumentMatching argumentMatching;
   @Nonnull final ArgumentCapturing argumentCapturing;
   private boolean justAfterWithCaptureInvocation;

   // Stores the index of the local variable holding a list passed in a withCapture(List) call, if any:
   @Nonnegative private int lastLoadedVarIndex;

   InvocationBlockModifier(@Nonnull MethodWriter mw, @Nonnull String blockOwner, boolean callEndInvocations) {
      super(mw);
      this.mw = mw;
      this.blockOwner = blockOwner;
      this.callEndInvocations = callEndInvocations;
      argumentMatching = new ArgumentMatching(this);
      argumentCapturing = new ArgumentCapturing(this);
   }

   void generateCallToActiveInvocationsMethod(@Nonnull String name) {
      mw.visitMethodInsn(INVOKESTATIC, CLASS_DESC, name, "()V", false);
   }

   void generateCallToActiveInvocationsMethod(@Nonnull String name, @Nonnull String desc) {
      visitMethodInstruction(INVOKESTATIC, CLASS_DESC, name, desc, false);
   }

   @Override
   public void visitFieldInsn(
      @Nonnegative int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc
   ) {
      boolean getField = opcode == GETFIELD;

      if ((getField || opcode == PUTFIELD) && blockOwner.equals(owner)) {
         if (name.indexOf('$') > 0) {
            // Nothing to do.
         }
         else if (getField && ArgumentMatching.isAnyField(name)) {
            argumentMatching.generateCodeToAddArgumentMatcherForAnyField(owner, name, desc);
            argumentMatching.addMatcher(stackSize);
            return;
         }
         else if (!getField && generateCodeThatReplacesAssignmentToSpecialField(name)) {
            visitInsn(POP);
            return;
         }
      }

      stackSize += stackSizeVariationForFieldAccess(opcode, desc);
      mw.visitFieldInsn(opcode, owner, name, desc);
   }

   private boolean generateCodeThatReplacesAssignmentToSpecialField(@Nonnull String fieldName) {
      if ("result".equals(fieldName)) {
         generateCallToActiveInvocationsMethod("addResult", "(Ljava/lang/Object;)V");
         return true;
      }

      if ("times".equals(fieldName) || "minTimes".equals(fieldName) || "maxTimes".equals(fieldName)) {
         generateCallToActiveInvocationsMethod(fieldName, "(I)V");
         return true;
      }

      return false;
   }

   private static int stackSizeVariationForFieldAccess(@Nonnegative int opcode, @Nonnull String fieldType) {
      char c = fieldType.charAt(0);
      boolean twoByteType = c == 'D' || c == 'J';

      switch (opcode) {
         case GETSTATIC: return twoByteType ? 2 : 1;
         case PUTSTATIC: return twoByteType ? -2 : -1;
         case GETFIELD: return twoByteType ? 1 : 0;
         default: return twoByteType ? -3 : -2;
      }
   }

   @Override
   public void visitMethodInsn(
      @Nonnegative int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf
   ) {
      if (opcode == INVOKESTATIC && (isBoxing(owner, name, desc) || isAccessMethod(owner, name))) {
         // It's an invocation to a primitive boxing method or to a synthetic method for private access, just ignore it.
         visitMethodInstruction(INVOKESTATIC, owner, name, desc, itf);
      }
      else if (isCallToArgumentMatcher(opcode, owner, name, desc)) {
         visitMethodInstruction(INVOKEVIRTUAL, owner, name, desc, itf);

         boolean withCaptureMethod = "withCapture".equals(name);

         if (argumentCapturing.registerMatcher(withCaptureMethod, desc, lastLoadedVarIndex)) {
            justAfterWithCaptureInvocation = withCaptureMethod;
            argumentMatching.addMatcher(stackSize);
         }
      }
      else if (isUnboxing(opcode, owner, desc)) {
         if (justAfterWithCaptureInvocation) {
            generateCodeToReplaceNullWithZeroOnTopOfStack(desc);
            justAfterWithCaptureInvocation = false;
         }
         else {
            visitMethodInstruction(opcode, owner, name, desc, itf);
         }
      }
      else {
         handleMockedOrNonMockedInvocation(opcode, owner, name, desc, itf);
      }
   }

   private boolean isAccessMethod(@Nonnull String methodOwner, @Nonnull String name) {
      return !methodOwner.equals(blockOwner) && name.startsWith("access$");
   }

   private void visitMethodInstruction(
      @Nonnegative int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf
   ) {
      if (!"()V".equals(desc)) {
         int argAndRetSize = Type.getArgumentsAndReturnSizes(desc);
         int argSize = argAndRetSize >> 2;

         if (opcode == INVOKESTATIC) {
            argSize--;
         }

         stackSize -= argSize;

         int retSize = argAndRetSize & 0x03;
         stackSize += retSize;
      }
      else if (opcode != INVOKESTATIC) {
         stackSize--;
      }

      mw.visitMethodInsn(opcode, owner, name, desc, itf);
   }

   private boolean isCallToArgumentMatcher(
      @Nonnegative int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc
   ) {
      return
         opcode == INVOKEVIRTUAL && owner.equals(blockOwner) &&
         ArgumentMatching.isCallToArgumentMatcher(name, desc);
   }

   private void generateCodeToReplaceNullWithZeroOnTopOfStack(@Nonnull String unboxingMethodDesc) {
      visitInsn(POP);
      char primitiveTypeCode = unboxingMethodDesc.charAt(2);

      int zeroOpcode;
      switch (primitiveTypeCode) {
         case 'J': zeroOpcode = LCONST_0; break;
         case 'F': zeroOpcode = FCONST_0; break;
         case 'D': zeroOpcode = DCONST_0; break;
         default: zeroOpcode = ICONST_0;
      }

      visitInsn(zeroOpcode);
   }

   private void handleMockedOrNonMockedInvocation(
      @Nonnegative int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf
   ) {
      if (argumentMatching.getMatcherCount() == 0) {
         visitMethodInstruction(opcode, owner, name, desc, itf);
      }
      else {
         boolean mockedInvocationUsingTheMatchers = argumentMatching.handleInvocationParameters(stackSize, desc);
         visitMethodInstruction(opcode, owner, name, desc, itf);
         handleArgumentCapturingIfNeeded(mockedInvocationUsingTheMatchers);
      }
   }

   private void handleArgumentCapturingIfNeeded(boolean mockedInvocationUsingTheMatchers) {
      if (mockedInvocationUsingTheMatchers) {
         argumentCapturing.generateCallsToCaptureMatchedArgumentsIfPending();
      }

      justAfterWithCaptureInvocation = false;
   }

   @Override
   public void visitLabel(@Nonnull Label label) {
      mw.visitLabel(label);

      if (!label.isDebug()) {
         stackSize = 0;
      }
   }

   @Override
   public void visitTypeInsn(@Nonnegative int opcode, @Nonnull String type) {
      argumentCapturing.registerTypeToCaptureIfApplicable(opcode, type);

      if (opcode == NEW) {
         stackSize++;
      }

      mw.visitTypeInsn(opcode, type);
   }

   @Override
   public void visitIntInsn(@Nonnegative int opcode, int operand) {
      if (opcode != NEWARRAY) {
         stackSize++;
      }

      mw.visitIntInsn(opcode, operand);
   }

   @Override
   public void visitVarInsn(@Nonnegative int opcode, @Nonnegative int varIndex) {
      if (opcode == ALOAD) {
         lastLoadedVarIndex = varIndex;
      }

      argumentCapturing.registerAssignmentToCaptureVariableIfApplicable(opcode, varIndex);

      if (opcode != RET) {
         stackSize += Frame.SIZE[opcode];
      }

      mw.visitVarInsn(opcode, varIndex);
   }

   @Override
   public void visitLdcInsn(Object cst) {
      stackSize++;

      if (cst instanceof Long || cst instanceof Double) {
         stackSize++;
      }

      mw.visitLdcInsn(cst);
   }

   @Override
   public void visitJumpInsn(@Nonnegative int opcode, Label label) {
      if (opcode != JSR) {
         stackSize += Frame.SIZE[opcode];
      }

      mw.visitJumpInsn(opcode, label);
   }

   @Override
   public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
      stackSize--;
      mw.visitTableSwitchInsn(min, max, dflt, labels);
   }

   @Override
   public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
      stackSize--;
      mw.visitLookupSwitchInsn(dflt, keys, labels);
   }

   @Override
   public void visitMultiANewArrayInsn(String desc, @Nonnegative int dims) {
      stackSize += 1 - dims;
      mw.visitMultiANewArrayInsn(desc, dims);
   }

   @Override
   public void visitInsn(@Nonnegative int opcode) {
      if (opcode == RETURN && callEndInvocations) {
         generateCallToActiveInvocationsMethod("endInvocations");
      }
      else {
         stackSize += Frame.SIZE[opcode];
      }

      mw.visitInsn(opcode);
   }

   @Override
   public void visitLocalVariable(
      @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nonnull Label start, @Nonnull Label end,
      @Nonnegative int index
   ) {
      if (signature != null) {
         argumentCapturing.registerTypeToCaptureIntoListIfApplicable(index, signature);
      }

      // In classes instrumented with EMMA some local variable information can be lost, so we discard it entirely to
      // avoid a ClassFormatError.
      if (end.position > 0) {
         mw.visitLocalVariable(name, desc, signature, start, end, index);
      }
   }

   @Nonnull MethodWriter getMethodWriter() { return mw; }
}
