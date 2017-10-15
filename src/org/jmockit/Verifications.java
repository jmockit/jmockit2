package org.jmockit;

/**
 * Allows expectations on {@linkplain Mocked mocked} types and instances to be <em>verified after</em> they get invoked
 * from code under test, similar to how expectations get recorded using {@link Expectations}.
 */
public class Verifications extends Invocations {
    protected Verifications() {
    }
}
