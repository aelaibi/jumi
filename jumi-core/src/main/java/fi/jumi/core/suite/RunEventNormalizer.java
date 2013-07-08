// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.suite;

import fi.jumi.api.drivers.TestId;
import fi.jumi.core.api.*;
import fi.jumi.core.runs.*;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

@NotThreadSafe
class RunEventNormalizer implements RunListener {

    private final SuiteListener listener;
    private final TestFile testFile;
    private final Map<TestId, String> testNamesById = new HashMap<>();

    public RunEventNormalizer(SuiteListener listener, TestFile testFile) {
        this.listener = listener;
        this.testFile = testFile;
    }

    @Override
    public void onTestFound(TestId testId, String name) {
        if (hasNotBeenFoundBefore(testId)) {
            checkParentWasFoundFirst(testId);
            rememberFoundTest(testId, name);
            listener.onTestFound(testFile, testId, name);
        } else {
            checkNameIsSameAsBefore(testId, name);
        }
    }

    private void rememberFoundTest(TestId testId, String name) {
        testNamesById.put(testId, name);
    }

    private boolean hasNotBeenFoundBefore(TestId testId) {
        return !testNamesById.containsKey(testId);
    }

    private void checkParentWasFoundFirst(TestId testId) {
        if (!testId.isRoot() && hasNotBeenFoundBefore(testId.getParent())) {
            throw new IllegalStateException("parent of " + testId + " must be found first");
        }
    }

    private void checkNameIsSameAsBefore(TestId testId, String newName) {
        String oldName = testNamesById.get(testId);
        if (oldName != null && !oldName.equals(newName)) {
            throw new IllegalArgumentException("test " + testId + " was already found with another name: " + oldName);
        }
    }

    // events which are delegated as-is

    @Override
    public void onInternalError(String message, Throwable cause) {
        listener.onInternalError(message, StackTrace.copyOf(cause));
    }

    @Override
    public void onRunStarted(RunId runId) {
        listener.onRunStarted(runId, testFile);
    }

    @Override
    public void onTestStarted(RunId runId, TestId testId) {
        listener.onTestStarted(runId, testId);
    }

    @Override
    public void onPrintedOut(RunId runId, String text) {
        listener.onPrintedOut(runId, text);
    }

    @Override
    public void onPrintedErr(RunId runId, String text) {
        listener.onPrintedErr(runId, text);
    }

    @Override
    public void onFailure(RunId runId, TestId testId, Throwable cause) {
        listener.onFailure(runId, StackTrace.copyOf(cause));
    }

    @Override
    public void onTestFinished(RunId runId, TestId testId) {
        listener.onTestFinished(runId);
    }

    @Override
    public void onRunFinished(RunId runId) {
        listener.onRunFinished(runId);
    }
}
