package org.jmockit.internal.expectations;

import org.jmockit.internal.expectations.invocation.*;

import javax.annotation.*;
import java.util.*;

final class UnorderedVerificationPhase extends BaseVerificationPhase
{
   @Nonnull final List<VerifiedExpectation> verifiedExpectations;

   UnorderedVerificationPhase(
      @Nonnull RecordAndReplayExecution recordAndReplay,
      @Nonnull List<Expectation> expectationsInReplayOrder,
      @Nonnull List<Object> invocationInstancesInReplayOrder,
      @Nonnull List<Object[]> invocationArgumentsInReplayOrder)
   {
      super(
         recordAndReplay, expectationsInReplayOrder,
         invocationInstancesInReplayOrder, invocationArgumentsInReplayOrder);
      verifiedExpectations = new ArrayList<>();
   }

   @Nonnull @Override
   protected List<ExpectedInvocation> findNonStrictExpectation(
      @Nullable Object mock, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc, @Nonnull Object[] args)
   {
      if (!matchInstance && recordAndReplay.executionState.isToBeMatchedOnInstance(mock, mockNameAndDesc)) {
         matchInstance = true;
      }

      replayIndex = -1;
      List<ExpectedInvocation> matchingInvocationsWithDifferentArgs = new ArrayList<>();

      for (int i = 0, n = expectationsInReplayOrder.size(); i < n; i++) {
         Expectation replayExpectation = expectationsInReplayOrder.get(i);
         Object replayInstance = invocationInstancesInReplayOrder.get(i);
         Object[] replayArgs = invocationArgumentsInReplayOrder.get(i);

         if (matches(mock, mockClassDesc, mockNameAndDesc, args, replayExpectation, replayInstance, replayArgs)) {
            Expectation verification = expectationBeingVerified();
            replayIndex = i;
            verification.constraints.invocationCount++;
            currentExpectation = replayExpectation;
            mapNewInstanceToReplacementIfApplicable(mock);
         }
         else if (matchingInvocationWithDifferentArgs != null) {
            matchingInvocationsWithDifferentArgs.add(matchingInvocationWithDifferentArgs);
         }
      }

      if (replayIndex >= 0) {
         pendingError = verifyConstraints();
      }

      return matchingInvocationsWithDifferentArgs;
   }

   @Nullable
   private Error verifyConstraints()
   {
      ExpectedInvocation lastInvocation = expectationsInReplayOrder.get(replayIndex).invocation;
      Object[] lastArgs = invocationArgumentsInReplayOrder.get(replayIndex);
      Expectation expectation = expectationBeingVerified();
      return expectation.verifyConstraints(lastInvocation, lastArgs, 1, -1);
   }

   @Override
   void addVerifiedExpectation(@Nonnull VerifiedExpectation verifiedExpectation)
   {
      super.addVerifiedExpectation(verifiedExpectation);
      verifiedExpectations.add(verifiedExpectation);
   }

   @Override
   public void handleInvocationCountConstraint(int minInvocations, int maxInvocations)
   {
      pendingError = null;

      Expectation verifying = expectationBeingVerified();
      Error errorThrown;

      if (replayIndex >= 0) {
         ExpectedInvocation replayInvocation = expectationsInReplayOrder.get(replayIndex).invocation;
         Object[] replayArgs = invocationArgumentsInReplayOrder.get(replayIndex);
         errorThrown = verifying.verifyConstraints(replayInvocation, replayArgs, minInvocations, maxInvocations);
      }
      else {
         errorThrown = verifying.verifyConstraints(minInvocations);
      }

      if (errorThrown != null) {
         throw errorThrown;
      }
   }

   @Nullable
   VerifiedExpectation firstExpectationVerified()
   {
      VerifiedExpectation first = null;

      for (VerifiedExpectation expectation : verifiedExpectations) {
         if (first == null || expectation.replayIndex < first.replayIndex) {
            first = expectation;
         }
      }

      return first;
   }
}
