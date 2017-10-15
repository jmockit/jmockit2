package org.jmockit.internal.expectations.invocation;

import org.jmockit.internal.expectations.invocation.InvocationResult.*;

import javax.annotation.*;
import java.lang.reflect.*;
import java.util.*;

public final class InvocationResults
{
   @Nonnull private final ExpectedInvocation invocation;
   @Nonnull private final InvocationConstraints constraints;
   @Nullable private InvocationResult currentResult;
   private InvocationResult lastResult;
   private int resultCount;

   public InvocationResults(@Nonnull ExpectedInvocation invocation, @Nonnull InvocationConstraints constraints)
   {
      this.invocation = invocation;
      this.constraints = constraints;
   }

   public void addReturnValue(@Nullable Object value)
   {
      addNewReturnValueResult(value);
   }

   private void addNewReturnValueResult(@Nullable Object value)
   {
      InvocationResult result = new ReturnValueResult(value);
      addResult(result);
   }

   public void addReturnValueResult(@Nullable Object value) { addNewReturnValueResult(value); }

   public void addReturnValues(@Nonnull Object... values)
   {
      for (Object value : values) {
         addReturnValue(value);
      }
   }

   public void addResults(@Nonnull Object array)
   {
      int n = Array.getLength(array);

      for (int i = 0; i < n; i++) {
         Object value = Array.get(array, i);
         addConsecutiveResult(value);
      }
   }

   private void addConsecutiveResult(@Nullable Object result)
   {
      if (result instanceof Throwable) {
         addThrowable((Throwable) result);
      }
      else {
         addReturnValue(result);
      }
   }

   public void addResults(@Nonnull Iterable<?> values)
   {
      for (Object value : values) {
         addConsecutiveResult(value);
      }
   }

   public void addDeferredResults(@Nonnull Iterator<?> values)
   {
      InvocationResult result = new DeferredResults(values);
      addResult(result);
      constraints.setUnlimitedMaxInvocations();
   }

   @Nullable
   public Object executeRealImplementation(@Nonnull Object instanceToInvoke, @Nonnull Object[] invocationArgs)
      throws Throwable
   {
      return currentResult.produceResult(invocationArgs);
   }

   public void addThrowable(@Nonnull Throwable t) { addResult(new ThrowableResult(t)); }

   private void addResult(@Nonnull InvocationResult result)
   {
      resultCount++;
      constraints.adjustMaxInvocations(resultCount);

      if (currentResult == null) {
         currentResult = result;
         lastResult = result;
      }
      else {
         lastResult.next = result;
         lastResult = result;
      }
   }

   @Nullable
   public Object produceResult(@Nullable Object invokedObject, @Nonnull Object[] invocationArgs) throws Throwable
   {
      InvocationResult resultToBeProduced = currentResult;

      if (resultToBeProduced == null) {
         return null;
      }

      InvocationResult nextResult = resultToBeProduced.next;

      if (nextResult != null) {
         currentResult = nextResult;
      }

      Object result = resultToBeProduced.produceResult(invokedObject, invocation, constraints, invocationArgs);

      return result;
   }
}
