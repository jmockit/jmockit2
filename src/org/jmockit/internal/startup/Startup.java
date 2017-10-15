package org.jmockit.internal.startup;

import org.jmockit.internal.*;
import org.jmockit.internal.expectations.transformation.*;
import org.jmockit.internal.state.*;

import javax.annotation.*;
import java.lang.instrument.*;

/**
 * This is the "agent class" that initializes the JMockit "Java agent". It is not intended for use in client code.
 *
 * @see #premain(String, Instrumentation)
 */
public final class Startup
{
   private static Instrumentation instrumentation;
   public static boolean initializing;
   public static String hostJREClassName;

   private Startup() {}

   /**
    * This method must only be called by the JVM, to provide the instrumentation object.
    * In order for this to occur, the JVM must be started with "-javaagent:jmockit-2.x.jar" as a command line parameter
    * (assuming the jar file is in the current directory).
    * <p/>
    * It is also possible to load user-specified fakes at this time, by having set the "fakes" system property.
    *
    * @param agentArgs not used
    * @param inst      the instrumentation service provided by the JVM
    */
   public static void premain(@Nullable String agentArgs, @Nonnull Instrumentation inst) {
      instrumentation = inst;
      hostJREClassName = ClassLoadingBridgeFields.createSyntheticFieldsInJREClassToHoldClassLoadingBridges(inst);
      inst.addTransformer(CachedClassfiles.INSTANCE, true);
      applyStartupFakes();
      inst.addTransformer(new ExpectationsTransformer(inst));
   }

   private static void applyStartupFakes() {
      initializing = true;

      try {
         new JMockitInitialization().initialize();
      }
      finally {
         initializing = false;
      }
   }

   public static void retransformClass(@Nonnull Class<?> aClass) {
      try { instrumentation.retransformClasses(aClass); } catch (UnmodifiableClassException ignore) {}
   }

   public static void redefineMethods(@Nonnull ClassIdentification classToRedefine, @Nonnull byte[] modifiedClassfile) {
      Class<?> loadedClass = classToRedefine.getLoadedClass();
      redefineMethods(loadedClass, modifiedClassfile);
   }

   public static void redefineMethods(@Nonnull Class<?> classToRedefine, @Nonnull byte[] modifiedClassfile) {
      redefineMethods(new ClassDefinition(classToRedefine, modifiedClassfile));
   }

   public static void redefineMethods(@Nonnull ClassDefinition... classDefs) {
      try {
         instrumentation.redefineClasses(classDefs);
      }
      catch (ClassNotFoundException | UnmodifiableClassException e) {
         // should never happen
         throw new RuntimeException(e);
      }
      catch (InternalError ignore) {
         // If a class to be redefined hasn't been loaded yet, the JVM may get a NoClassDefFoundError during
         // redefinition. Unfortunately, it then throws a plain InternalError instead.
         for (ClassDefinition classDef : classDefs) {
            detectMissingDependenciesIfAny(classDef.getDefinitionClass());
         }

         // If the above didn't throw upon detecting a NoClassDefFoundError, then ignore the original error and
         // continue, in order to prevent secondary failures.
      }
   }

   private static void detectMissingDependenciesIfAny(@Nonnull Class<?> mockedClass) {
      try {
         Class.forName(mockedClass.getName(), false, mockedClass.getClassLoader());
      }
      catch (NoClassDefFoundError e) {
         throw new RuntimeException("Unable to mock " + mockedClass + " due to a missing dependency", e);
      }
      catch (ClassNotFoundException ignore) {
         // Shouldn't happen since the mocked class would already have been found in the classpath.
      }
   }

   @Nullable
   public static Class<?> getClassIfLoaded(@Nonnull String classDescOrName) {
      String className = classDescOrName.replace('/', '.');

      for (Class<?> aClass : instrumentation.getAllLoadedClasses()) {
         if (aClass.getName().equals(className)) {
            return aClass;
         }
      }

      return null;
   }
}
