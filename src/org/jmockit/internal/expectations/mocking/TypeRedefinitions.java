package org.jmockit.internal.expectations.mocking;

import org.jmockit.internal.state.*;

import javax.annotation.*;
import java.lang.reflect.*;
import java.util.*;

public class TypeRedefinitions
{
   @Nonnull private final List<Class<?>> targetClasses;

   protected TypeRedefinitions() { targetClasses = new ArrayList<Class<?>>(2); }

   protected final void addTargetClass(@Nonnull MockedType mockedType)
   {
      Class<?> targetClass = mockedType.getClassType();

      if (targetClass != TypeVariable.class) {
         targetClasses.add(targetClass);
      }
   }

   @Nonnull public final List<Class<?>> getTargetClasses() { return targetClasses; }

   protected static void registerMock(@Nonnull MockedType mockedType, @Nonnull Object mock)
   {
      TestRun.getExecutingTest().registerMock(mockedType, mock);
   }

   public void cleanUp()
   {
   }
}
