// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.junit;

import fi.jumi.api.drivers.Driver;
import fi.jumi.core.drivers.DriverFinder;
import junit.framework.TestCase;
import org.hamcrest.Matchers;
import org.junit.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class JUnitCompatibilityDriverFinderTest {

    private JUnitCompatibilityDriverFinder finder;

    @Before
    public void setup() throws Exception {
        finder = new JUnitCompatibilityDriverFinder();
    }

    @Test
    public void supports_JUnit_3_tests() {
        Driver driver = finder.findTestClassDriver(JUnit3Test.class);

        assertThat(driver, is(instanceOf(JUnitCompatibilityDriver.class)));
    }

    @Test
    public void supports_JUnit_4_tests() {
        Driver driver = finder.findTestClassDriver(JUnit4Test.class);

        assertThat(driver, is(instanceOf(JUnitCompatibilityDriver.class)));
    }

    @Test
    public void does_not_support_non_JUnit_tests() {
        Driver driver = finder.findTestClassDriver(NotJUnitTest.class);

        assertThat(driver, Matchers.is(DriverFinder.DRIVER_NOT_FOUND));
    }

    private static class JUnit3Test extends TestCase {
    }

    private static class JUnit4Test {
        @Test
        public void foo() {
        }
    }

    private static class NotJUnitTest {
    }
}
