package org.jmockit.internal.expectations;

import org.jmockit.internal.expectations.invocation.*;
import static org.jmockit.internal.reflection.ConstructorReflection.*;
import static org.jmockit.internal.reflection.MethodReflection.*;
import org.jmockit.internal.util.*;

import javax.annotation.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.util.*;

final class ReturnTypeConversion
{
   private static final Class<?>[] STRING = {String.class};

   @Nonnull private final Expectation expectation;
   @Nonnull private final Class<?> returnType;
   @Nonnull private final Object valueToReturn;

   ReturnTypeConversion(@Nonnull Expectation expectation, @Nonnull Class<?> returnType, @Nonnull Object value)
   {
      this.expectation = expectation;
      this.returnType = returnType;
      valueToReturn = value;
   }

   private boolean hasReturnOfDifferentType()
   {
      return
         !returnType.isArray() &&
         !Iterable.class.isAssignableFrom(returnType) && !Iterator.class.isAssignableFrom(returnType) &&
         !returnType.isAssignableFrom(valueToReturn.getClass());
   }

   void addConvertedValue()
   {
      Class<?> wrapperType =
         AutoBoxing.isWrapperOfPrimitiveType(returnType) ? returnType : AutoBoxing.getWrapperType(returnType);
      Class<?> valueType = valueToReturn.getClass();

      if (valueType == wrapperType) {
         addReturnValue(valueToReturn);
      }
      else if (wrapperType != null && AutoBoxing.isWrapperOfPrimitiveType(valueType)) {
         addPrimitiveValueConvertingAsNeeded(wrapperType);
      }
      else {
         boolean valueIsArray = valueType.isArray();

         if (valueIsArray || valueToReturn instanceof Iterable<?> || valueToReturn instanceof Iterator<?>) {
            addMultiValuedResultBasedOnTheReturnType(valueIsArray);
         }
         else if (wrapperType != null) {
            throw newIncompatibleTypesException();
         }
         else {
            addResultFromSingleValue();
         }
      }
   }

   private void addReturnValue(@Nonnull Object returnValue)
   {
      expectation.getResults().addReturnValueResult(returnValue);
   }

   private void addMultiValuedResultBasedOnTheReturnType(boolean valueIsArray)
   {
      if (returnType == void.class) {
         addMultiValuedResult(valueIsArray);
      }
      else if (returnType == Object.class) {
         addReturnValue(valueToReturn);
      }
      else if (valueIsArray && addCollectionOrMapWithElementsFromArray()) {
         return;
      }
      else if (hasReturnOfDifferentType()) {
         addMultiValuedResult(valueIsArray);
      }
      else {
         addReturnValue(valueToReturn);
      }
   }

   private void addMultiValuedResult(boolean valueIsArray)
   {
      InvocationResults results = expectation.getResults();

      if (valueIsArray) {
         results.addResults(valueToReturn);
      }
      else if (valueToReturn instanceof Iterable<?>) {
         results.addResults((Iterable<?>) valueToReturn);
      }
      else {
         results.addDeferredResults((Iterator<?>) valueToReturn);
      }
   }

   private boolean addCollectionOrMapWithElementsFromArray()
   {
      int n = Array.getLength(valueToReturn);
      Object values = null;

      if (returnType.isAssignableFrom(ListIterator.class)) {
         List<Object> list = new ArrayList<>(n);
         addArrayElements(list, n);
         values = list.listIterator();
      }
      else if (returnType.isAssignableFrom(List.class)) {
         values = addArrayElements(new ArrayList<>(n), n);
      }
      else if (returnType.isAssignableFrom(Set.class)) {
         values = addArrayElements(new LinkedHashSet<>(n), n);
      }
      else if (returnType.isAssignableFrom(SortedSet.class)) {
         values = addArrayElements(new TreeSet<>(), n);
      }
      else if (returnType.isAssignableFrom(Map.class)) {
         values = addArrayElements(new LinkedHashMap<>(n), n);
      }
      else if (returnType.isAssignableFrom(SortedMap.class)) {
         values = addArrayElements(new TreeMap<>(), n);
      }

      if (values != null) {
         expectation.getResults().addReturnValue(values);
         return true;
      }

      return false;
   }

   @Nonnull
   private Object addArrayElements(@Nonnull Collection<Object> values, int elementCount)
   {
      for (int i = 0; i < elementCount; i++) {
         Object element = Array.get(valueToReturn, i);
         values.add(element);
      }

      return values;
   }

   @Nullable
   private Object addArrayElements(@Nonnull Map<Object, Object> values, int elementPairCount)
   {
      for (int i = 0; i < elementPairCount; i++) {
         Object keyAndValue = Array.get(valueToReturn, i);

         if (keyAndValue == null || !keyAndValue.getClass().isArray()) {
            return null;
         }

         Object key = Array.get(keyAndValue, 0);
         Object element = Array.getLength(keyAndValue) > 1 ? Array.get(keyAndValue, 1) : null;
         values.put(key, element);
      }

      return values;
   }

   private void addResultFromSingleValue()
   {
      if (returnType == Object.class) {
         addReturnValue(valueToReturn);
      }
      else if (returnType == void.class) {
         throw newIncompatibleTypesException();
      }
      else if (returnType.isArray()) {
         addArray();
      }
      else if (returnType.isAssignableFrom(ArrayList.class)) {
         addCollectionWithSingleElement(new ArrayList<>(1));
      }
      else if (returnType.isAssignableFrom(LinkedList.class)) {
         addCollectionWithSingleElement(new LinkedList<>());
      }
      else if (returnType.isAssignableFrom(HashSet.class)) {
         addCollectionWithSingleElement(new HashSet<>(1));
      }
      else if (returnType.isAssignableFrom(TreeSet.class)) {
         addCollectionWithSingleElement(new TreeSet<>());
      }
      else if (returnType.isAssignableFrom(ListIterator.class)) {
         addListIterator();
      }
      else if (valueToReturn instanceof CharSequence) {
         addCharSequence((CharSequence) valueToReturn);
      }
      else {
         addPrimitiveValue();
      }
   }

   @Nonnull
   private IllegalArgumentException newIncompatibleTypesException()
   {
      ExpectedInvocation invocation = expectation.invocation;
      String valueTypeName = valueToReturn.getClass().getName().replace("java.lang.", "");
      String returnTypeName = returnType.getName().replace("java.lang.", "");

      MethodFormatter methodDesc =
         new MethodFormatter(invocation.getClassDesc(), invocation.getMethodNameAndDescription());
      String msg =
         "Value of type " + valueTypeName + " incompatible with return type " + returnTypeName + " of " + methodDesc;

      return new IllegalArgumentException(msg);
   }

   private void addArray()
   {
      Object array = Array.newInstance(returnType.getComponentType(), 1);
      Array.set(array, 0, valueToReturn);
      addReturnValue(array);
   }

   private void addCollectionWithSingleElement(@Nonnull Collection<Object> container)
   {
      container.add(valueToReturn);
      addReturnValue(container);
   }

   private void addListIterator()
   {
      List<Object> l = new ArrayList<>(1);
      l.add(valueToReturn);
      ListIterator<Object> iterator = l.listIterator();
      addReturnValue(iterator);
   }

   private void addCharSequence(@Nonnull CharSequence textualValue)
   {
      @Nonnull Object convertedValue = textualValue;

      if (returnType.isAssignableFrom(ByteArrayInputStream.class)) {
         convertedValue = new ByteArrayInputStream(textualValue.toString().getBytes());
      }
      else if (returnType.isAssignableFrom(StringReader.class)) {
         convertedValue = new StringReader(textualValue.toString());
      }
      else if (!(textualValue instanceof StringBuilder) && returnType.isAssignableFrom(StringBuilder.class)) {
         convertedValue = new StringBuilder(textualValue);
      }
      else if (!(textualValue instanceof CharBuffer) && returnType.isAssignableFrom(CharBuffer.class)) {
         convertedValue = CharBuffer.wrap(textualValue);
      }
      else {
         Object valueFromText = newInstanceUsingPublicConstructorIfAvailable(returnType, STRING, textualValue);

         if (valueFromText != null) {
            convertedValue = valueFromText;
         }
      }

      addReturnValue(convertedValue);
   }

   private void addPrimitiveValue()
   {
      Class<?> primitiveType = AutoBoxing.getPrimitiveType(valueToReturn.getClass());

      if (primitiveType != null) {
         Class<?>[] parameterType = {primitiveType};
         Object convertedValue =
            newInstanceUsingPublicConstructorIfAvailable(returnType, parameterType, valueToReturn);

         if (convertedValue == null) {
            convertedValue = invokePublicIfAvailable(returnType, null, "valueOf", parameterType, valueToReturn);
         }

         if (convertedValue != null) {
            addReturnValue(convertedValue);
            return;
         }
      }

      throw newIncompatibleTypesException();
   }

   private void addPrimitiveValueConvertingAsNeeded(@Nonnull Class<?> targetType)
   {
      Object convertedValue = null;

      if (valueToReturn instanceof Number) {
         convertedValue = convertFromNumber(targetType, (Number) valueToReturn);
      }
      else if (valueToReturn instanceof Character) {
         convertedValue = convertFromChar(targetType, (Character) valueToReturn);
      }

      if (convertedValue == null) {
         throw newIncompatibleTypesException();
      }

      addReturnValue(convertedValue);
   }

   @Nullable
   private static Object convertFromNumber(@Nonnull Class<?> targetType, @Nonnull Number number)
   {
      if (targetType == Integer.class) {
         return number.intValue();
      }

      if (targetType == Short.class) {
         return number.shortValue();
      }

      if (targetType == Long.class) {
         return number.longValue();
      }

      if (targetType == Byte.class) {
         return number.byteValue();
      }

      if (targetType == Double.class) {
         return number.doubleValue();
      }

      if (targetType == Float.class) {
         return number.floatValue();
      }

      if (targetType == Character.class) {
         return (char) number.intValue();
      }

      return null;
   }

   @Nullable
   private static Object convertFromChar(@Nonnull Class<?> targetType, char c)
   {
      if (targetType == Integer.class) {
         return (int) c;
      }

      if (targetType == Short.class) {
         return (short) c;
      }

      if (targetType == Long.class) {
         return (long) c;
      }

      if (targetType == Byte.class) {
         //noinspection NumericCastThatLosesPrecision
         return (byte) c;
      }

      if (targetType == Double.class) {
         return (double) c;
      }

      if (targetType == Float.class) {
         return (float) c;
      }

      return null;
   }
}
