package org.jmockit.internal.state;

import static java.lang.reflect.Modifier.*;
import org.jmockit.internal.*;
import org.jmockit.internal.expectations.mocking.*;
import org.jmockit.internal.startup.*;
import org.jmockit.internal.util.*;
import static org.jmockit.internal.util.GeneratedClasses.*;
import static org.jmockit.internal.util.Utilities.*;

import javax.annotation.*;
import java.lang.instrument.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;

/**
 * Holds data about redefined/transformed classes, with methods to add/remove and query such data.
 */
public final class MockFixture
{
   /**
    * Similar to {@link #redefinedClasses}, but for classes modified by a {@code ClassFileTransformer} such as the
    * {@code CaptureTransformer}, and containing the pre-transform bytecode instead of the modified one.
    *
    * @see #addTransformedClass(ClassIdentification, byte[])
    * @see #getTransformedClasses()
    * @see #restoreTransformedClasses(Set)
    */
   @Nonnull private final Map<ClassIdentification, byte[]> transformedClasses;

   /**
    * Real classes currently redefined in the running JVM and their current (modified) bytecodes.
    * <p/>
    * The keys in the map allow each redefined real class to be later restored to a previous definition.
    * <p/>
    * The modified bytecode arrays in the map allow a new redefinition to be made on top of the current redefinition
    * (in the case of the Faking API), or to restore the class to a previous definition (provided the map is copied
    * between redefinitions of the same class).
    *
    * @see #addRedefinedClass(ClassDefinition)
    * @see #getRedefinedClasses()
    * @see #getRedefinedClassfile(Class)
    * @see #containsRedefinedClass(Class)
    * @see #restoreRedefinedClasses(Map)
    */
   @Nonnull private final Map<Class<?>, byte[]> redefinedClasses;

   /**
    * Subset of all currently redefined classes which contain one or more native methods.
    * <p/>
    * This is needed because in order to restore such methods it is necessary (for some classes) to re-register them
    * with the JVM.
    *
    * @see #addRedefinedClassWithNativeMethods(String)
    * @see #reregisterNativeMethodsForRestoredClass(Class)
    */
   @Nonnull private final Set<String> redefinedClassesWithNativeMethods;

   /**
    * Maps redefined real classes to the internal name of the corresponding fake classes, when it's the case.
    * <p/>
    * This allows any global state associated to a fake class to be discarded when the corresponding real class is later
    * restored to its original definition.
    *
    * @see #addRedefinedClass(String, ClassDefinition)
    */
   @Nonnull private final Map<Class<?>, String> realClassesToFakeClasses;

   /**
    * A list of classes that are currently mocked.
    * Said classes are also added to {@link #mockedTypesAndInstances}.
    *
    * @see #registerMockedClass(Class)
    * @see #getMockedClasses()
    * @see #isStillMocked(Object, String)
    * @see #isInstanceOfMockedClass(Object)
    * @see #removeMockedClasses(List)
    */
   @Nonnull private final List<Class<?>> mockedClasses;

   /**
    * A map of mocked types to their corresponding {@linkplain InstanceFactory mocked instance factories}.
    *
    * @see #registerInstanceFactoryForMockedType(Class, InstanceFactory)
    * @see #findInstanceFactory(Type)
    * @see #isStillMocked(Object, String)
    * @see #removeMockedClasses(List)
    */
   @Nonnull private final Map<Type, InstanceFactory> mockedTypesAndInstances;

   public MockFixture()
   {
      transformedClasses = new HashMap<>(2);
      redefinedClasses = new ConcurrentHashMap<>(8);
      redefinedClassesWithNativeMethods = new HashSet<>();
      realClassesToFakeClasses = new IdentityHashMap<>(8);
      mockedClasses = new ArrayList<>();
      mockedTypesAndInstances = new IdentityHashMap<>();
   }

   // Methods to add/remove transformed/redefined classes /////////////////////////////////////////////////////////////

   public void addTransformedClass(@Nonnull ClassIdentification classId, @Nonnull byte[] pretransformClassfile)
   {
      transformedClasses.put(classId, pretransformClassfile);
   }

   // Methods used by both the Mocking and Faking APIs.

   public void addRedefinedClass(@Nonnull ClassDefinition newClassDefinition)
   {
      redefinedClasses.put(newClassDefinition.getDefinitionClass(), newClassDefinition.getDefinitionClassFile());
   }

   public void registerMockedClass(@Nonnull Class<?> mockedType)
   {
      if (!isMockedClass(mockedType)) {
         if (Proxy.isProxyClass(mockedType)) {
            mockedType = mockedType.getInterfaces()[0];
         }

         mockedClasses.add(mockedType);
      }
   }

   private boolean isMockedClass(@Nonnull Class<?> targetClass)
   {
      int n = mockedClasses.size();

      for (int i = 0; i < n; i++) {
         Class<?> mockedClass = mockedClasses.get(i);

         if (mockedClass == targetClass) {
            return true;
         }
      }

      return false;
   }

   // Methods used by the Mocking API.

   public void redefineClasses(@Nonnull ClassDefinition... definitions)
   {
      Startup.redefineMethods(definitions);

      for (ClassDefinition def : definitions) {
         addRedefinedClass(def);
      }
   }

   public void redefineMethods(@Nonnull Map<Class<?>, byte[]> modifiedClassfiles)
   {
      ClassDefinition[] classDefs = new ClassDefinition[modifiedClassfiles.size()];
      int i = 0;

      for (Entry<Class<?>, byte[]> classAndBytecode : modifiedClassfiles.entrySet()) {
         Class<?> modifiedClass = classAndBytecode.getKey();
         byte[] modifiedClassfile = classAndBytecode.getValue();

         ClassDefinition classDef = new ClassDefinition(modifiedClass, modifiedClassfile);
         classDefs[i++] = classDef;

         addRedefinedClass(classDef);
      }

      Startup.redefineMethods(classDefs);
   }

   public boolean isStillMocked(@Nullable Object instance, @Nonnull String classDesc)
   {
      Class<?> targetClass;

      if (instance == null) {
         targetClass = ClassLoad.loadByInternalName(classDesc);
         return isClassAssignableTo(mockedClasses, targetClass);
      }

      targetClass = instance.getClass();
      return mockedTypesAndInstances.containsKey(targetClass) || isInstanceOfMockedClass(instance);
   }

   public boolean isInstanceOfMockedClass(@Nonnull Object mockedInstance)
   {
      Class<?> mockedClass = mockedInstance.getClass();
      return findClassAssignableFrom(mockedClasses, mockedClass) != null;
   }

   public void registerInstanceFactoryForMockedType(
      @Nonnull Class<?> mockedType, @Nonnull InstanceFactory mockedInstanceFactory)
   {
      registerMockedClass(mockedType);
      mockedTypesAndInstances.put(mockedType, mockedInstanceFactory);
   }

   @Nullable
   public InstanceFactory findInstanceFactory(@Nonnull Type mockedType)
   {
      InstanceFactory instanceFactory = mockedTypesAndInstances.get(mockedType);

      if (instanceFactory != null) {
         return instanceFactory;
      }

      Class<?> mockedClass = getClassType(mockedType);
      //noinspection ReuseOfLocalVariable
      instanceFactory = mockedTypesAndInstances.get(mockedClass);

      if (instanceFactory != null) {
         return instanceFactory;
      }

      boolean abstractType = mockedClass.isInterface() || isAbstract(mockedClass.getModifiers());

      for (Entry<Type, InstanceFactory> entry : mockedTypesAndInstances.entrySet()) {
         Type registeredMockedType = entry.getKey();
         Class<?> registeredMockedClass = getClassType(registeredMockedType);

         if (abstractType) {
            registeredMockedClass = getMockedClassOrInterfaceType(registeredMockedClass);
         }

         if (mockedClass.isAssignableFrom(registeredMockedClass)) {
            instanceFactory = entry.getValue();
            break;
         }
      }

      return instanceFactory;
   }

   // Methods used by the Faking API.

   public void addRedefinedClass(@Nonnull String fakeClassInternalName, @Nonnull ClassDefinition classDef)
   {
      @Nonnull Class<?> redefinedClass = classDef.getDefinitionClass();
      String previousNames = realClassesToFakeClasses.put(redefinedClass, fakeClassInternalName);

      if (previousNames != null) {
         realClassesToFakeClasses.put(redefinedClass, previousNames + ' ' + fakeClassInternalName);
      }

      addRedefinedClass(classDef);
   }

   // Methods used by test save-points ////////////////////////////////////////////////////////////////////////////////

   void restoreTransformedClasses(@Nonnull Set<ClassIdentification> previousTransformedClasses)
   {
      if (!transformedClasses.isEmpty()) {
         Set<ClassIdentification> classesToRestore;

         if (previousTransformedClasses.isEmpty()) {
            classesToRestore = transformedClasses.keySet();
         }
         else {
            classesToRestore = getTransformedClasses();
            classesToRestore.removeAll(previousTransformedClasses);
         }

         if (!classesToRestore.isEmpty()) {
            restoreAndRemoveTransformedClasses(classesToRestore);
         }
      }
   }

   @Nonnull
   Set<ClassIdentification> getTransformedClasses()
   {
      return transformedClasses.isEmpty() ? Collections.<ClassIdentification>emptySet() : new HashSet<>(transformedClasses.keySet());
   }

   @Nonnull
   Map<Class<?>, byte[]> getRedefinedClasses()
   {
      return redefinedClasses.isEmpty() ? Collections.<Class<?>, byte[]>emptyMap() : new HashMap<>(redefinedClasses);
   }

   private void restoreAndRemoveTransformedClasses(@Nonnull Set<ClassIdentification> classesToRestore)
   {
      for (ClassIdentification transformedClassId : classesToRestore) {
         byte[] definitionToRestore = transformedClasses.get(transformedClassId);
         Startup.redefineMethods(transformedClassId, definitionToRestore);
      }

      transformedClasses.keySet().removeAll(classesToRestore);
   }

   void restoreRedefinedClasses(@Nonnull Map<?, byte[]> previousDefinitions)
   {
      if (redefinedClasses.isEmpty()) {
         return;
      }

      Iterator<Entry<Class<?>, byte[]>> itr = redefinedClasses.entrySet().iterator();

      while (itr.hasNext()) {
         Entry<Class<?>, byte[]> entry = itr.next();
         Class<?> redefinedClass = entry.getKey();
         byte[] currentDefinition = entry.getValue();
         byte[] previousDefinition = previousDefinitions.get(redefinedClass);

         if (previousDefinition == null) {
            restoreDefinition(redefinedClass);
            itr.remove();
         }
         else if (currentDefinition != previousDefinition) {
            Startup.redefineMethods(redefinedClass, previousDefinition);
            entry.setValue(previousDefinition);
         }
      }
   }

   private void restoreDefinition(@Nonnull Class<?> redefinedClass)
   {
      if (!isGeneratedImplementationClass(redefinedClass)) {
         byte[] previousDefinition = ClassFile.createReaderOrGetFromCache(redefinedClass).b;
         Startup.redefineMethods(redefinedClass, previousDefinition);
      }

      if (redefinedClassesWithNativeMethods.contains(redefinedClass.getName())) {
         reregisterNativeMethodsForRestoredClass(redefinedClass);
      }

      removeMockedClass(redefinedClass);
      discardStateForCorrespondingFakeClassIfAny(redefinedClass);
   }

   private void removeMockedClass(@Nonnull Class<?> mockedClass)
   {
      mockedTypesAndInstances.remove(mockedClass);
      mockedClasses.remove(mockedClass);
   }

   private void discardStateForCorrespondingFakeClassIfAny(@Nonnull Class<?> redefinedClass)
   {
      String mockClassesInternalNames = realClassesToFakeClasses.remove(redefinedClass);
      TestRun.getFakeStates().removeClassState(redefinedClass, mockClassesInternalNames);
   }

   void removeMockedClasses(@Nonnull List<Class<?>> previousMockedClasses)
   {
      int currentMockedClassCount = mockedClasses.size();

      if (currentMockedClassCount > 0) {
         int previousMockedClassCount = previousMockedClasses.size();

         if (previousMockedClassCount == 0) {
            mockedClasses.clear();
            mockedTypesAndInstances.clear();
         }
         else if (previousMockedClassCount < currentMockedClassCount) {
            mockedClasses.retainAll(previousMockedClasses);
            mockedTypesAndInstances.keySet().retainAll(previousMockedClasses);
         }
      }
   }

   // Methods that deal with redefined native methods /////////////////////////////////////////////////////////////////

   public void addRedefinedClassWithNativeMethods(@Nonnull String redefinedClassInternalName)
   {
      redefinedClassesWithNativeMethods.add(redefinedClassInternalName.replace('/', '.'));
   }

   private static void reregisterNativeMethodsForRestoredClass(@Nonnull Class<?> realClass)
   {
      Method registerNatives = null;

      try {
         registerNatives = realClass.getDeclaredMethod("registerNatives");
      }
      catch (NoSuchMethodException ignore) {
         try { registerNatives = realClass.getDeclaredMethod("initIDs"); }
         catch (NoSuchMethodException ignored) {} // OK
      }

      if (registerNatives != null) {
         try {
            registerNatives.setAccessible(true);
            registerNatives.invoke(null);
         }
         catch (IllegalAccessException ignore)    {} // won't happen
         catch (InvocationTargetException ignore) {} // shouldn't happen either
      }

      // OK, although another solution will be required for this particular class if it requires
      // natives to be explicitly registered again (not all do, such as java.lang.Float).
   }

   // Getter methods for the maps and collections of transformed/redefined/mocked classes /////////////////////////////

   @Nullable
   public byte[] getRedefinedClassfile(@Nonnull Class<?> redefinedClass)
   {
      return redefinedClasses.get(redefinedClass);
   }

   public boolean containsRedefinedClass(@Nonnull Class<?> redefinedClass)
   {
      return redefinedClasses.containsKey(redefinedClass);
   }

   @Nonnull
   public List<Class<?>> getMockedClasses()
   {
      return mockedClasses.isEmpty() ? Collections.<Class<?>>emptyList() : new ArrayList<>(mockedClasses);
   }
}
