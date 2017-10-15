package org.jmockit.internal.reflection;

import javax.annotation.*;

public final class ThrowOfCheckedException
{
   private static Exception exceptionToThrow;

   ThrowOfCheckedException() throws Exception { throw exceptionToThrow; }

   public static synchronized void doThrow(@Nonnull Exception checkedException)
   {
      exceptionToThrow = checkedException;
      ConstructorReflection.newInstanceUsingDefaultConstructor(ThrowOfCheckedException.class);
   }
}
