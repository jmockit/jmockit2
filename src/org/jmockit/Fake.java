package org.jmockit;

import org.jmockit.internal.faking.*;
import org.jmockit.internal.state.*;

import javax.annotation.*;
import java.lang.reflect.*;

/**
 * Allows a fake implementation to be defined and applied.
 *
 * @param <T> the type of a class to be faked
 */
public class Fake<T>
{
    /**
     * Holds the class or generic type targeted by this fake instance.
     */
    private final Type targetType;

    @Nullable private final Class<?> fakedClass;

    /**
     * Applies the fake methods defined in the concrete subclass to the class or interface specified through the type
     * parameter.
     */
    protected Fake() {
        Fake<?> previousFake = findPreviouslyFakedClassIfFakeAlreadyApplied();

        if (previousFake != null) {
            targetType = previousFake.targetType;
            fakedClass = previousFake.fakedClass;
            return;
        }

        targetType = getTypeToFake();
        Class<T> classToFake = null;

        if (targetType instanceof Class<?>) {
            //noinspection unchecked
            classToFake = (Class<T>) targetType;
        }
        else if (targetType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) targetType;
            //noinspection unchecked
            classToFake = (Class<T>) parameterizedType.getRawType();
        }

        assert classToFake != null;
        FakeClassSetup fakeSetup = new FakeClassSetup(classToFake, targetType, this);
        fakeSetup.redefineMethods();
        fakedClass = classToFake;
    }

    @Nullable
    private Fake<?> findPreviouslyFakedClassIfFakeAlreadyApplied() {
        FakeClasses fakeClasses = TestRun.getFakeClasses();
        return fakeClasses.findPreviouslyAppliedFake(this);
    }

    @Nonnull
    private Type getTypeToFake() {
        Class<?> currentClass = getClass();

        do {
            Type superclass = currentClass.getGenericSuperclass();

            if (superclass instanceof ParameterizedType) {
                return ((ParameterizedType) superclass).getActualTypeArguments()[0];
            }

            if (superclass == Fake.class) {
                throw new IllegalArgumentException("No target type");
            }

            currentClass = (Class<?>) superclass;
        }
        while (true);
    }

    /**
     * An empty method that can be overridden in a fake class that wants to be notified whenever the fake is
     * automatically torn down.
     * Tear down happens when the fake goes out of scope: at the end of the test when applied inside a test, at the end
     * of the test class when applied before the test class, or at the end of the test run when applied through the
     * "<code>fakes</code>" system property.
     * <p/>
     * By default, this method does nothing.
     */
    protected void onTearDown() {}
}
