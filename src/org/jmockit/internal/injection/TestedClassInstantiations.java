package org.jmockit.internal.injection;

import org.jmockit.*;
import static org.jmockit.external.asm.Opcodes.*;
import org.jmockit.internal.expectations.mocking.*;
import static org.jmockit.internal.injection.TestedObject.*;

import javax.annotation.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

public final class TestedClassInstantiations
{
   private static final int FIELD_ACCESS_MASK = ACC_SYNTHETIC + ACC_STATIC;
   private static final int METHOD_ACCESS_MASK = ACC_BRIDGE + ACC_VARARGS + ACC_NATIVE + ACC_ABSTRACT + ACC_SYNTHETIC;

   @Nonnull private final List<TestedField> testedFields;
   @Nonnull private final List<MockedType> injectableFields;
   @Nonnull final InjectionState injectionState;

   public TestedClassInstantiations()
   {
      testedFields = new LinkedList<>();
      injectableFields = new ArrayList<>();
      injectionState = new InjectionState();
   }

   public boolean findTestedAndInjectableMembers(@Nonnull Class<?> testClass)
   {
      findAllTestedAndInjectableMembersInTestClassHierarchy(testClass);
      return !testedFields.isEmpty() || injectionState.interfaceResolution.canResolveInterfaces();
   }

   private void findAllTestedAndInjectableMembersInTestClassHierarchy(@Nonnull Class<?> testClass)
   {
      Class<?> superclass = testClass.getSuperclass();

      if (superclass.getClassLoader() != null) {
         findAllTestedAndInjectableMembersInTestClassHierarchy(superclass);
      }

      examineInstanceFields(testClass);
      examineMethods(testClass);
   }

   private void examineInstanceFields(@Nonnull Class<?> testClass)
   {
      for (Field candidateField : testClass.getDeclaredFields()) {
         if ((candidateField.getModifiers() & FIELD_ACCESS_MASK) == 0) {
            addAsTestedOrInjectableFieldIfApplicable(candidateField);
         }
      }
   }

   private void examineMethods(@Nonnull Class<?> testClass)
   {
      for (Method candidateMethod : testClass.getDeclaredMethods()) {
         if ((candidateMethod.getModifiers() & METHOD_ACCESS_MASK) == 0) {
            addAsTestedMethodIfApplicable(candidateMethod);
         }
      }
   }

   private void addAsTestedOrInjectableFieldIfApplicable(@Nonnull Field fieldFromTestClass)
   {
      for (Annotation fieldAnnotation : fieldFromTestClass.getDeclaredAnnotations()) {
         if (fieldAnnotation instanceof Mocked) {
            MockedType mockedType = new MockedType(fieldFromTestClass);
            injectableFields.add(mockedType);
            break;
         }

         Tested testedMetadata = getTestedAnnotationIfPresent(fieldAnnotation);

         if (testedMetadata != null) {
            TestedField testedField = new TestedField(injectionState, fieldFromTestClass, testedMetadata);
            testedFields.add(testedField);
            break;
         }
      }
   }

   private void addAsTestedMethodIfApplicable(@Nonnull Method methodFromTestClass)
   {
      for (Annotation methodAnnotation : methodFromTestClass.getDeclaredAnnotations()) {
         Tested testedMetadata = getTestedAnnotationIfPresent(methodAnnotation);

         if (testedMetadata != null) {
            addTestedMethodIfApplicable(methodFromTestClass);
            break;
         }
      }
   }

   private void addTestedMethodIfApplicable(@Nonnull Method methodFromTestClass)
   {
      Class<?> returnType = methodFromTestClass.getReturnType();

      if (returnType == Class.class) {
         Type[] parameterTypes = methodFromTestClass.getGenericParameterTypes();

         if (parameterTypes.length == 1) {
            Type parameterType = parameterTypes[0];

            if (parameterType instanceof ParameterizedType) {
               ParameterizedType interfaceType = (ParameterizedType) parameterType;

               if (interfaceType.getRawType() == Class.class) {
                  injectionState.interfaceResolution.addInterfaceResolutionMethod(interfaceType, methodFromTestClass);
               }
            }
         }
      }
   }

   public void assignNewInstancesToTestedFields(@Nonnull Object testClassInstance, boolean beforeSetup)
   {
      injectionState.buildListsOfInjectables(testClassInstance, injectableFields);

      for (TestedObject testedField : testedFields) {
         if (!beforeSetup || testedField.isAvailableDuringSetup()) {
            instantiateTestedObject(testClassInstance, testedField);
         }
      }
   }

   private void instantiateTestedObject(@Nonnull Object testClassInstance, @Nonnull TestedObject testedObject)
   {
      try {
         testedObject.instantiateWithInjectableValues(testClassInstance);
      }
      finally {
         injectionState.resetConsumedInjectionProviders();
      }
   }

   public void clearTestedObjects()
   {
      injectionState.lifecycleMethods.executeTerminationMethodsIfAny();
      injectionState.clearTestedObjectsAndInstantiatedDependencies();
      resetTestedFields(false);
   }

   private void resetTestedFields(boolean duringTearDown)
   {
      Object testClassInstance = injectionState.getCurrentTestClassInstance();

      for (TestedObject testedField : testedFields) {
         testedField.clearIfAutomaticCreation(testClassInstance, duringTearDown);
      }
   }

   public void clearTestedObjectsCreatedDuringSetup()
   {
      resetTestedFields(true);
   }

   @Nonnull
   public BeanExporter getBeanExporter() { return injectionState.getBeanExporter(); }
}
