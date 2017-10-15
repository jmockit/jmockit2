package org.jmockit.internal.expectations.invocation;

import org.jmockit.external.asm.*;
import org.jmockit.internal.*;
import org.jmockit.internal.expectations.argumentMatching.*;
import org.jmockit.internal.expectations.state.*;
import org.jmockit.internal.reflection.*;
import org.jmockit.internal.state.*;
import org.jmockit.internal.util.*;

import javax.annotation.*;
import java.lang.reflect.*;
import java.util.*;

public final class InvocationArguments
{
   @Nonnull final String classDesc;
   @Nonnull final String methodNameAndDesc;
   @Nullable final String genericSignature;
   @Nonnull private final ArgumentValuesAndMatchers valuesAndMatchers;
   @Nullable private Member realMethodOrConstructor;

   InvocationArguments(
      int access, @Nonnull String classDesc, @Nonnull String methodNameAndDesc, @Nullable String genericSignature,
      @Nonnull Object[] args)
   {
      this.classDesc = classDesc;
      this.methodNameAndDesc = methodNameAndDesc;
      this.genericSignature = genericSignature;
      valuesAndMatchers =
         (access & Opcodes.ACC_VARARGS) == 0 ?
            new ArgumentValuesAndMatchersWithoutVarargs(this, args) :
            new ArgumentValuesAndMatchersWithVarargs(this, args);
   }

   @Nonnull String getClassName() { return classDesc.replace('/', '.'); }

   boolean isForConstructor() { return methodNameAndDesc.charAt(0) == '<'; }

   @Nonnull public Object[] getValues() { return valuesAndMatchers.values; }
   void setValues(@Nonnull Object[] values) { valuesAndMatchers.values = values; }

   public void setValuesWithNoMatchers(@Nonnull Object[] argsToVerify)
   {
      valuesAndMatchers.setValuesWithNoMatchers(argsToVerify);
   }

   public void setValuesAndMatchers(@Nonnull Object[] argsToVerify, @Nullable List<ArgumentMatcher<?>> matchers)
   {
      valuesAndMatchers.setValuesAndMatchers(argsToVerify, matchers);
   }

   @Nullable public List<ArgumentMatcher<?>> getMatchers() { return valuesAndMatchers.matchers; }
   public void setMatchers(@Nullable List<ArgumentMatcher<?>> matchers) { valuesAndMatchers.matchers = matchers; }

   @Nonnull
   public Object[] prepareForVerification(@Nonnull Object[] argsToVerify, @Nullable List<ArgumentMatcher<?>> matchers)
   {
      return valuesAndMatchers.prepareForVerification(argsToVerify, matchers);
   }

   public boolean isMatch(@Nonnull Object[] replayArgs, @Nonnull Map<Object, Object> instanceMap)
   {
      TestRun.enterNoMockingZone();
      ExecutingTest executingTest = TestRun.getExecutingTest();
      boolean previousFlag = executingTest.setShouldIgnoreMockingCallbacks(true);

      try {
         return valuesAndMatchers.isMatch(replayArgs, instanceMap);
      }
      finally {
         executingTest.setShouldIgnoreMockingCallbacks(previousFlag);
         TestRun.exitNoMockingZone();
      }
   }

   @Nullable
   public Error assertMatch(@Nonnull Object[] replayArgs, @Nonnull Map<Object, Object> instanceMap)
   {
      return valuesAndMatchers.assertMatch(replayArgs, instanceMap);
   }

   @Nonnull
   Error argumentMismatchMessage(int paramIndex, @Nullable Object expected, @Nullable Object actual)
   {
      ArgumentMismatch message = new ArgumentMismatch();
      message.append("Parameter ");

      String parameterName = ParameterNames.getName(classDesc, methodNameAndDesc, paramIndex);

      if (parameterName == null) {
         message.append(paramIndex);
      }
      else {
         message.appendFormatted(parameterName);
      }

      message.append(" of ").append(new MethodFormatter(classDesc, methodNameAndDesc).toString());
      message.append(" expected ").appendFormatted(expected);

      if (!message.isFinished()) {
         message.append(", got ").appendFormatted(actual);

         if (actual != null) {
            appendWarningMessageAboutLackOfEqualsMethod(message, actual);
         }
      }

      return new UnexpectedInvocation(message.toString());
   }

   private static void appendWarningMessageAboutLackOfEqualsMethod(
      @Nonnull ArgumentMismatch message, @Nonnull Object value)
   {
      Class<?> argClass = value.getClass();

      if (
         argClass == String.class || argClass == Boolean.class || argClass == Character.class ||
         Number.class.isAssignableFrom(argClass)
      ) {
         return;
      }

      Method equalsMethod;
      try { equalsMethod = argClass.getMethod("equals", Object.class); }
      catch (NoSuchMethodException e) { throw new RuntimeException(e); }

      if (equalsMethod.getDeclaringClass() == Object.class) {
         message.append("\n   Warning: argument class ").append(argClass.getName()).append(" has no \"equals\" method");
      }
   }

   @Override
   public String toString()
   {
      MethodFormatter methodFormatter = new MethodFormatter(classDesc, methodNameAndDesc, false);
      List<String> parameterTypes = methodFormatter.getParameterTypes();
      String arguments = valuesAndMatchers.toString(parameterTypes);
      methodFormatter.append(arguments);
      return methodFormatter.toString();
   }

   public boolean hasEquivalentMatchers(@Nonnull InvocationArguments other)
   {
      return valuesAndMatchers.hasEquivalentMatchers(other.valuesAndMatchers);
   }

   @Nonnull
   Member getRealMethodOrConstructor()
   {
      if (realMethodOrConstructor == null) {
         try { realMethodOrConstructor = new RealMethodOrConstructor(getClassName(), methodNameAndDesc).getMember(); }
         catch (NoSuchMethodException e) { throw new RuntimeException(e); }
      }

      return realMethodOrConstructor;
   }
}
