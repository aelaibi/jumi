// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.launcher.ui;

import fi.jumi.actors.eventizers.Event;
import fi.jumi.actors.queue.MessageReceiver;
import fi.jumi.api.drivers.TestId;
import fi.jumi.core.*;
import fi.jumi.core.results.*;
import fi.jumi.core.runs.RunId;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.*;

@NotThreadSafe
public class TextUI {

    // TODO: if multiple readers are needed, create a Streamer class per the original designs
    private final MessageReceiver<Event<SuiteListener>> eventStream;
    private final Printer printer;

    private final SuiteEventDemuxer demuxer = new SuiteEventDemuxer();
    private final SuitePrinter suitePrinter = new SuitePrinter();
    private boolean passingTestsVisible = true;

    public TextUI(MessageReceiver<Event<SuiteListener>> eventStream, Printer printer) {
        this.eventStream = eventStream;
        this.printer = printer;
    }

    public void setPassingTestsVisible(boolean passingTestsVisible) {
        this.passingTestsVisible = passingTestsVisible;
    }

    public void update() {
        while (!demuxer.isSuiteFinished()) {
            Event<SuiteListener> message = eventStream.poll();
            if (message == null) {
                break;
            }
            updateWithMessage(message);
        }
    }

    public void updateUntilFinished() throws InterruptedException {
        while (!demuxer.isSuiteFinished()) {
            Event<SuiteListener> message = eventStream.take();
            updateWithMessage(message);
        }
    }

    private void updateWithMessage(Event<SuiteListener> message) {
        demuxer.send(message);
        message.fireOn(suitePrinter);
    }


    // printing visitors

    @NotThreadSafe
    private class SuitePrinter extends NullSuiteListener {

        @Override
        public void onRunFinished(RunId runId) {
            if (passingTestsVisible || hasFailures(runId)) {
                demuxer.visitRun(runId, new RunPrinter());
            }
        }

        private boolean hasFailures(RunId runId) {
            SuiteResultsSummary tmp = new SuiteResultsSummary();
            demuxer.visitRun(runId, tmp);
            return tmp.getFailingTests() > 0;
        }

        @Override
        public void onSuiteFinished() {
            SuiteResultsSummary summary = new SuiteResultsSummary();
            demuxer.visitAllRuns(summary);
            printSuiteFooter(summary);
        }

        // visual style

        private void printSuiteFooter(SuiteResultsSummary summary) {
            int pass = summary.getPassingTests();
            int fail = summary.getFailingTests();
            int total = summary.getTotalTests();
            printer.printlnMeta(String.format("Pass: %d, Fail: %d, Total: %d", pass, fail, total));
        }
    }

    @NotThreadSafe
    private class RunPrinter implements RunVisitor {

        private int testNestingLevel = 0;

        @Override
        public void onRunStarted(RunId runId, String testClass) {
            printRunHeader(testClass, runId);
        }

        @Override
        public void onTestStarted(RunId runId, String testClass, TestId testId) {
            testNestingLevel++;
            printTestName("+", testClass, testId);
        }

        @Override
        public void onPrintedOut(RunId runId, String testClass, TestId testId, String text) {
            printer.printOut(text);
        }

        @Override
        public void onPrintedErr(RunId runId, String testClass, TestId testId, String text) {
            printer.printErr(text);
        }

        @Override
        public void onFailure(RunId runId, String testClass, TestId testId, Throwable cause) {
            printer.printErr(getStackTrace(cause));
        }

        @Override
        public void onTestFinished(RunId runId, String testClass, TestId testId) {
            printTestName("-", testClass, testId);
            testNestingLevel--;
        }

        @Override
        public void onRunFinished(RunId runId, String testClass) {
            printRunFooter();
        }

        // visual style

        private void printRunHeader(String testClass, RunId runId) {
            printer.printlnMeta(" > Run #" + runId.toInt() + " in " + testClass);
        }

        private void printTestName(String bullet, String testClass, TestId testId) {
            printer.printlnMeta(" > " + testNameIndent() + bullet + " " + demuxer.getTestName(testClass, testId));
        }

        private void printRunFooter() {
            printer.printlnMeta("");
        }

        private String testNameIndent() {
            StringBuilder indent = new StringBuilder();
            for (int i = 1; i < testNestingLevel; i++) {
                indent.append("  ");
            }
            return indent.toString();
        }
    }

    private static String getStackTrace(Throwable cause) {
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        cause.printStackTrace(writer);
        writer.close(); // XXX: line not tested
        return buffer.toString();
    }
}
