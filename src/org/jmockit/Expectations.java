package org.jmockit;

/**
 * Allows expectations on {@linkplain Mocked mocked} types and instances to be <em>recorded before</em> they get invoked
 * from code under test.
 * Each expectation is specified by calling the expected mocked method or constructor from inside the instance
 * initialization block of a subclass.
 * Calling other methods or constructors in said block is not allowed.
 */
public class Expectations extends Invocations {
    protected Expectations() {
    }
}
