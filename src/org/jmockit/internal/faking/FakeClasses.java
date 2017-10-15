package org.jmockit.internal.faking;

import org.jmockit.*;
import org.jmockit.internal.util.*;

import javax.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;

public final class FakeClasses
{
   private static final Method ON_TEAR_DOWN_METHOD;
   static {
      try {
         ON_TEAR_DOWN_METHOD = Fake.class.getDeclaredMethod("onTearDown");
         ON_TEAR_DOWN_METHOD.setAccessible(true);
      }
      catch (NoSuchMethodException e) { throw new RuntimeException(e); }
   }

   private static void notifyOfTearDown(@Nonnull Fake<?> fake) {
      try { ON_TEAR_DOWN_METHOD.invoke(fake); }
      catch (IllegalAccessException ignore) {}
      catch (InvocationTargetException e) { e.getCause().printStackTrace(); }
   }

   @Nonnull private final Map<String, Fake<?>> startupFakes;
   @Nonnull private final Map<Class<?>, Fake<?>> fakeClassesToFakeInstances;
   @Nonnull public final FakeStates fakeStates;

   public FakeClasses() {
      startupFakes = new IdentityHashMap<>(8);
      fakeClassesToFakeInstances = new IdentityHashMap<>();
      fakeStates = new FakeStates();
   }

   void addFake(@Nonnull String fakeClassDesc, @Nonnull Fake<?> fake) {
      startupFakes.put(fakeClassDesc, fake);
   }

   void addFake(@Nonnull Fake<?> fake) {
      Class<?> fakeClass = fake.getClass();
      fakeClassesToFakeInstances.put(fakeClass, fake);
   }

   @Nonnull
   public Fake<?> getFake(@Nonnull String fakeClassDesc) {
      Fake<?> startupFake = startupFakes.get(fakeClassDesc);

      if (startupFake != null) {
         return startupFake;
      }

      Class<?> fakeClass = ClassLoad.loadByInternalName(fakeClassDesc);
      Fake<?> fakeInstance = fakeClassesToFakeInstances.get(fakeClass);
      return fakeInstance;
   }

   @Nullable
   public Fake<?> findPreviouslyAppliedFake(@Nonnull Fake<?> newFake) {
      Class<?> fakeClass = newFake.getClass();
      Fake<?> fakeInstance = fakeClassesToFakeInstances.get(fakeClass);

      if (fakeInstance != null) {
         fakeStates.copyFakeStates(fakeInstance, newFake);
         return fakeInstance;
      }

      return null;
   }

   private void discardFakeInstancesExceptPreviousOnes(@Nonnull Map<Class<?>, Boolean> previousFakeClasses) {
      for (Entry<Class<?>, Fake<?>> fakeClassAndInstances : fakeClassesToFakeInstances.entrySet()) {
         Class<?> fakeClass = fakeClassAndInstances.getKey();

         if (!previousFakeClasses.containsKey(fakeClass)) {
            Fake<?> fakeInstance = fakeClassAndInstances.getValue();
            notifyOfTearDown(fakeInstance);
         }
      }

      fakeClassesToFakeInstances.keySet().retainAll(previousFakeClasses.keySet());
   }

   private void discardAllFakeInstances() {
      if (!fakeClassesToFakeInstances.isEmpty()) {
         for (Fake<?> fakeInstance : fakeClassesToFakeInstances.values()) {
            notifyOfTearDown(fakeInstance);
         }

         fakeClassesToFakeInstances.clear();
      }
   }

   public void discardStartupFakes() {
      for (Fake<?> startupFake : startupFakes.values()) {
         notifyOfTearDown(startupFake);
      }
   }

   public final class SavePoint
   {
      @Nonnull private final Map<Class<?>, Boolean> previousFakeClasses;

      public SavePoint() {
         previousFakeClasses = new IdentityHashMap<>();

         for (Entry<Class<?>, Fake<?>> fakeClassAndInstance : fakeClassesToFakeInstances.entrySet()) {
            Class<?> fakeClass = fakeClassAndInstance.getKey();
            previousFakeClasses.put(fakeClass, false);
         }
      }

      public void rollback() {
         if (previousFakeClasses.isEmpty()) {
            discardAllFakeInstances();
         }
         else {
            discardFakeInstancesExceptPreviousOnes(previousFakeClasses);
         }
      }
   }
}
