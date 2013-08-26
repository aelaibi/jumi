// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.serialization;

import fi.jumi.actors.eventizers.Event;
import fi.jumi.api.drivers.TestId;
import fi.jumi.core.api.*;
import fi.jumi.core.ipc.IpcBuffer;

import javax.annotation.concurrent.NotThreadSafe;
import java.nio.file.Paths;
import java.util.*;

@NotThreadSafe
public class SuiteListenerEncoding implements SuiteListener, MessageEncoding<SuiteListener> {

    private static final byte onSuiteStarted = 1;
    private static final byte onInternalError = 2;
    private static final byte onTestFileFound = 3;
    private static final byte onAllTestFilesFound = 4;
    private static final byte onTestFound = 5;
    private static final byte onRunStarted = 6;
    private static final byte onTestStarted = 7;
    private static final byte onPrintedOut = 8;
    private static final byte onPrintedErr = 9;
    private static final byte onFailure = 10;
    private static final byte onTestFinished = 11;
    private static final byte onRunFinished = 12;
    private static final byte onTestFileFinished = 13;
    private static final byte onSuiteFinished = 14;

    private final IpcBuffer buffer;

    public SuiteListenerEncoding(IpcBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public String getInterfaceName() {
        return SuiteListener.class.getName();
    }

    @Override
    public int getInterfaceVersion() {
        return 1;
    }

    @Override
    public void encode(Event<SuiteListener> message) {
        message.fireOn(this);
    }

    @Override
    public void decode(SuiteListener target) {
        byte type = readEventType();
        switch (type) {
            case onSuiteStarted:
                target.onSuiteStarted();
                break;
            case onInternalError:
                target.onInternalError(readString(), readStackTrace());
                break;
            case onTestFileFound:
                target.onTestFileFound(readTestFile());
                break;
            case onAllTestFilesFound:
                target.onAllTestFilesFound();
                break;
            case onTestFound:
                target.onTestFound(readTestFile(), readTestId(), readString());
                break;
            case onRunStarted:
                target.onRunStarted(readRunId(), readTestFile());
                break;
            case onTestStarted:
                target.onTestStarted(readRunId(), readTestId());
                break;
            case onPrintedOut:
                target.onPrintedOut(readRunId(), readString());
                break;
            case onPrintedErr:
                target.onPrintedErr(readRunId(), readString());
                break;
            case onFailure:
                target.onFailure(readRunId(), readStackTrace());
                break;
            case onTestFinished:
                target.onTestFinished(readRunId());
                break;
            case onRunFinished:
                target.onRunFinished(readRunId());
                break;
            case onTestFileFinished:
                target.onTestFileFinished(readTestFile());
                break;
            case onSuiteFinished:
                target.onSuiteFinished();
                break;
            default:
                throw new IllegalArgumentException("Unknown type " + type);
        }
    }

    // serializing events

    @Override
    public void onSuiteStarted() {
        writeEventType(onSuiteStarted);
    }

    @Override
    public void onInternalError(String message, StackTrace cause) {
        writeEventType(onInternalError);
        writeString(message);
        writeStackTrace(cause);
    }

    @Override
    public void onTestFileFound(TestFile testFile) {
        writeEventType(onTestFileFound);
        writeTestFile(testFile);
    }

    @Override
    public void onAllTestFilesFound() {
        writeEventType(onAllTestFilesFound);
    }

    @Override
    public void onTestFound(TestFile testFile, TestId testId, String name) {
        writeEventType(onTestFound);
        writeTestFile(testFile);
        writeTestId(testId);
        writeString(name);
    }

    @Override
    public void onRunStarted(RunId runId, TestFile testFile) {
        writeEventType(onRunStarted);
        writeRunId(runId);
        writeTestFile(testFile);
    }

    @Override
    public void onTestStarted(RunId runId, TestId testId) {
        writeEventType(onTestStarted);
        writeRunId(runId);
        writeTestId(testId);
    }

    @Override
    public void onPrintedOut(RunId runId, String text) {
        writeEventType(onPrintedOut);
        writeRunId(runId);
        writeString(text);
    }

    @Override
    public void onPrintedErr(RunId runId, String text) {
        writeEventType(onPrintedErr);
        writeRunId(runId);
        writeString(text);
    }

    @Override
    public void onFailure(RunId runId, StackTrace cause) {
        writeEventType(onFailure);
        writeRunId(runId);
        writeStackTrace(cause);
    }

    @Override
    public void onTestFinished(RunId runId) {
        writeEventType(onTestFinished);
        writeRunId(runId);
    }

    @Override
    public void onRunFinished(RunId runId) {
        writeEventType(onRunFinished);
        writeRunId(runId);
    }

    @Override
    public void onTestFileFinished(TestFile testFile) {
        writeEventType(onTestFileFinished);
        writeTestFile(testFile);
    }

    @Override
    public void onSuiteFinished() {
        writeEventType(onSuiteFinished);
    }


    // event type

    private byte readEventType() {
        return buffer.readByte();
    }

    private void writeEventType(byte type) {
        buffer.writeByte(type);
    }

    // TestFile

    private TestFile readTestFile() {
        return TestFile.fromPath(Paths.get(readString()));
    }

    private void writeTestFile(TestFile testFile) {
        writeString(testFile.getPath());
    }

    // TestId

    private TestId readTestId() {
        int[] path = new int[buffer.readInt()];
        for (int i = 0; i < path.length; i++) {
            path[i] = buffer.readInt();
        }
        return TestId.of(path);
    }

    private void writeTestId(TestId testId) {
        // TODO: extract this into TestId as "getPath(): int[]"
        List<Integer> path = new ArrayList<>();
        for (TestId id = testId; !id.isRoot(); id = id.getParent()) {
            path.add(id.getIndex());
        }
        Collections.reverse(path);

        buffer.writeInt(path.size());
        for (Integer index : path) {
            buffer.writeInt(index);
        }
    }

    // RunId

    private RunId readRunId() {
        return new RunId(buffer.readInt());
    }

    private void writeRunId(RunId runId) {
        buffer.writeInt(runId.toInt());
    }

    // StackTrace

    StackTrace readStackTrace() {
        return new StackTrace.Builder()
                .setExceptionClass(readString())
                .setToString(readString())
                .setMessage(readNullableString())
                .setStackTrace(readStackTraceElements())
                .setCause(readOptionalException())
                .setSuppressed(readManyExceptions())
                .build();
    }

    void writeStackTrace(StackTrace stackTrace) {
        writeString(stackTrace.getExceptionClass());
        writeString(stackTrace.toString());
        writeNullableString(stackTrace.getMessage());
        writeStackTraceElements(stackTrace.getStackTrace());
        writeOptionalException(stackTrace.getCause());
        writeManyExceptions(stackTrace.getSuppressed());
    }

    private StackTraceElement[] readStackTraceElements() {
        StackTraceElement[] elements = new StackTraceElement[buffer.readInt()];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = new StackTraceElement(readString(), readString(), readString(), buffer.readInt());
        }
        return elements;
    }

    private void writeStackTraceElements(StackTraceElement[] elements) {
        buffer.writeInt(elements.length);
        for (StackTraceElement element : elements) {
            writeString(element.getClassName());
            writeString(element.getMethodName());
            writeString(element.getFileName());
            buffer.writeInt(element.getLineNumber());
        }
    }

    private Throwable readOptionalException() {
        Throwable[] exceptions = readManyExceptions();
        return exceptions.length == 0 ? null : exceptions[0];
    }

    private void writeOptionalException(Throwable exception) {
        writeManyExceptions(exception == null ? new Throwable[0] : new Throwable[]{exception});
    }

    private Throwable[] readManyExceptions() {
        Throwable[] exceptions = new Throwable[buffer.readInt()];
        for (int i = 0; i < exceptions.length; i++) {
            exceptions[i] = readStackTrace();
        }
        return exceptions;
    }

    private void writeManyExceptions(Throwable[] exceptions) {
        buffer.writeInt(exceptions.length);
        for (Throwable exception : exceptions) {
            writeStackTrace((StackTrace) exception);
        }
    }

    // String

    private String readString() {
        return StringEncoding.readString(buffer);
    }

    private void writeString(String s) {
        StringEncoding.writeString(buffer, s);
    }

    private String readNullableString() {
        return StringEncoding.readNullableString(buffer);
    }

    private void writeNullableString(String s) {
        StringEncoding.writeNullableString(buffer, s);
    }
}
