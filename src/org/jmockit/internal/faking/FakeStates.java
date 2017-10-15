package org.jmockit.internal.faking;

import org.jmockit.internal.util.*;

import javax.annotation.*;
import java.util.*;
import java.util.Map.*;
import java.util.regex.*;

/**
 * Holds state associated with a fake class containing fake methods.
 */
public final class FakeStates
{
   private static final Pattern SPACE = Pattern.compile(" ");

   /**
    * For each fake instance and each {@code @Mock} method containing the {@code Invocation} parameter, a runtime state
    * will be kept here.
    */
   @Nonnull private final Map<Object, List<FakeState>> fakesToFakeStates;
   @Nonnull private final Map<Object, List<FakeState>> startupFakesToFakeStates;

   public FakeStates()
   {
      startupFakesToFakeStates = new IdentityHashMap<Object, List<FakeState>>(2);
      fakesToFakeStates = new IdentityHashMap<Object, List<FakeState>>(8);
   }

   void addStartupFakeAndItsFakeStates(@Nonnull Object fake, @Nonnull List<FakeState> fakeStates)
   {
      startupFakesToFakeStates.put(fake, fakeStates);
   }

   void addFakeAndItsFakeStates(@Nonnull Object fake, @Nonnull List<FakeState> fakeStates)
   {
      fakesToFakeStates.put(fake, fakeStates);
   }

   public void copyFakeStates(@Nonnull Object previousFake, @Nonnull Object newFake)
   {
      List<FakeState> fakeStates = fakesToFakeStates.get(previousFake);

      if (fakeStates != null) {
         List<FakeState> copiedFakeStates = new ArrayList<FakeState>(fakeStates.size());

         for (FakeState fakeState : fakeStates) {
            copiedFakeStates.add(new FakeState(fakeState));
         }

         fakesToFakeStates.put(newFake, copiedFakeStates);
      }
   }

   public void removeClassState(@Nonnull Class<?> redefinedClass, @Nullable String internalNameForOneOrMoreFakeClasses)
   {
      removeFakeStates(redefinedClass);

      if (internalNameForOneOrMoreFakeClasses != null) {
         if (internalNameForOneOrMoreFakeClasses.indexOf(' ') < 0) {
            removeFakeStates(internalNameForOneOrMoreFakeClasses);
         }
         else {
            String[] fakeClassesInternalNames = SPACE.split(internalNameForOneOrMoreFakeClasses);

            for (String fakeClassInternalName : fakeClassesInternalNames) {
               removeFakeStates(fakeClassInternalName);
            }
         }
      }
   }

   private void removeFakeStates(@Nonnull Class<?> redefinedClass)
   {
      Iterator<List<FakeState>> itr = fakesToFakeStates.values().iterator();

      while (itr.hasNext()) {
         List<FakeState> fakeStates = itr.next();
         FakeState fakeState = fakeStates.get(0);

         if (fakeState.getRealClass() == redefinedClass) {
            fakeStates.clear();
            itr.remove();
         }
      }
   }

   private void removeFakeStates(@Nonnull String fakeClassInternalName)
   {
      Class<?> fakeClass = ClassLoad.loadClass(fakeClassInternalName.replace('/', '.'));
      Iterator<Entry<Object, List<FakeState>>> itr = fakesToFakeStates.entrySet().iterator();

      while (itr.hasNext()) {
         Entry<Object, List<FakeState>> fakeAndFakeStates = itr.next();
         Object fake = fakeAndFakeStates.getKey();

         if (fake.getClass() == fakeClass) {
            itr.remove();
         }
      }
   }

   public boolean updateFakeState(@Nonnull Object fake, int fakeStateIndex)
   {
      FakeState fakeState = getFakeState(fake, fakeStateIndex);
      return fakeState.update();
   }

   @Nonnull
   FakeState getFakeState(@Nonnull Object fake, int fakeStateIndex)
   {
      List<FakeState> fakeStates = startupFakesToFakeStates.get(fake);

      if (fakeStates == null) {
         fakeStates = fakesToFakeStates.get(fake);
      }

      FakeState fakeState = fakeStates.get(fakeStateIndex);
      assert fakeState != null;
      return fakeState;
   }
}
