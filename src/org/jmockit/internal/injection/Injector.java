package org.jmockit.internal.injection;

import org.jmockit.internal.injection.full.*;

import javax.annotation.*;

public class Injector
{
   @Nonnull protected final InjectionState injectionState;
   @Nullable protected final FullInjection fullInjection;

   protected Injector(@Nonnull InjectionState state, @Nullable FullInjection fullInjection)
   {
      injectionState = state;
      this.fullInjection = fullInjection;
   }

   public void fillOutDependenciesRecursively(@Nonnull Object dependency, @Nonnull TestedClass testedClass) {}
}
