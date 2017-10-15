package org.jmockit.internal.faking;

import org.jmockit.*;
import org.jmockit.external.asm.*;
import static org.jmockit.external.asm.ClassReader.*;
import org.jmockit.internal.*;
import org.jmockit.internal.startup.*;
import org.jmockit.internal.state.*;

import javax.annotation.*;
import java.lang.instrument.*;
import java.lang.reflect.*;
import java.lang.reflect.Type;

public final class FakeClassSetup
{
   @Nonnull private final Class<?> realClass;
   @Nullable private ClassReader rcReader;
   @Nonnull private final FakeMethods fakeMethods;
   @Nonnull private final Fake<?> fake;
   private final boolean forStartupFake;

   public FakeClassSetup(@Nonnull Class<?> classToFake, @Nullable Type fakedType, @Nonnull Fake<?> fake) {
      realClass = classToFake;
      fakeMethods = new FakeMethods(classToFake, fakedType);
      this.fake = fake;
      forStartupFake = Startup.initializing;

      Class<?> fakeClass = fake.getClass();
      new FakeMethodCollector(fakeMethods).collectFakeMethods(fakeClass);

      fakeMethods.registerFakeStates(fake, forStartupFake);

      FakeClasses fakeClasses = TestRun.getFakeClasses();

      if (forStartupFake) {
         fakeClasses.addFake(fakeMethods.getFakeClassInternalName(), fake);
      }
      else {
         fakeClasses.addFake(fake);
      }
   }

   public void redefineMethods() {
      @Nullable Class<?> classToModify = realClass;

      while (classToModify != null && fakeMethods.hasUnusedFakes()) {
         byte[] modifiedClassFile = modifyRealClass(classToModify);

         if (modifiedClassFile != null) {
            applyClassModifications(classToModify, modifiedClassFile);
         }

         Class<?> superClass = classToModify.getSuperclass();
         classToModify = superClass == Object.class || superClass == Proxy.class ? null : superClass;
         rcReader = null;
      }
   }

   @Nullable
   private byte[] modifyRealClass(@Nonnull Class<?> classToModify) {
      if (rcReader == null) {
         rcReader = ClassFile.createReaderFromLastRedefinitionIfAny(classToModify);
      }

      FakedClassModifier modifier = new FakedClassModifier(rcReader, classToModify, fake, fakeMethods);
      rcReader.accept(modifier, SKIP_FRAMES);

      return modifier.wasModified() ? modifier.toByteArray() : null;
   }

   private void applyClassModifications(@Nonnull Class<?> classToModify, @Nonnull byte[] modifiedClassFile) {
      ClassDefinition classDef = new ClassDefinition(classToModify, modifiedClassFile);
      Startup.redefineMethods(classDef);

      if (forStartupFake) {
         CachedClassfiles.addClassfile(classToModify, modifiedClassFile);
      }
      else {
         String fakeClassDesc = fakeMethods.getFakeClassInternalName();
         TestRun.mockFixture().addRedefinedClass(fakeClassDesc, classDef);
      }
   }
}
