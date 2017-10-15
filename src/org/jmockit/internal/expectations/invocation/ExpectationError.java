package org.jmockit.internal.expectations.invocation;

import org.jmockit.internal.util.*;

import javax.annotation.*;

final class ExpectationError extends AssertionError
{
   private String message;

   @Override
   @Nonnull public String toString() { return message; }

   void prepareForDisplay(@Nonnull String title)
   {
      message = title;
      StackTrace.filterStackTrace(this);
   }

   void defineCause(@Nonnull String title, @Nonnull Throwable error)
   {
      prepareForDisplay(title);
      error.initCause(this);
   }
}
