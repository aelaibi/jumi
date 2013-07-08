// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.runs;

import fi.jumi.api.drivers.TestId;
import fi.jumi.core.api.RunId;

public interface RunListener {

    void onInternalError(String message, Throwable cause);

    void onTestFound(TestId testId, String name);

    void onRunStarted(RunId runId);

    void onTestStarted(RunId runId, TestId testId);

    void onPrintedOut(RunId runId, String text);

    void onPrintedErr(RunId runId, String text);

    void onFailure(RunId runId, TestId testId, Throwable cause);

    void onTestFinished(RunId runId, TestId testId);

    void onRunFinished(RunId runId);
}
