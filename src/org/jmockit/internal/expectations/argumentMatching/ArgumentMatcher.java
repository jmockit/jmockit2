package org.jmockit.internal.expectations.argumentMatching;

import javax.annotation.*;

/**
 * An argument matcher for the recording/verification of expectations.
 */
public interface ArgumentMatcher<M extends ArgumentMatcher<M>>
{
   /**
    * Indicates whether this matcher instance is functionally the same as another one of the same type.
    */
   boolean same(@Nonnull M other);

   /**
    * Evaluates the matcher for the given argument.
    */
   boolean matches(@Nullable Object argValue);

   /**
    * Writes a phrase to be part of an error message describing an argument mismatch.
    */
   void writeMismatchPhrase(@Nonnull ArgumentMismatch argumentMismatch);
}
