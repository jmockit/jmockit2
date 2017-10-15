package org.jmockit.internal.expectations.argumentMatching;

import javax.annotation.*;
import java.util.*;

public final class LenientEqualityMatcher extends EqualityMatcher
{
   @Nonnull private final Map<Object, Object> instanceMap;

   public LenientEqualityMatcher(@Nullable Object equalArg, @Nonnull Map<Object, Object> instanceMap)
   {
      super(equalArg);
      this.instanceMap = instanceMap;
   }

   @Override
   public boolean matches(@Nullable Object argValue)
   {
      if (argValue == null) {
         return object == null;
      }
      else if (object == null) {
         return false;
      }
      else if (argValue == object || instanceMap.get(argValue) == object) {
         return true;
      }

      return areEqualWhenNonNull(argValue, object);
   }
}
