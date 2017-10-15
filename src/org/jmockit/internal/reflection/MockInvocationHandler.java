package org.jmockit.internal.reflection;

import org.jmockit.internal.util.*;

import javax.annotation.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

/**
 * Handles invocations to all kinds of mock implementations created for interfaces and annotation types through any of
 * the mocking APIs.
 * <p/>
 * The {@code java.lang.Object} methods {@code equals}, {@code hashCode}, and {@code toString} are handled in a
 * meaningful way, returning a value that makes sense for the proxy instance.
 * The special {@linkplain Annotation} contracts for these three methods is <em>not</em> observed, though, since it
 * would require making dynamic calls to the mocked annotation attributes.
 * <p/>
 * Any other method invocation is handled by simply returning the default value according to the method's return type
 * (as defined in {@linkplain DefaultValues}).
 */
public final class MockInvocationHandler implements InvocationHandler
{
   public static final InvocationHandler INSTANCE = new MockInvocationHandler();
   private static final Class<?>[] CONSTRUCTOR_PARAMETERS_FOR_PROXY_CLASS = {InvocationHandler.class};

   @Nonnull
   public static Object newMockedInstance(@Nonnull Class<?> proxyClass)
   {
      Constructor<?> publicConstructor;
      try { publicConstructor = proxyClass.getConstructor(CONSTRUCTOR_PARAMETERS_FOR_PROXY_CLASS); }
      catch (NoSuchMethodException e) { throw new RuntimeException(e); }

      return ConstructorReflection.invoke(publicConstructor, INSTANCE);
   }

   @Nullable @Override
   public Object invoke(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args)
   {
      Class<?> declaringClass = method.getDeclaringClass();
      String methodName = method.getName();

      if (declaringClass == Object.class) {
         if ("equals".equals(methodName)) {
            return proxy == args[0];
         }
         else if ("hashCode".equals(methodName)) {
            return System.identityHashCode(proxy);
         }
         else { // "toString"
            return ObjectMethods.objectIdentity(proxy);
         }
      }

      if (declaringClass == Annotation.class) {
         return proxy.getClass().getInterfaces()[0];
      }

      Class<?> retType = method.getReturnType();
      return DefaultValues.computeForType(retType);
   }
}
