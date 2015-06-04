/*
 * Copyright (c) 2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jmockit;

import java.util.function.*;

/**
 * API for the recording & verification of expectations.
 */
public final class Expectation
{
   @FunctionalInterface
   public interface RecordingBlock { void perform(); }

   @FunctionalInterface
   public interface VerificationBlock { void perform(); }

   public interface Delegate {}

   @FunctionalInterface
   public interface Execution { <T> T proceed(Object... replacementArgs); }

   private Expectation() {}

   public static Recording record(RecordingBlock expectations) { expectations.perform(); return new Recording(); }

   public static Verification verify(VerificationBlock verifications) { verifications.perform(); return new Verification(); }
   public static void verifyInOrder(VerificationBlock verifications) { verifications.perform(); }
   public static void verifyAll(VerificationBlock verifications) { verifications.perform(); }
   public static void verifyAll(Object mockedClassOrInstance, VerificationBlock verifications) { verifications.perform(); }

   public static <T> T any() { return null; }
   public static <T> T notNull() { return null; }
   public static <T> T same(T instance) { return null; }
   public static <T> T as(Predicate<? super T> predicate) { return null; }

   public static final class Recording
   {
      public Object result;
      public Delegate delegate;
   }

   public static final class Verification
   {
      public int times;
      public int minTimes;
      public int maxTimes;
   }
}
