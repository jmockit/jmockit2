package org.jmockit.internal.injection.field;

import org.jmockit.internal.injection.*;

import javax.annotation.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

final class FieldToInject extends InjectionProvider
{
   @Nonnull private final Field targetField;

   FieldToInject(@Nonnull Field targetField)
   {
      super(targetField.getGenericType(), targetField.getName());
      this.targetField = targetField;
   }

   @Nonnull @Override public Class<?> getClassOfDeclaredType() { return targetField.getType(); }
   @Nonnull @Override public Annotation[] getAnnotations() { return targetField.getDeclaredAnnotations(); }

   @Override
   public String toString() { return "field " + super.toString(); }
}
