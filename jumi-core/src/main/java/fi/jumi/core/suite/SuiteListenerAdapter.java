// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.suite;

import fi.jumi.api.drivers.TestId;
import fi.jumi.core.api.*;
import fi.jumi.core.runs.*;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
class SuiteListenerAdapter implements RunListener {

    private final SuiteListener suiteListener;
    private final Class<?> testClass;

    public SuiteListenerAdapter(SuiteListener suiteListener, Class<?> testClass) {
        this.suiteListener = suiteListener;
        this.testClass = testClass;
    }

    @Override
    public void onTestFound(TestId testId, String name) {
        suiteListener.onTestFound(testClass.getName(), testId, name);
    }

    @Override
    public void onRunStarted(RunId runId) {
        suiteListener.onRunStarted(runId, testClass.getName());
    }

    @Override
    public void onTestStarted(RunId runId, TestId testId) {
        suiteListener.onTestStarted(runId, testId);
    }

    @Override
    public void onPrintedOut(RunId runId, String text) {
        suiteListener.onPrintedOut(runId, text);
    }

    @Override
    public void onPrintedErr(RunId runId, String text) {
        suiteListener.onPrintedErr(runId, text);
    }

    @Override
    public void onFailure(RunId runId, TestId testId, Throwable cause) {
        suiteListener.onFailure(runId, StackTrace.copyOf(cause));
    }

    @Override
    public void onTestFinished(RunId runId, TestId testId) {
        suiteListener.onTestFinished(runId);
    }

    @Override
    public void onRunFinished(RunId runId) {
        suiteListener.onRunFinished(runId);
    }
}
