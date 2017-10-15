package org.jmockit;

import org.jmockit.internal.*;

import javax.annotation.*;
import java.lang.reflect.*;

/**
 *
 */
public class Invocation
{
    @Nullable private final Object invokedInstance;
    private final Object[] invokedArguments;
    private final int invocationCount;

    /**
     * For internal use only.
     */
    protected Invocation(@Nullable Object invokedInstance, @Nonnull Object[] invokedArguments, int invocationCount)
    {
        this.invokedInstance = invokedInstance;
        this.invokedArguments = invokedArguments;
        this.invocationCount = invocationCount;
    }

    /**
     * Returns the instance on which the current invocation was made, or {@code null} for a {@code static} method
     * invocation.
     */
    public final <T> T getInvokedInstance()
    {
        //noinspection unchecked,ConstantConditions
        return (T) invokedInstance;
    }

    /**
     * Returns the {@code Method} or {@code Constructor} object corresponding to the target method or constructor,
     * respectively.
     */
    public final <M extends Member> M getInvokedMember()
    {
        //noinspection unchecked,ClassReferencesSubclass
        return (M) ((BaseInvocation) this).getRealMember();
    }

    /**
     * Returns the actual argument values passed in the invocation to the target method/constructor.
     */
    public final Object[] getInvokedArguments() { return invokedArguments; }

    /**
     * Returns the current invocation count. The first invocation starts at 1 (one).
     */
    public final int getInvocationCount() { return invocationCount; }

    /**
     * Returns the index for the current invocation. The first invocation starts at 0 (zero).
     * Note that this is equivalent to {@link #getInvocationCount()} - 1.
     */
    public final int getInvocationIndex() { return invocationCount - 1; }

    /**
     * Allows execution to proceed into the real implementation of the target method/constructor.
     * <p/>
     * In the case of a method, the real implementation is executed with the argument values originally received or
     * explicitly given as replacement.
     * Whatever comes out (either a return value or a thrown exception/error) becomes the result for this execution of
     * the method.
     * <p/>
     * In the case of a constructor, the real constructor implementation code which comes after the necessary call to
     * "<code>super</code>" is executed, using the original argument values; replacement arguments are not supported.
     * If the execution of said code throws an exception or error, it is propagated out to the caller of the target
     * constructor.
     * Contrary to proceeding into a method, it's not possible to actually execute test code inside the delegate or fake
     * method after proceeding into the real constructor, nor to proceed into it more than once.
     *
     * @param replacementArguments the argument values to be passed to the real method, as replacement for the values
     *                             received by the delegate or fake method; if those received values should be passed
     *                             without replacement, then this method should be called with no values
     * @param <T> the return type of the target method
     *
     * @return the same value returned by the target method, if any
     *
     * @throws UnsupportedOperationException if attempting to proceed into a {@code native} method, or into an interface
     * or abstract method
     *
     * @see <a href="http://jmockit.org/tutorial/Faking.html#proceed" target="tutorial">Tutorial</a>
     */
    public final <T> T proceed(Object... replacementArguments)
    {
        //noinspection ClassReferencesSubclass
        return ((BaseInvocation) this).doProceed(replacementArguments);
    }
}
