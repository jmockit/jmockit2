package org.jmockit.internal.expectations.invocation;

import org.jmockit.internal.*;

import javax.annotation.*;
import java.util.*;

public final class InvocationConstraints
{
   public int minInvocations;
   private int maxInvocations;
   public int invocationCount;

   public InvocationConstraints(boolean nonStrictInvocation)
   {
      setLimits(nonStrictInvocation ? 0 : 1, -1);
   }

   public void setLimits(int minInvocations, int maxInvocations)
   {
      this.minInvocations = minInvocations;
      this.maxInvocations = maxInvocations;
   }

   void adjustMaxInvocations(int expectedInvocationCount)
   {
      if (maxInvocations > 0 && maxInvocations < expectedInvocationCount) {
         maxInvocations = expectedInvocationCount;
      }
   }

   void setUnlimitedMaxInvocations() { maxInvocations = -1; }

   public void incrementInvocationCount() { invocationCount++; }

   public boolean isInvocationCountLessThanMinimumExpected() { return invocationCount < minInvocations; }

   public boolean isInvocationCountMoreThanMaximumExpected()
   {
      return maxInvocations >= 0 && invocationCount > maxInvocations;
   }

   @Nullable
   public Error verifyLowerLimit(@Nonnull ExpectedInvocation invocation, int lowerLimit)
   {
      if (invocationCount < lowerLimit) {
         int missingInvocations = lowerLimit - invocationCount;
         return invocation.errorForMissingInvocations(missingInvocations, Collections.<ExpectedInvocation>emptyList());
      }

      return null;
   }

   @Nullable
   public Error verifyUpperLimit(@Nonnull ExpectedInvocation invocation, @Nonnull Object[] replayArgs, int upperLimit)
   {
      if (upperLimit >= 0) {
         int unexpectedInvocations = invocationCount - upperLimit;

         if (unexpectedInvocations > 0) {
            UnexpectedInvocation error = invocation.errorForUnexpectedInvocations(replayArgs, unexpectedInvocations);
            return error;
         }
      }

      return null;
   }

   @Nonnull
   public Error errorForMissingExpectations(
      @Nonnull ExpectedInvocation invocation, @Nonnull List<ExpectedInvocation> nonMatchingInvocations)
   {
      return invocation.errorForMissingInvocations(minInvocations - invocationCount, nonMatchingInvocations);
   }
}
