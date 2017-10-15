package org.jmockit.internal.faking;

import org.jmockit.internal.faking.FakeMethods.*;
import org.jmockit.internal.reflection.*;

import javax.annotation.*;
import java.lang.reflect.*;

final class FakeState
{
   @Nonnull final FakeMethod fakeMethod;
   @Nullable private Method actualFakeMethod;
   @Nullable private Member realMethodOrConstructor;
   @Nullable private Object realClass;

   // Current fake invocation state:
   private int invocationCount;
   @Nullable private ThreadLocal<FakeInvocation> proceedingInvocation;

   // Helper field just for synchronization:
   @Nonnull private final Object invocationCountLock;

   FakeState(@Nonnull FakeMethod fakeMethod)
   {
      this.fakeMethod = fakeMethod;
      invocationCountLock = new Object();

      if (fakeMethod.canBeReentered()) {
         makeReentrant();
      }
   }

   FakeState(@Nonnull FakeState fakeState)
   {
      fakeMethod = fakeState.fakeMethod;
      actualFakeMethod = fakeState.actualFakeMethod;
      realMethodOrConstructor = fakeState.realMethodOrConstructor;
      invocationCountLock = new Object();

      if (fakeState.proceedingInvocation != null) {
         makeReentrant();
      }
   }

   @Nonnull Class<?> getRealClass() { return fakeMethod.getRealClass(); }

   private void makeReentrant() { proceedingInvocation = new ThreadLocal<FakeInvocation>(); }

   boolean update()
   {
      if (proceedingInvocation != null) {
         FakeInvocation invocation = proceedingInvocation.get();

         if (invocation != null && invocation.proceeding) {
            invocation.proceeding = false;
            return false;
         }
      }

      synchronized (invocationCountLock) {
         invocationCount++;
      }

      return true;
   }

   int getTimesInvoked()
   {
      synchronized (invocationCountLock) {
         return invocationCount;
      }
   }

   @Nonnull
   Member getRealMethodOrConstructor(
      @Nonnull String fakedClassDesc, @Nonnull String fakedMethodName, @Nonnull String fakedMethodDesc)
   {
      if (realMethodOrConstructor == null || !fakedClassDesc.equals(realClass)) {
         String memberName = "$init".equals(fakedMethodName) ? "<init>" : fakedMethodName;

         RealMethodOrConstructor realMember;
         try { realMember = new RealMethodOrConstructor(fakedClassDesc, memberName, fakedMethodDesc); }
         catch (NoSuchMethodException e) { throw new RuntimeException(e); }

         Member member = realMember.getMember();

         if (fakeMethod.isAdvice) {
            return member;
         }

         realMethodOrConstructor = member;
         realClass = fakedClassDesc;
      }

      return realMethodOrConstructor;
   }

   @Nonnull
   Member getRealMethodOrConstructor(
      @Nonnull Class<?> fakedClass, @Nonnull String fakedMethodName, @Nonnull String fakedMethodDesc)
   {
      if (realMethodOrConstructor == null || !fakedClass.equals(realClass)) {
         String memberName = "$init".equals(fakedMethodName) ? "<init>" : fakedMethodName;

         RealMethodOrConstructor realMember;
         try { realMember = new RealMethodOrConstructor(fakedClass, memberName, fakedMethodDesc); }
         catch (NoSuchMethodException e) { throw new RuntimeException(e); }

         Member member = realMember.getMember();

         if (fakeMethod.isAdvice) {
            return member;
         }

         realMethodOrConstructor = member;
         realClass = fakedClass;
      }

      return realMethodOrConstructor;
   }

   boolean shouldProceedIntoRealImplementation(@Nullable Object fake, @Nonnull String classDesc)
   {
      if (proceedingInvocation != null) {
         FakeInvocation pendingInvocation = proceedingInvocation.get();

         if (pendingInvocation != null && pendingInvocation.isMethodInSuperclass(fake, classDesc)) {
            return true;
         }
      }

      return false;
   }

   void prepareToProceed(@Nonnull FakeInvocation invocation)
   {
      if (proceedingInvocation == null) {
         throw new UnsupportedOperationException("Cannot proceed into abstract/interface method");
      }

      if (fakeMethod.isForNativeMethod()) {
         throw new UnsupportedOperationException("Cannot proceed into real implementation of native method");
      }

      FakeInvocation previousInvocation = proceedingInvocation.get();

      if (previousInvocation != null) {
         invocation.setPrevious(previousInvocation);
      }

      proceedingInvocation.set(invocation);
   }

   void prepareToProceedFromNonRecursiveFake(@Nonnull FakeInvocation invocation)
   {
      assert proceedingInvocation != null;
      proceedingInvocation.set(invocation);
   }

   void clearProceedIndicator()
   {
      assert proceedingInvocation != null;
      FakeInvocation currentInvocation = proceedingInvocation.get();
      FakeInvocation previousInvocation = (FakeInvocation) currentInvocation.getPrevious();
      proceedingInvocation.set(previousInvocation);
   }

   @Nonnull
   Method getFakeMethod(@Nonnull Class<?> fakeClass, @Nonnull Class<?>[] parameterTypes)
   {
      if (actualFakeMethod == null) {
         actualFakeMethod = MethodReflection.findCompatibleMethod(fakeClass, fakeMethod.name, parameterTypes);
      }

      return actualFakeMethod;
   }

   @Override @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
   public boolean equals(@Nonnull Object other) { return fakeMethod.equals(((FakeState) other).fakeMethod); }

   @Override
   public int hashCode() { return fakeMethod.hashCode(); }
}
