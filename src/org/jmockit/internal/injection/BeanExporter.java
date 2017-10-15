package org.jmockit.internal.injection;

import org.jmockit.internal.expectations.mocking.*;
import org.jmockit.internal.util.*;

import javax.annotation.*;
import java.util.*;
import java.util.Map.*;

public final class BeanExporter
{
   @Nonnull private final List<MockedType> injectables;
   @Nonnull private final Map<InjectionPoint, Object> testedObjects;
   @Nonnull private final Map<InjectionPoint, Object> instantiatedDependencies;
   @Nonnull private final Map<InjectionPoint, Object> globalDependencies;
   private Object currentTestClassInstance;

   BeanExporter(
      @Nonnull List<MockedType> injectables,
      @Nonnull Map<InjectionPoint, Object> testedObjects,
      @Nonnull Map<InjectionPoint, Object> instantiatedDependencies,
      @Nonnull Map<InjectionPoint, Object> globalDependencies
   ) {
      this.injectables = injectables;
      this.testedObjects = testedObjects;
      this.instantiatedDependencies = instantiatedDependencies;
      this.globalDependencies = globalDependencies;
   }

   void setCurrentTestClassInstance(@Nonnull Object currentTestClassInstance)
   {
      this.currentTestClassInstance = currentTestClassInstance;
   }

   @Nullable
   public Object getBean(@Nonnull String name)
   {
      for (InjectionProvider injectable : injectables) {
         if (name.equals(injectable.getName())) {
            return injectable.getValue(currentTestClassInstance);
         }
      }

      Object bean = findByName(testedObjects, name);

      if (bean == null) {
         bean = findByName(instantiatedDependencies, name);

         if (bean == null) {
            bean = findByName(globalDependencies, name);
         }
      }

      return bean;
   }

   @Nullable
   private static Object findByName(@Nonnull Map<InjectionPoint, Object> dependencies, @Nonnull String name)
   {
      for (Entry<InjectionPoint, Object> injectionPointAndObject : dependencies.entrySet()) {
         InjectionPoint injectionPoint = injectionPointAndObject.getKey();

         if (name.equals(injectionPoint.name)) {
            return injectionPointAndObject.getValue();
         }
      }

      return null;
   }

   @Nullable
   public <T> T getBean(@Nonnull Class<T> type)
   {
      for (MockedType injectable : injectables) {
         if (type == injectable.getClassType()) {
            @SuppressWarnings("unchecked") T injectableValue = (T) injectable.getValue(currentTestClassInstance);
            return injectableValue;
         }
      }

      Object bean = findByType(testedObjects, type);

      if (bean == null) {
         bean = findByType(instantiatedDependencies, type);

         if (bean == null) {
            bean = findByType(globalDependencies, type);
         }
      }

      //noinspection unchecked
      return (T) bean;
   }

   @Nullable
   private static Object findByType(@Nonnull Map<InjectionPoint, Object> dependencies, @Nonnull Class<?> type)
   {
      for (Entry<InjectionPoint, Object> injectionPointAndObject : dependencies.entrySet()) {
         InjectionPoint injectionPoint = injectionPointAndObject.getKey();
         Class<?> injectedType = Utilities.getClassType(injectionPoint.type);

         if (type.isAssignableFrom(injectedType)) {
            return injectionPointAndObject.getValue();
         }
      }

      return null;
   }
}
