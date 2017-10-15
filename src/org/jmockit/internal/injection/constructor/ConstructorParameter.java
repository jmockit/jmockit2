package org.jmockit.internal.injection.constructor;

import org.jmockit.internal.injection.*;
import static org.jmockit.internal.util.Utilities.*;

import javax.annotation.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

final class ConstructorParameter extends InjectionProvider
{
   @Nonnull private final Class<?> classOfDeclaredType;
   @Nonnull private final Annotation[] annotations;
   @Nullable private final Object value;

   ConstructorParameter(
      @Nonnull Type declaredType, @Nonnull Annotation[] annotations, @Nonnull String name, @Nullable Object value)
   {
      super(declaredType, name);
      classOfDeclaredType = getClassType(declaredType);
      this.annotations = annotations;
      this.value = value;
   }

   @Nonnull @Override public Class<?> getClassOfDeclaredType() { return classOfDeclaredType; }
   @Nonnull @Override public Annotation[] getAnnotations() { return annotations; }
   @Nullable @Override public Object getValue(@Nullable Object owner) { return value; }

   @Override
   public String toString() { return "parameter " + super.toString(); }
}
