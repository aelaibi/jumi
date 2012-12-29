// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.launcher.ui;

import fi.jumi.actors.eventizers.Event;
import fi.jumi.actors.queue.MessageQueue;
import fi.jumi.api.drivers.TestId;
import fi.jumi.core.*;
import fi.jumi.core.events.suiteListener.SuiteListenerToEvent;
import fi.jumi.core.runs.RunId;
import org.junit.Test;

import static fi.jumi.core.util.Asserts.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TextUITest {

    private static final String SUMMARY_LINE = "Pass";

    private final MessageQueue<Event<SuiteListener>> stream = new MessageQueue<>();
    private final SuiteListener listener = new SuiteListenerToEvent(stream);
    private final EventBuilder suite = new EventBuilder(listener);

    private final StringBuilder out = new StringBuilder();
    private final TextUI ui = new TextUI(stream, new PlainTextPrinter(out));


    private String runAndGetOutput() {
        ui.update();
        return forceUnixLineSeparators(out.toString());
    }

    private static String forceUnixLineSeparators(String output) {
        String lineSeparator = System.getProperty("line.separator");
        return output.replaceAll(lineSeparator, "\n");
    }

    private void assertInOutput(String... expectedLines) {
        assertContainsSubStrings(runAndGetOutput(), expectedLines);
    }

    private void assertNotInOutput(String... expectedLines) {
        assertNotContainsSubStrings(runAndGetOutput(), expectedLines);
    }


    // updating

    @Test(timeout = 1000L)
    public void can_update_non_blockingly() {
        ui.update(); // given no events in stream, should exit quickly

        assertNotInOutput(SUMMARY_LINE);
    }

    @Test(timeout = 1000L)
    public void can_update_blockingly() throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                SuiteMother.emptySuite(listener);
            }
        });
        t.start();

        ui.updateUntilFinished(); // should exit only after all events have arrived

        assertInOutput(SUMMARY_LINE);
    }


    // summary line

    @Test
    public void summary_line_for_no_tests() {
        SuiteMother.emptySuite(listener);

        assertInOutput("Pass: 0, Fail: 0, Total: 0");
    }

    @Test
    public void summary_line_for_one_passing_test() {
        SuiteMother.onePassingTest(listener);

        assertInOutput("Pass: 1, Fail: 0, Total: 1");
    }

    @Test
    public void summary_line_for_one_failing_test() {
        SuiteMother.oneFailingTest(listener);

        assertInOutput("Pass: 0, Fail: 1, Total: 1");
    }

    @Test
    public void summary_line_for_multiple_nested_tests() {
        SuiteMother.nestedFailingAndPassingTests(listener);

        assertInOutput("Pass: 2, Fail: 1, Total: 3");
    }

    @Test
    public void summary_line_is_not_printed_until_all_events_have_arrived() {
        suite.begin();
        assertNotInOutput(SUMMARY_LINE);

        suite.end();
        assertInOutput(SUMMARY_LINE);
    }

    @Test
    public void each_TestClass_TestId_pair_is_counted_only_once_in_the_summary() {
        suite.begin();

        final RunId run1 = suite.nextRunId();
        suite.runStarted(run1, SuiteMother.TEST_CLASS);
        suite.test(run1, TestId.ROOT, SuiteMother.TEST_CLASS_NAME, new Runnable() {
            @Override
            public void run() {
                suite.test(run1, TestId.of(0), "test one");
            }
        });
        suite.runFinished(run1);

        // same root test is executed twice, but should be counted only once in the total
        final RunId run2 = suite.nextRunId();
        suite.runStarted(run2, SuiteMother.TEST_CLASS);
        suite.test(run2, TestId.ROOT, SuiteMother.TEST_CLASS_NAME, new Runnable() {
            @Override
            public void run() {
                suite.test(run2, TestId.of(1), "test two");
            }
        });
        suite.runFinished(run2);

        // a different test class, same TestId, should be counted separately
        final RunId run3 = suite.nextRunId();
        suite.runStarted(run3, "com.example.AnotherDummyTest");
        suite.test(run3, TestId.ROOT, "AnotherDummyTest");
        suite.runFinished(run3);
        suite.end();

        assertInOutput("Pass: 4, Fail: 0, Total: 4");
    }


    // test runs

    @Test
    public void prints_test_run_header() {
        suite.begin();
        RunId run1 = suite.nextRunId();
        suite.runStarted(run1, "com.example.DummyTest");
        suite.test(run1, TestId.ROOT, "Human-readable name");
        suite.runFinished(run1);
        suite.end();

        // expected content:
        // - run ID
        // - full name of the test class
        // - human-readable name of the test class (it MAY be different from class name)
        assertInOutput(
                "Run #1 in com.example.DummyTest",
                "Human-readable name"
        );
    }

    @Test
    public void test_run_header_is_printed_for_each_test_run() {
        SuiteMother.twoPassingRuns(listener);

        assertInOutput(
                "Run #1 in com.example.DummyTest",
                "Run #2 in com.example.DummyTest"
        );
    }

    @Test
    public void test_run_header_is_printed_only_once_per_test_run() {
        suite.begin();
        final RunId run1 = suite.nextRunId();

        // First test of the test run - should print the class name
        suite.runStarted(run1, SuiteMother.TEST_CLASS);
        suite.test(run1, TestId.ROOT, "Human readable name of test class", new Runnable() {
            @Override
            public void run() {

                // Second test of the test run - should NOT print the class name a second time,
                // because a test run cannot span many classes
                suite.test(run1, TestId.of(0), "test one");
            }
        });
        suite.runFinished(run1);
        suite.end();

        assertInOutput(SuiteMother.TEST_CLASS); // should show once
        assertNotInOutput(SuiteMother.TEST_CLASS, SuiteMother.TEST_CLASS); // should not show twice
    }

    @Test
    public void there_is_a_spacer_between_test_runs() {
        SuiteMother.twoPassingRuns(listener);

        assertInOutput("" +
                " > - DummyTest\n" +
                "\n" +                  // the expected spacer - an empty line
                " > Run #2");
    }

    @Test
    public void interleaved_test_runs_are_reported_without_interleaving() {
        SuiteMother.twoInterleavedRuns(listener);

        assertInOutput(
                "Run #1",
                "+ testOne",
                "- testOne",
                "Run #2",
                "+ testTwo",
                "- testTwo"
        );
    }


    // test names

    @Test
    public void prints_that_when_a_test_starts_and_ends() {
        suite.begin();
        RunId run1 = suite.nextRunId();
        suite.runStarted(run1, SuiteMother.TEST_CLASS);
        suite.test(run1, TestId.ROOT, "Dummy test");
        suite.runFinished(run1);
        suite.end();

        assertInOutput(
                "+ Dummy test",
                "- Dummy test"
        );
    }

    @Test
    public void prints_with_indentation_that_when_a_nested_test_starts_and_ends() {
        suite.begin();
        final RunId run1 = suite.nextRunId();
        suite.runStarted(run1, SuiteMother.TEST_CLASS);
        suite.test(run1, TestId.ROOT, "Dummy test", new Runnable() {
            @Override
            public void run() {
                suite.test(run1, TestId.of(0), "test one");
                suite.test(run1, TestId.of(1), "test two", new Runnable() {
                    @Override
                    public void run() {
                        suite.test(run1, TestId.of(1, 0), "deeply nested test");
                    }
                });
            }
        });
        suite.runFinished(run1);
        suite.end();

        assertInOutput(
                "> + Dummy test",
                ">   + test one",
                ">   - test one",
                ">   + test two",
                ">     + deeply nested test",
                ">     - deeply nested test",
                ">   - test two",
                "> - Dummy test"
        );
    }


    // stack traces

    @Test
    public void prints_failure_stack_traces() {
        SuiteMother.oneFailingTest(listener);

        assertInOutput("java.lang.Throwable: dummy exception");
    }

    @Test
    public void prints_failure_stack_traces_only_after_the_test_is_finished() {
        suite.begin();
        {
            {
                RunId run1 = suite.nextRunId();
                listener.onTestFound(SuiteMother.TEST_CLASS, TestId.ROOT, SuiteMother.TEST_CLASS_NAME);
                listener.onRunStarted(run1, SuiteMother.TEST_CLASS);
                listener.onTestStarted(run1, TestId.ROOT);
                listener.onFailure(run1, StackTrace.copyOf(new Throwable("dummy exception")));

                assertNotInOutput("java.lang.Throwable: dummy exception");

                listener.onTestFinished(run1);
                listener.onRunFinished(run1);
            }

            assertInOutput("java.lang.Throwable: dummy exception");
        }
        suite.end();
    }

    @Test
    public void prints_failure_stack_traces_only_after_the_surrounding_test_is_finished() { // i.e. the test run is finished
        suite.begin();
        {
            {
                RunId run1 = suite.nextRunId();
                suite.runStarted(run1, SuiteMother.TEST_CLASS);
                listener.onTestFound(SuiteMother.TEST_CLASS, TestId.ROOT, SuiteMother.TEST_CLASS_NAME);
                listener.onTestStarted(run1, TestId.ROOT);
                suite.failingTest(run1, TestId.of(0), "testOne",
                        new Throwable("dummy exception")
                );

                assertNotInOutput("java.lang.Throwable: dummy exception");

                listener.onTestFinished(run1);
                listener.onRunFinished(run1);
            }
            assertInOutput("java.lang.Throwable: dummy exception");
        }
        suite.end();
    }


    // standard output

    @Test
    public void prints_what_tests_printed_to_stdout() {
        SuiteMother.printsToStdout(listener);

        assertInOutput("printed to stdout\n");
    }

    @Test
    public void prints_what_tests_printed_to_stderr() {
        SuiteMother.printsToStderr(listener);

        assertInOutput("printed to stderr\n");
    }

    @Test
    public void always_puts_test_start_and_end_events_on_a_new_line() {
        SuiteMother.printsToStdoutWithoutNewlineAtEnd(listener);

        String printedToStdout = "this doesn't end with newline";
        String theNextMetaLine = " > -";
        assertInOutput(printedToStdout + "\n" + theNextMetaLine);
    }


    // configuration

    @Test
    public void can_choose_to_show_passing_tests() {
        ui.setPassingTestsVisible(true);

        SuiteMother.onePassingTest(listener);

        assertInOutput("Run #1");
        assertInOutput("Pass: 1");
    }

    @Test
    public void can_choose_to_hide_passing_tests() {
        ui.setPassingTestsVisible(false);

        SuiteMother.onePassingTest(listener);

        assertNotInOutput("Run #1");
        assertInOutput("Pass: 1");
    }


    // suite results

    @Test
    public void tells_when_the_suite_had_no_failures() {
        SuiteMother.onePassingTest(listener);
        runAndGetOutput();

        assertThat("has failures", ui.hasFailures(), is(false));
    }

    @Test
    public void tells_when_the_suite_had_some_failures() {
        SuiteMother.oneFailingTest(listener);
        runAndGetOutput();

        assertThat("has failures", ui.hasFailures(), is(true));
    }
}
