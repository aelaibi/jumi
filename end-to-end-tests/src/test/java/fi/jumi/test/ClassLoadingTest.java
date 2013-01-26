// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.test;

import org.junit.*;
import sample.ContextClassLoaderTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class ClassLoadingTest {

    @Rule
    public final AppRunner app = new AppRunner();

    @Test(timeout = Timeouts.END_TO_END_TEST)
    public void test_thread_context_class_loader_and_current_class_loader_are_the_same() throws Exception {
        app.runTests(ContextClassLoaderTest.class);

        assertThat(app.getRunOutput(ContextClassLoaderTest.class, "testContextClassLoader"),
                containsString("Current thread is jumi-test-"));
        app.checkSuitePasses();
    }

    // TODO: driver_thread_context_class_loader_and_current_class_loader_are_the_same
}
