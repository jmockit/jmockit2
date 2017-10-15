package org.jmockit.internal.util;

import javax.annotation.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

public final class TestMethod
{
   @Nonnull public final String testClassDesc;
   @Nonnull public final String testMethodDesc;
   @Nonnull private final Type[] parameterTypes;
   @Nonnull private final Class<?>[] parameterClasses;
   @Nonnull private final Annotation[][] parameterAnnotations;
   @Nonnull private final Object[] parameterValues;

   public TestMethod(@Nonnull Method testMethod, @Nonnull Object[] parameterValues)
   {
      testClassDesc = org.jmockit.external.asm.Type.getInternalName(testMethod.getDeclaringClass());
      testMethodDesc = testMethod.getName() + org.jmockit.external.asm.Type.getMethodDescriptor(testMethod);
      parameterTypes = testMethod.getGenericParameterTypes();
      parameterClasses = testMethod.getParameterTypes();
      parameterAnnotations = testMethod.getParameterAnnotations();
      this.parameterValues = parameterValues;
   }

   @Nonnegative public int getParameterCount() { return parameterTypes.length; }
   @Nonnull public Type getParameterType(@Nonnegative int index) { return parameterTypes[index]; }
   @Nonnull public Class<?> getParameterClass(@Nonnegative int index) { return parameterClasses[index]; }
   @Nonnull public Annotation[] getParameterAnnotations(@Nonnegative int index) { return parameterAnnotations[index]; }
   @Nullable public Object getParameterValue(@Nonnegative int index) { return parameterValues[index]; }

   public void setParameterValue(@Nonnegative int index, @Nullable Object value)
   {
      if (value != null) {
         parameterValues[index] = value;
      }
   }
}
