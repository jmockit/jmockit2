package acceptanceTests;

import org.jmockit.*;
import static org.junit.Assert.*;

import org.junit.*;

public final class TestedObjectsTest
{
    @Tested ClassToBeTested cut;

    @Tested
    Class<?> resolveDependencyInterfaces(Class<? extends AnotherDependency> dependencyType) {
        return AnotherDependencyImpl.class;
    }

    @Test
    public void createCUTUsingNoArgsConstructorWithFieldInjectedDependency() {
        assertNotNull(cut);
        assertNotNull(cut.dependency);
    }

    @Test
    public void createCUTUsingConstructorFedWithTestedParameter(@Tested("testing") String text) {
        assertEquals("testing", text);
        assertEquals(text, cut.text);
        assertNotNull(cut.dependency);
    }

    @Test
    public void declareTestedParameterOfPrimitiveType(@Tested("123") int value) {
        assertEquals(123, value);
    }
}
