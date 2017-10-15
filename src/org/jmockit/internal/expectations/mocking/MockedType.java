package org.jmockit.internal.expectations.mocking;

import static java.lang.reflect.Modifier.*;
import org.jmockit.*;
import org.jmockit.internal.expectations.state.*;
import org.jmockit.internal.injection.*;
import org.jmockit.internal.reflection.*;
import org.jmockit.internal.state.*;
import org.jmockit.internal.util.*;

import javax.annotation.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

@SuppressWarnings("EqualsAndHashcode")
public final class MockedType extends InjectionProvider
{
   @Mocked private static final Object DUMMY = null;
   private static final int DUMMY_HASHCODE;

   static
   {
      int h = 0;

      try {
         Field dummy = MockedType.class.getDeclaredField("DUMMY");
         Mocked mocked = dummy.getAnnotation(Mocked.class);
         h = mocked.hashCode();
      }
      catch (NoSuchFieldException ignore) {}

      DUMMY_HASHCODE = h;
   }

   @Nullable public final Field field;
   public final boolean fieldFromTestClass;
   private final int accessModifiers;
   @Nullable private final Mocked mocked;
   @Nullable private final Class<?> parameterImplementationClass;
   public final boolean injectable;
   @Nullable Object providedValue;

   public MockedType(@Nonnull Field field)
   {
      super(field.getGenericType(), field.getName());
      this.field = field;
      fieldFromTestClass = true;
      accessModifiers = field.getModifiers();
      mocked = field.getAnnotation(Mocked.class);
      parameterImplementationClass = null;
      injectable = true;
      registerCascadingAsNeeded();
   }

   private void registerCascadingAsNeeded()
   {
      if (isMockableType()) {
         Type mockedType = declaredType;

         if (!(mockedType instanceof TypeVariable<?>)) {
            ExecutingTest executingTest = TestRun.getExecutingTest();
            CascadingTypes types = executingTest.getCascadingTypes();
            types.add(fieldFromTestClass, mockedType);
         }
      }
   }

   MockedType(
      @Nonnull TestMethod testMethod, @Nonnegative int paramIndex,
      @Nonnull Type parameterType, @Nonnull Annotation[] annotationsOnParameter,
      @Nullable Class<?> parameterImplementationClass)
   {
      super(parameterType, ParameterNames.getName(testMethod, paramIndex));
      field = null;
      fieldFromTestClass = false;
      accessModifiers = 0;
      mocked = getAnnotation(annotationsOnParameter, Mocked.class);
      this.parameterImplementationClass = parameterImplementationClass;
      injectable = true;

      if (providedValue == null && parameterType instanceof Class<?>) {
         Class<?> parameterClass = (Class<?>) parameterType;

         if (parameterClass.isPrimitive()) {
            providedValue = DefaultValues.defaultValueForPrimitiveType(parameterClass);
         }
      }

      registerCascadingAsNeeded();
   }

   @Nullable
   private static <A extends Annotation> A getAnnotation(
      @Nonnull Annotation[] annotations, @Nonnull Class<A> annotation)
   {
      for (Annotation paramAnnotation : annotations) {
         if (paramAnnotation.annotationType() == annotation) {
            //noinspection unchecked
            return (A) paramAnnotation;
         }
      }

      return null;
   }

   MockedType(@Nonnull String cascadingMethodName, @Nonnull Type cascadedType)
   {
      super(cascadedType, cascadingMethodName);
      field = null;
      fieldFromTestClass = false;
      accessModifiers = 0;
      mocked = null;
      injectable = true;
      parameterImplementationClass = null;
   }

   @Nonnull @Override public Class<?> getClassOfDeclaredType() { return getClassType(); }

   /**
    * @return the class object corresponding to the type to be mocked, or {@code TypeVariable.class} in case the
    * mocked type is a type variable (which usually occurs when the mocked implements/extends multiple types)
    */
   @Nonnull
   public Class<?> getClassType()
   {
      if (parameterImplementationClass != null) {
         return parameterImplementationClass;
      }

      Type mockedType = declaredType;

      if (mockedType instanceof Class<?>) {
         return (Class<?>) mockedType;
      }

      if (mockedType instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType) mockedType;
         return (Class<?>) parameterizedType.getRawType();
      }

      // Occurs when declared type is a TypeVariable, usually having two or more bound types.
      // In such cases, there isn't a single class type.
      return TypeVariable.class;
   }

   boolean isMockableType()
   {
      if (mocked == null && !injectable) {
         return false;
      }

      Type mockedType = declaredType;

      if (!(mockedType instanceof Class<?>)) {
         return true;
      }

      Class<?> classType = (Class<?>) mockedType;

      if (isUnmockableJREType(classType)) {
         return false;
      }

      if (injectable) {
         if (isJREValueType(classType) || classType.isEnum()) {
            return false;
         }
      }

      return true;
   }

   private static boolean isUnmockableJREType(@Nonnull Class<?> type)
   {
      return type.isPrimitive() || type.isArray() || type == Integer.class;
   }

   private static boolean isJREValueType(@Nonnull Class<?> type)
   {
      return
         type == String.class || type == Boolean.class || type == Character.class ||
         Number.class.isAssignableFrom(type);
   }

   boolean isFinalFieldOrParameter() { return field == null || isFinal(accessModifiers); }

   @Nullable @Override
   public Object getValue(@Nullable Object owner)
   {
      if (field == null) {
         return providedValue;
      }

      Object value = FieldReflection.getFieldValue(field, owner);

      if (!injectable) {
         return value;
      }

      Class<?> fieldType = field.getType();

      if (value == null) {
         if (providedValue != null) {
            return providedValue;
         }

         if (isFinalFieldOrParameter()) {
            return NULL;
         }

         if (fieldType == String.class) {
            return "";
         }

         return null;
      }

      if (providedValue == null) {
         return value;
      }

      if (!fieldType.isPrimitive()) {
         return value;
      }

      Object defaultValue = DefaultValues.defaultValueForPrimitiveType(fieldType);

      return value.equals(defaultValue) ? providedValue : value;
   }

   @Override
   public int hashCode()
   {
      int result = declaredType.hashCode();

      if (isFinal(accessModifiers)) {
         result *= 31;
      }

      if (injectable) {
         result *= 37;
      }

      if (mocked != null) {
         int h = mocked.hashCode();

         if (h != DUMMY_HASHCODE) {
            result = 31 * result + h;
         }
      }

      return result;
   }
}
