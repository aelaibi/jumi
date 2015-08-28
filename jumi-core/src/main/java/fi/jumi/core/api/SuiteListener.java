// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.api;

import fi.jumi.actors.generator.GenerateEventizer;
import fi.jumi.api.drivers.TestId;

@GenerateEventizer(targetPackage = "fi.jumi.core.events")
public interface SuiteListener {

    void onSuiteStarted();

    void onInternalError(String message, StackTrace cause);

    void onTestFileFound(TestFile testFile);

    void onAllTestFilesFound();

    void onTestFound(TestFile testFile, TestId testId, String name);

    void onRunStarted(RunId runId, TestFile testFile);

    void onTestStarted(RunId runId, TestId testId);

    void onPrintedOut(RunId runId, String text);

    void onPrintedErr(RunId runId, String text);

    void onFailure(RunId runId, StackTrace cause);

    void onTestFinished(RunId runId);

    void onRunFinished(RunId runId);

    void onTestFileFinished(TestFile testFile);

    void onSuiteFinished();
}
