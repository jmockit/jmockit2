package org.jmockit.internal.util;

import javax.annotation.*;

public final class ClassNaming
{
   private ClassNaming() {}

   /**
    * This method was created to work around an issue in the standard {@link Class#isAnonymousClass()} method, which
    * causes a sibling nested class to be loaded when called on a nested class. If that sibling nested class is not in
    * the classpath, a {@code ClassNotFoundException} would result.
    * <p/>
    * This method checks only the given class name, never causing any other classes to be loaded.
    */
   public static boolean isAnonymousClass(@Nonnull Class<?> aClass)
   {
      return isAnonymousClass(aClass.getName());
   }

   public static boolean isAnonymousClass(@Nonnull String className)
   {
      int p = className.lastIndexOf('$');

      if (p <= 0) {
         return false;
      }

      return isAllNumeric(className, p + 1);
   }

   private static boolean isAllNumeric(@Nonnull String className, @Nonnegative int initialPosition)
   {
      int nextPos = initialPosition;
      int n = className.length();

      while (nextPos < n) {
         char c = className.charAt(nextPos);
         if (c < '0' || c > '9') return false;
         nextPos++;
      }

      return true;
   }
}
