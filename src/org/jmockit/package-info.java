/**
 * Provides the classes and annotations used when writing tests with the JMockit testing API.
 * <p/>
 * The {@linkplain org.jmockit.Tested @Tested} annotation allows for the instantiation of classes under test, with the
 * injection and recursive instantiation of dependencies.
 * It can inject <em>mocked</em> instances from {@linkplain org.jmockit.Mocked @Mocked} declarations, and
 * <em>non-mocked</em> (real) instances from other {@code @Tested} declarations.
 * Remaining real instances are automatically created and injected into applicable fields and/or constructor parameters
 * of tested classes.
 * <p/>
 * The {@link org.jmockit.Expectations} class provides an API for recording expected invocations which are later
 * replayed and implicitly verified.
 * This API applies only to types and instances for which there is a {@linkplain org.jmockit.Mocked @Mocked} declaration
 * in scope.
 * The {@link org.jmockit.Verifications} class allows expectations that were not recorded to be explicitly verified
 * <em>after</em> having exercised the code under test; apart from that, it is used in the same as {@code Expectations}.
 * <p/>
 * {@linkplain org.jmockit.Fake <code>Fake&lt;T></code>} allows the definition of fake implementations for external
 * classes, where {@code T} is the type to be faked.
 * Each public method in a fake class is meant to take the place of a method in {@code T}'s class hierarchy, when one is
 * found with the same signature.
 * Such "fake" methods can also (optionally) have an {@link org.jmockit.Invocation} parameter.
 * <p/>
 * For example-based descriptions of the various parts of the testing API, see the
 * <a href="http://jmockit.org/tutorial.html" target="tutorial">Tutorial</a>.
 */
package org.jmockit;
