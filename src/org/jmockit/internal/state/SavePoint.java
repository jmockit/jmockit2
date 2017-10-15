package org.jmockit.internal.state;

import org.jmockit.internal.*;
import static org.jmockit.internal.expectations.RecordAndReplayExecution.*;
import org.jmockit.internal.faking.*;

import javax.annotation.*;
import java.util.*;

public final class SavePoint
{
   @Nonnull private final Set<ClassIdentification> previousTransformedClasses;
   @Nonnull private final Map<Class<?>, byte[]> previousRedefinedClasses;
   @Nonnull private final List<Class<?>> previousMockedClasses;
   @Nonnull private final FakeClasses.SavePoint previousFakeClasses;

   public SavePoint()
   {
      MockFixture mockFixture = TestRun.mockFixture();
      previousTransformedClasses = mockFixture.getTransformedClasses();
      previousRedefinedClasses = mockFixture.getRedefinedClasses();
      previousMockedClasses = mockFixture.getMockedClasses();
      previousFakeClasses = TestRun.getFakeClasses().new SavePoint();
   }

   public synchronized void rollback()
   {
      RECORD_OR_REPLAY_LOCK.lock();

      try {
         MockFixture mockFixture = TestRun.mockFixture();
         mockFixture.restoreTransformedClasses(previousTransformedClasses);
         mockFixture.restoreRedefinedClasses(previousRedefinedClasses);
         mockFixture.removeMockedClasses(previousMockedClasses);
         previousFakeClasses.rollback();
      }
      finally {
         RECORD_OR_REPLAY_LOCK.unlock();
      }
   }
}
