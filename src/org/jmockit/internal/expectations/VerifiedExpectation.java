package org.jmockit.internal.expectations;

import org.jmockit.internal.expectations.argumentMatching.*;

import javax.annotation.*;
import java.util.*;

final class VerifiedExpectation
{
   @Nonnull final Expectation expectation;
   @Nonnull final Object[] arguments;
   @Nullable final List<ArgumentMatcher<?>> argMatchers;
   final int replayIndex;

   VerifiedExpectation(
      @Nonnull Expectation expectation, @Nonnull Object[] arguments, @Nullable List<ArgumentMatcher<?>> argMatchers,
      int replayIndex)
   {
      this.expectation = expectation;
      this.arguments = arguments;
      this.argMatchers = argMatchers;
      this.replayIndex = replayIndex;
   }

   @Nullable Object captureNewInstance() { return expectation.invocation.instance; }
}
