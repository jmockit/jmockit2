package org.jmockit.internal;

import javax.annotation.*;

/**
 * Thrown to indicate that one or more unexpected invocations occurred during the test.
 */
public final class UnexpectedInvocation extends Error
{
   public UnexpectedInvocation(@Nonnull String detailMessage) { super(detailMessage); }

   @Override
   public String toString() { return getMessage(); }
}
