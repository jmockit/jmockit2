package org.jmockit.internal.injection;

import static org.jmockit.internal.util.Utilities.*;

import javax.annotation.*;
import java.lang.reflect.*;
import java.util.*;

final class MultiValuedProvider extends InjectionProvider
{
   @Nonnull private final List<InjectionProvider> individualProviders;

   MultiValuedProvider(@Nonnull Type elementType)
   {
      super(elementType, "");
      individualProviders = new ArrayList<InjectionProvider>();
   }

   void addInjectable(@Nonnull InjectionProvider provider)
   {
      individualProviders.add(provider);
   }

   @Nonnull @Override
   public Class<?> getClassOfDeclaredType() { return getClassType(declaredType); }

   @Nullable @Override
   public Object getValue(@Nullable Object owner)
   {
      List<Object> values = new ArrayList<Object>(individualProviders.size());

      for (InjectionProvider provider : individualProviders) {
         Object value = provider.getValue(owner);
         values.add(value);
      }

      return values;
   }
}
