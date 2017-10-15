package org.jmockit.internal.faking;

import org.jmockit.internal.*;
import org.jmockit.internal.state.*;

import javax.annotation.*;
import java.lang.reflect.*;

public final class FakeBridge extends ClassLoadingBridge
{
   @Nonnull public static final ClassLoadingBridge MB = new FakeBridge();

   private FakeBridge() { super("$FB"); }

   @Nonnull @Override
   public Object invoke(@Nullable Object faked, Method method, @Nonnull Object[] args) throws Throwable
   {
      if (TestRun.isInsideNoMockingZone()) {
         return false;
      }

      TestRun.enterNoMockingZone();

      try {
         String fakeClassDesc = (String) args[0];

         if (notToBeMocked(faked, fakeClassDesc)) {
            return false;
         }

         Integer fakeStateIndex = (Integer) args[1];
         return TestRun.updateFakeState(fakeClassDesc, fakeStateIndex);
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }
}
