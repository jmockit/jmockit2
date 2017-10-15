package org.jmockit.internal.injection;

import org.jmockit.internal.injection.constructor.*;
import org.jmockit.internal.injection.full.*;

import javax.annotation.*;
import java.lang.reflect.*;

public final class TestedObjectCreation
{
   @Nonnull private final InjectionState injectionState;
   @Nullable private final FullInjection fullInjection;
   @Nonnull final TestedClass testedClass;

   TestedObjectCreation(
      @Nonnull InjectionState injectionState, @Nullable FullInjection fullInjection,
      @Nonnull Type declaredType, @Nonnull Class<?> declaredClass)
   {
      this.injectionState = injectionState;
      this.fullInjection = fullInjection;
      testedClass = new TestedClass(declaredType, declaredClass);
   }

   public TestedObjectCreation(
      @Nonnull InjectionState injectionState, @Nullable FullInjection fullInjection,
      @Nonnull Class<?> implementationClass)
   {
      this.injectionState = injectionState;
      this.fullInjection = fullInjection;
      testedClass = new TestedClass(implementationClass, implementationClass);
   }

   @Nonnull
   public Object create()
   {
      ConstructorSearch constructorSearch = new ConstructorSearch(injectionState, testedClass, fullInjection != null);
      Constructor<?> constructor = constructorSearch.findConstructorToUse();

      if (constructor == null) {
         String description = constructorSearch.getDescription();
         throw new IllegalArgumentException(
            "No constructor in tested class that can be satisfied by available tested/injectable values" + description);
      }

      ConstructorInjection constructorInjection = new ConstructorInjection(injectionState, fullInjection, constructor);

      return constructorInjection.instantiate(constructorSearch.parameterProviders, testedClass);
   }
}
