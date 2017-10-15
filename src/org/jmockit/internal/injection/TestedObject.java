package org.jmockit.internal.injection;

import org.jmockit.*;
import org.jmockit.internal.injection.field.*;
import org.jmockit.internal.injection.full.*;
import static org.jmockit.internal.util.AutoBoxing.*;
import static org.jmockit.internal.util.DefaultValues.*;

import javax.annotation.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

abstract class TestedObject
{
   @Nonnull private final InjectionState injectionState;
   @Nonnull private final String testedName;
   @Nonnull final Tested metadata;
   @Nonnull private final FullInjection fullInjection;
   @Nonnull private final TestedClass testedClass;
   @Nullable private final TestedObjectCreation testedObjectCreation;
   @Nullable private List<Field> targetFields;
   boolean createAutomatically;

   @Nullable
   static Tested getTestedAnnotationIfPresent(@Nonnull Annotation annotation)
   {
      if (annotation instanceof Tested) {
         return (Tested) annotation;
      }

      return annotation.annotationType().getAnnotation(Tested.class);
   }

   TestedObject(
      @Nonnull InjectionState injectionState, @Nonnull Tested metadata,
      @Nonnull String testedName, @Nonnull Type testedType, @Nonnull Class<?> testedClass)
   {
      this.injectionState = injectionState;
      this.testedName = testedName;
      this.metadata = metadata;
      fullInjection = new FullInjection(injectionState, testedClass, testedName);

      if (testedClass.isInterface() || testedClass.isEnum() || testedClass.isPrimitive() || testedClass.isArray()) {
         testedObjectCreation = null;
         this.testedClass = new TestedClass(testedType, testedClass);
      }
      else {
         testedObjectCreation = new TestedObjectCreation(injectionState, fullInjection, testedType, testedClass);
         this.testedClass = testedObjectCreation.testedClass;
         injectionState.lifecycleMethods.findLifecycleMethods(testedClass);
      }
   }

   boolean isAvailableDuringSetup() { return true; }

   void instantiateWithInjectableValues(@Nonnull Object testClassInstance)
   {
      if (alreadyInstantiated(testClassInstance)) {
         return;
      }

      Object testedObject = getExistingTestedInstanceIfApplicable(testClassInstance);
      Class<?> testedObjectClass = testedClass.targetClass;

      if (isNonInstantiableType(testedObjectClass, testedObject)) {
         reusePreviouslyCreatedInstance(testClassInstance);
         return;
      }

      if (testedObject == null && createAutomatically) {
         if (reusePreviouslyCreatedInstance(testClassInstance)) {
            return;
         }

         testedObject = createAndRegisterNewObject(testClassInstance);
      }
      else if (testedObject != null) {
         registerTestedObject(testedObject);
         testedObjectClass = testedObject.getClass();
      }

      if (testedObject != null && testedObjectClass.getClassLoader() != null) {
         performFieldInjection(testedObjectClass, testedObject);
         executeInitializationMethodsIfAny(testedObjectClass, testedObject);
      }
   }

   boolean alreadyInstantiated(@Nonnull Object testClassInstance) { return false; }

   @Nullable
   abstract Object getExistingTestedInstanceIfApplicable(@Nonnull Object testClassInstance);

   static boolean isNonInstantiableType(@Nonnull Class<?> targetClass, @Nullable Object currentValue)
   {
      return
         targetClass.isPrimitive() && defaultValueForPrimitiveType(targetClass).equals(currentValue) ||
         currentValue == null && (
            targetClass.isArray() || targetClass.isEnum() || targetClass.isAnnotation() ||
            isWrapperOfPrimitiveType(targetClass)
         );
   }

   private boolean reusePreviouslyCreatedInstance(@Nonnull Object testClassInstance)
   {
      Object previousInstance = injectionState.getTestedInstance(testedClass.declaredType, testedName);

      if (previousInstance != null) {
         setInstance(testClassInstance, previousInstance);
         return true;
      }

      return false;
   }

   void setInstance(@Nonnull Object testClassInstance, @Nullable Object testedInstance) {}

   @Nullable
   private Object createAndRegisterNewObject(@Nonnull Object testClassInstance)
   {
      Object testedInstance = null;

      if (testedObjectCreation != null) {
         testedInstance = testedObjectCreation.create();
         setInstance(testClassInstance, testedInstance);
         registerTestedObject(testedInstance);
      }

      return testedInstance;
   }

   private void registerTestedObject(@Nonnull Object testedObject)
   {
      InjectionPoint injectionPoint = new InjectionPoint(testedClass.declaredType, testedName);
      injectionState.saveTestedObject(injectionPoint, testedObject);
   }

   private void performFieldInjection(@Nonnull Class<?> targetClass, @Nonnull Object testedObject)
   {
      FieldInjection fieldInjection = new FieldInjection(injectionState, fullInjection);

      if (targetFields == null) {
         targetFields = fieldInjection.findAllTargetInstanceFieldsInTestedClassHierarchy(targetClass, testedClass);
      }

      fieldInjection.injectIntoEligibleFields(targetFields, testedObject, testedClass);
   }

   private void executeInitializationMethodsIfAny(@Nonnull Class<?> testedClass, @Nonnull Object testedObject)
   {
      if (createAutomatically) {
         injectionState.lifecycleMethods.executeInitializationMethodsIfAny(testedClass, testedObject);
      }
   }

   void clearIfAutomaticCreation(@Nonnull Object testClassInstance, boolean duringTearDown)
   {
      if (createAutomatically && (duringTearDown || !isAvailableDuringSetup())) {
         setInstance(testClassInstance, null);
      }
   }
}
