package acceptanceTests;

import org.jmockit.*;
import static org.junit.Assert.*;

import org.junit.*;

public final class TestedObjectsTest
{
    @Tested ClassToBeTested cut;

    @Test
    public void automaticallyCreatedClassUnderTestAndFieldInjectedDependency() {
        assertNotNull(cut);
        assertNotNull(cut.dependency);
    }
}
