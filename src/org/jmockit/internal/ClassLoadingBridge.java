package org.jmockit.internal;

import org.jmockit.internal.faking.*;
import org.jmockit.internal.startup.*;
import org.jmockit.internal.util.*;

import javax.annotation.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.jar.*;

public abstract class ClassLoadingBridge implements InvocationHandler
{
   private static final Object[] EMPTY_ARGS = {};
   private static final ReentrantLock LOCK = new ReentrantLock();
   private static boolean fieldsSet;
   public final String id;

   /**
    * The instance is stored in a place directly accessible through the Java SE API, so that it can be recovered from
    * any class loader.
    */
   protected ClassLoadingBridge(@Nonnull String id) { this.id = id; }

   protected static boolean notToBeMocked(@Nullable Object instance, @Nonnull String classDesc)
   {
      return
         (instance == null && "java/lang/System".equals(classDesc) ||
          instance != null && instanceOfClassThatParticipatesInClassLoading(instance.getClass())
         ) && wasCalledDuringClassLoading();
   }

   public static boolean instanceOfClassThatParticipatesInClassLoading(@Nonnull Class<?> aClass)
   {
      return
         aClass == System.class || aClass == File.class || aClass == URL.class ||
         aClass == FileInputStream.class || aClass == Manifest.class ||
         JarFile.class.isAssignableFrom(aClass) || JarEntry.class.isAssignableFrom(aClass) ||
         Vector.class.isAssignableFrom(aClass) || Hashtable.class.isAssignableFrom(aClass);
   }

   private static boolean wasCalledDuringClassLoading()
   {
      if (LOCK.isHeldByCurrentThread()) {
         return true;
      }

      LOCK.lock();

      try {
         StackTrace st = new StackTrace(new Throwable());
         int n = st.getDepth();

         for (int i = 3; i < n; i++) {
            StackTraceElement ste = st.getElement(i);

            if (
               "ClassLoader.java".equals(ste.getFileName()) &&
               "loadClass getResource loadLibrary".contains(ste.getMethodName())
            ) {
               return true;
            }
         }

         return false;
      }
      finally {
         LOCK.unlock();
      }
   }

   @Nonnull
   protected static Object[] extractArguments(@Nonnegative int startingIndex, @Nonnull Object[] args)
   {
      if (args.length > startingIndex) {
         Object[] targetMemberArgs = new Object[args.length - startingIndex];
         System.arraycopy(args, startingIndex, targetMemberArgs, 0, targetMemberArgs.length);
         return targetMemberArgs;
      }

      return EMPTY_ARGS;
   }

   @Nonnull
   static String getHostClassName()
   {
      if (!fieldsSet) {
         setBridgeFields();
         fieldsSet = true;
      }

      return InstrumentationHolder.hostJREClassName;
   }

   private static void setBridgeFields()
   {
      Class<?> hostClass = ClassLoad.loadByInternalName(InstrumentationHolder.hostJREClassName);
      setBridgeField(hostClass, FakeBridge.MB);
      setBridgeField(hostClass, FakeMethodBridge.MB);
   }

   private static void setBridgeField(@Nonnull Class<?> hostClass, @Nonnull ClassLoadingBridge bridge)
   {
      try {
         hostClass.getDeclaredField(bridge.id).set(null, bridge);
      }
      catch (NoSuchFieldException ignore) {}
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
   }
}
