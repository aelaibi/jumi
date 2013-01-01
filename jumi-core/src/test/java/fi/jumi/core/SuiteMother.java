// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core;

import fi.jumi.api.drivers.TestId;
import fi.jumi.core.api.SuiteListener;
import fi.jumi.core.runs.RunId;

public class SuiteMother {

    public static final String TEST_CLASS = "com.example.DummyTest";
    public static final String TEST_CLASS_NAME = "DummyTest";

    public static void emptySuite(SuiteListener listener) {
        EventBuilder suite = new EventBuilder(listener);
        suite.begin();
        suite.end();
    }


    // passing and failing tests

    public static void onePassingTest(SuiteListener listener) {
        EventBuilder suite = new EventBuilder(listener);
        suite.begin();

        RunId run1 = suite.nextRunId();
        suite.runStarted(run1, TEST_CLASS);
        suite.test(run1, TestId.ROOT, TEST_CLASS_NAME);
        suite.runFinished(run1);

        suite.end();
    }

    public static void oneFailingTest(SuiteListener listener) {
        EventBuilder suite = new EventBuilder(listener);
        suite.begin();

        RunId run1 = suite.nextRunId();
        suite.runStarted(run1, TEST_CLASS);
        suite.failingTest(run1, TestId.ROOT, TEST_CLASS_NAME,
                new Throwable("dummy exception")
        );
        suite.runFinished(run1);

        suite.end();
    }

    public static void nestedFailingAndPassingTests(SuiteListener listener) {
        final EventBuilder suite = new EventBuilder(listener);
        suite.begin();

        final RunId run1 = suite.nextRunId();
        suite.runStarted(run1, TEST_CLASS);
        suite.test(run1, TestId.ROOT, TEST_CLASS_NAME, new Runnable() {
            @Override
            public void run() {
                suite.test(run1, TestId.of(0), "testOne");
                suite.failingTest(run1, TestId.of(1), "testTwo",
                        new Throwable("dummy exception")
                );
            }
        });
        suite.runFinished(run1);

        suite.end();
    }


    // multiple runs

    public static void twoPassingRuns(SuiteListener listener) {
        final EventBuilder suite = new EventBuilder(listener);
        suite.begin();

        final RunId run1 = suite.nextRunId();
        suite.runStarted(run1, TEST_CLASS);
        suite.test(run1, TestId.ROOT, TEST_CLASS_NAME, new Runnable() {
            @Override
            public void run() {
                suite.test(run1, TestId.of(0), "testOne");
            }
        });
        suite.runFinished(run1);

        final RunId run2 = suite.nextRunId();
        suite.runStarted(run2, TEST_CLASS);
        suite.test(run2, TestId.ROOT, TEST_CLASS_NAME, new Runnable() {
            @Override
            public void run() {
                suite.test(run2, TestId.of(1), "testTwo");
            }
        });
        suite.runFinished(run2);

        suite.end();
    }

    public static void twoInterleavedRuns(final SuiteListener listener) {
        final EventBuilder suite = new EventBuilder(listener);
        suite.begin();

        final RunId run1 = suite.nextRunId();
        final RunId run2 = suite.nextRunId();
        suite.runStarted(run1, TEST_CLASS);
        suite.runStarted(run2, TEST_CLASS);
        listener.onTestFound(TEST_CLASS, TestId.of(0), "testOne");
        listener.onTestFound(TEST_CLASS, TestId.of(1), "testTwo");
        listener.onTestStarted(run1, TestId.of(0));
        listener.onTestStarted(run2, TestId.of(1));
        listener.onTestFinished(run1);
        listener.onTestFinished(run2);
        suite.runFinished(run1);
        suite.runFinished(run2);

        suite.end();
    }


    // standard output

    public static void printsToStdout(SuiteListener listener) {
        final EventBuilder suite = new EventBuilder(listener);
        suite.begin();

        final RunId run1 = suite.nextRunId();
        suite.runStarted(run1, TEST_CLASS);
        suite.test(run1, TestId.ROOT, TEST_CLASS_NAME, new Runnable() {
            @Override
            public void run() {
                suite.printOut(run1, "printed to stdout\n");
            }
        });
        suite.runFinished(run1);

        suite.end();
    }

    public static void printsToStderr(SuiteListener listener) {
        final EventBuilder suite = new EventBuilder(listener);
        suite.begin();

        final RunId run1 = suite.nextRunId();
        suite.runStarted(run1, TEST_CLASS);
        suite.test(run1, TestId.ROOT, TEST_CLASS_NAME, new Runnable() {
            @Override
            public void run() {
                suite.printErr(run1, "printed to stderr\n");
            }
        });
        suite.runFinished(run1);

        suite.end();
    }

    public static void printsToStdoutWithoutNewlineAtEnd(SuiteListener listener) {
        final EventBuilder suite = new EventBuilder(listener);
        suite.begin();

        final RunId run1 = suite.nextRunId();
        suite.runStarted(run1, TEST_CLASS);
        suite.test(run1, TestId.ROOT, TEST_CLASS_NAME, new Runnable() {
            @Override
            public void run() {
                suite.printOut(run1, "this doesn't end with newline");
            }
        });
        suite.runFinished(run1);

        suite.end();
    }
}
