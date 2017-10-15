package org.jmockit.internal.expectations;

import javax.annotation.*;
import java.util.*;

abstract class Phase
{
   @Nonnull final RecordAndReplayExecution recordAndReplay;

   Phase(@Nonnull RecordAndReplayExecution recordAndReplay) { this.recordAndReplay = recordAndReplay; }

   @Nonnull
   public final Map<Object, Object> getInstanceMap() { return recordAndReplay.executionState.instanceMap; }

   @Nonnull
   final Map<Object, Object> getReplacementMap() { return recordAndReplay.executionState.replacementMap; }

   @Nullable
   abstract Object handleInvocation(
      @Nullable Object mock, int mockAccess, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc,
      @Nullable String genericSignature, boolean withRealImpl, @Nonnull Object[] args)
      throws Throwable;
}
