package acceptanceTests;

import org.jmockit.*;
import static org.junit.Assert.*;

import org.junit.*;

public final class TestedObjectsTest
{
    @Tested ClassToBeTested cut;

//    @Test
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
}
