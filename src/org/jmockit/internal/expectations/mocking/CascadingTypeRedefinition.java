package org.jmockit.internal.expectations.mocking;

import javax.annotation.*;
import java.lang.reflect.*;

public final class CascadingTypeRedefinition extends BaseTypeRedefinition
{
   @Nonnull private final Type mockedType;

   public CascadingTypeRedefinition(@Nonnull String cascadingMethodName, @Nonnull Type mockedType)
   {
      super(new MockedType(cascadingMethodName, mockedType));
      this.mockedType = mockedType;
   }

   @Nullable
   public InstanceFactory redefineType()
   {
      return redefineType(mockedType);
   }
}
