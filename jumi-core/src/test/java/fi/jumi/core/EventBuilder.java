// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core;

import fi.jumi.api.drivers.TestId;
import fi.jumi.core.api.*;

import java.util.*;

public class EventBuilder {

    private final SuiteListener listener;

    private final Map<RunId, TestFile> testFilesByRunId = new HashMap<>();
    private int nextRunId = RunId.FIRST_ID;

    public EventBuilder(SuiteListener listener) {
        this.listener = listener;
    }

    public RunId nextRunId() {
        try {
            return new RunId(nextRunId);
        } finally {
            nextRunId++;
        }
    }

    public void begin() {
        listener.onSuiteStarted();
    }

    public void end() {
        // XXX: We are not firing the onTestFileFound et al. events naturally, but this should still be valid according to the protocol.
        for (TestFile testFile : testFilesByRunId.values()) {
            listener.onTestFileFinished(testFile);
        }
        listener.onAllTestFilesFound();
        listener.onSuiteFinished();
    }

    public void internalError(String message, Throwable cause) {
        listener.onInternalError(message, StackTrace.copyOf(cause));
    }

    public void runStarted(RunId runId, TestFile testFile) {
        // XXX: We are not firing the onTestFileFound et al. events naturally, but this should still be valid according to the protocol.
        if (!testFilesByRunId.values().contains(testFile)) {
            listener.onTestFileFound(testFile);
        }
        testFilesByRunId.put(runId, testFile);
        listener.onRunStarted(runId, testFile);
    }

    public void runFinished(RunId runId) {
        listener.onRunFinished(runId);
    }

    public void test(RunId runId, TestId testId, String name, Runnable testBody) {
        TestFile testFile = testFilesByRunId.get(runId);
        assert testFile != null;
        listener.onTestFound(testFile, testId, name);

        listener.onTestStarted(runId, testId);
        testBody.run();
        listener.onTestFinished(runId);
    }

    public void test(RunId runId, TestId testId, String name) {
        test(runId, testId, name, new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    public void failingTest(final RunId runId, final TestId testId, String name, final Throwable cause) {
        test(runId, testId, name, new Runnable() {
            @Override
            public void run() {
                listener.onFailure(runId, StackTrace.copyOf(cause));
            }
        });
    }

    public void printOut(RunId runId, String text) {
        listener.onPrintedOut(runId, text);
    }

    public void printErr(RunId runId, String text) {
        listener.onPrintedErr(runId, text);
    }
}
