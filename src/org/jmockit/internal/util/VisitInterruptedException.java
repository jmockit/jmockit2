package org.jmockit.internal.util;

public final class VisitInterruptedException extends RuntimeException
{
   public static final VisitInterruptedException INSTANCE = new VisitInterruptedException();

   private VisitInterruptedException() {}
}
