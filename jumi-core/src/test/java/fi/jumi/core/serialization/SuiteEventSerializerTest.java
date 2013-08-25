// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.serialization;

import fi.jumi.api.drivers.TestId;
import fi.jumi.core.api.*;
import fi.jumi.core.ipc.*;
import fi.jumi.core.runs.RunIdSequence;
import fi.jumi.core.util.SpyListener;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.*;
import org.junit.rules.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;

import static fi.jumi.core.util.ConcurrencyUtil.runConcurrently;
import static fi.jumi.core.util.EqualityMatchers.deepEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class SuiteEventSerializerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Rule
    public final TemporaryFolder tempDir = new TemporaryFolder();


    @Test
    public void serializes_and_deserializes_all_events() {
        SpyListener<SuiteListener> spy = new SpyListener<>(SuiteListener.class);
        exampleUsage(spy.getListener());
        spy.replay();
        IpcBuffer buffer = newIpcBuffer();

        // serialize
        SuiteEventSerializer serializer = new SuiteEventSerializer(buffer);
        serializer.start();
        exampleUsage(serializer);
        serializer.end();

        // deserialize
        buffer.position(0);
        SuiteEventSerializer.deserialize(buffer, spy.getListener());

        spy.verify();
    }

    @Test
    public void example_usage_invokes_every_method_in_the_interface() {
        SuiteListenerSpy spy = new SuiteListenerSpy();

        exampleUsage(spy);

        for (Method method : SuiteListener.class.getMethods()) {
            assertThat("invoked methods", spy.methodInvocations.keySet(), hasItem(method));
        }
    }

    private static void exampleUsage(SuiteListener listener) {
        TestFile testFile = TestFile.fromClassName("com.example.SampleTest");
        RunId runId = new RunId(1);

        listener.onSuiteStarted();
        listener.onTestFileFound(testFile);
        listener.onAllTestFilesFound();

        listener.onTestFound(testFile, TestId.ROOT, "SampleTest");
        listener.onTestFound(testFile, TestId.of(0), "testName");

        listener.onRunStarted(runId, testFile);
        listener.onTestStarted(runId, TestId.ROOT);
        listener.onTestStarted(runId, TestId.of(0));
        listener.onPrintedOut(runId, "printed to out");
        listener.onPrintedErr(runId, "printed to err");
        listener.onFailure(runId, StackTrace.from(new AssertionError("assertion message"))); // TODO: add cause and suppressed, also test the stack trace elements
        listener.onTestFinished(runId);
        listener.onTestFinished(runId);
        listener.onRunFinished(runId);

        listener.onInternalError("error message", StackTrace.from(new Exception("exception message")));
        listener.onTestFileFinished(testFile);
        listener.onSuiteFinished();
    }

    @Ignore // TODO
    @Test
    public void test_concurrent_producer_and_consumer() throws Exception {
        SpyListener<SuiteListener> expectations = new SpyListener<>(SuiteListener.class);
        lotsOfEventsForConcurrencyTesting(expectations.getListener());
        expectations.replay();

        Path mmf = tempDir.getRoot().toPath().resolve("mmf");

        Runnable producer = () -> {
            IpcBuffer buffer = new IpcBuffer(new MappedByteBufferSequence(new FileSegmenter(mmf, 4 * 1024, 512 * 1024)));

            SuiteEventSerializer serializer = new SuiteEventSerializer(buffer);
            serializer.start();
            lotsOfEventsForConcurrencyTesting(serializer);
            serializer.end();
        };

        Runnable consumer = () -> {
            IpcBuffer buffer = new IpcBuffer(new MappedByteBufferSequence(
                    new FileSegmenter(mmf, 1024, 1024))); // different segment size, because the reader should not create new segments

            SuiteEventSerializer.deserialize(buffer, expectations.getListener());
        };

        runConcurrently(producer, consumer);
    }

    private static void lotsOfEventsForConcurrencyTesting(SuiteListener listener) {
        TestFile testFile = TestFile.fromClassName("DummyTest");
        RunIdSequence runIds = new RunIdSequence();
        for (int i = 0; i < 10000; i++) {
            RunId runId = runIds.nextRunId();

            // Not a realistic scenario, because we are only interested in concurrency testing
            // the IPC protocol and not the specifics of a particular interface.
            listener.onSuiteStarted();
            listener.onRunStarted(runId, testFile);
            listener.onRunFinished(runId);
            listener.onSuiteFinished();
        }
    }


    // headers

    @Test
    public void cannot_deserialize_if_header_has_wrong_magic_bytes() {
        IpcBuffer buffer = serializeSomeEvents();

        buffer.setInt(0, 1);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("wrong header: expected 4A 75 6D 69 but was 00 00 00 01");
        tryToDeserialize(buffer);
    }

    @Test
    public void cannot_deserialize_if_header_has_wrong_protocol_version() {
        IpcBuffer buffer = serializeSomeEvents();

        buffer.setInt(4, 9999);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("unsupported protocol version: 9999");
        tryToDeserialize(buffer);
    }

    @Test
    public void cannot_deserialize_if_header_has_wrong_interface() {
        IpcBuffer buffer = serializeSomeEvents();

        buffer.position(8);
        new SuiteEventSerializer(buffer).writeString("com.example.AnotherInterface");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("wrong interface: expected fi.jumi.core.api.SuiteListener but was com.example.AnotherInterface");
        tryToDeserialize(buffer);
    }

    @Test
    public void cannot_deserialize_if_header_has_wrong_interface_version() {
        IpcBuffer buffer = serializeSomeEvents();

        buffer.position(8);
        SuiteEventSerializer.readString(buffer); // go to interface version's position
        buffer.writeInt(9999);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("unsupported interface version: 9999");
        tryToDeserialize(buffer);
    }

    private static IpcBuffer serializeSomeEvents() {
        IpcBuffer buffer = newIpcBuffer();
        SuiteEventSerializer serializer = new SuiteEventSerializer(buffer);
        serializer.start();
        serializer.onSuiteStarted();
        serializer.end();
        return buffer;
    }

    private static void tryToDeserialize(IpcBuffer buffer) {
        buffer.position(0);
        SuiteEventSerializer.deserialize(buffer, mock(SuiteListener.class));
    }


    // StackTrace

    @Test
    public void test_serialization_of_StackTrace() {
        StackTrace original = StackTrace.from(new IOException());

        assertThat(roundTripStackTrace(original), is(deepEqualTo(original)));
    }

    @Test
    public void test_serialization_of_StackTrace_with_message() {
        StackTrace original = StackTrace.from(new IOException("the message"));

        assertThat(roundTripStackTrace(original), is(deepEqualTo(original)));
    }

    @Test
    public void test_serialization_of_StackTrace_with_causes() {
        StackTrace original = StackTrace.from(
                new IOException("the message",
                        new IllegalArgumentException("cause 1",
                                new IllegalStateException("cause 2"))));

        assertThat(roundTripStackTrace(original), is(deepEqualTo(original)));
    }

    @Test
    public void test_serialization_of_StackTrace_with_suppressed() {
        IOException e = new IOException("the message");
        e.addSuppressed(new IllegalArgumentException("suppressed 1"));
        e.addSuppressed(new IllegalStateException("suppressed 2"));
        StackTrace original = StackTrace.from(e);

        assertThat(roundTripStackTrace(original), is(deepEqualTo(original)));
    }


    // String

    @Test
    public void test_serialization_of_String() {
        assertThat("empty string", roundTripString(""), is(""));

        String original = RandomStringUtils.random(10);
        assertThat("random string", roundTripString(original), is(original));
    }

    @Test
    public void test_serialization_of_String_with_non_printable_characters() {
        for (char c = 0; c < ' '; c++) {
            String original = "" + c;
            assertThat("0x" + Integer.toHexString(c), roundTripString(original), is(original));
        }
    }

    @Test
    public void test_serialization_of_null_String() {
        String nullString = null;
        assertThat("null string", roundTripNullableString(nullString), is(nullString));

        try {
            serializeAndDeserialize(nullString, SuiteEventSerializer::writeString, SuiteEventSerializer::readNullableString);
            fail("should have thrown NullPointerException on serialization");
        } catch (NullPointerException e) {
            // OK
        }

        try {
            serializeAndDeserialize(nullString, SuiteEventSerializer::writeNullableString, SuiteEventSerializer::readString);
            fail("should have thrown NullPointerException on deserialization");
        } catch (NullPointerException e) {
            // OK
        }
    }


    // helpers

    private static StackTrace roundTripStackTrace(StackTrace expected) {
        return serializeAndDeserialize(expected, SuiteEventSerializer::writeStackTrace, SuiteEventSerializer::readStackTrace);
    }

    private static String roundTripString(String original) {
        return serializeAndDeserialize(original, SuiteEventSerializer::writeString, SuiteEventSerializer::readString);
    }

    private static String roundTripNullableString(String original) {
        return serializeAndDeserialize(original, SuiteEventSerializer::writeNullableString, SuiteEventSerializer::readNullableString);
    }

    private static <T> T serializeAndDeserialize(T original, WriteOp<T> writeOp, ReadOp<T> readOp) {
        IpcBuffer buffer = newIpcBuffer();
        writeOp.write(new SuiteEventSerializer(buffer), original);
        buffer.position(0);
        return readOp.read(buffer);
    }

    private interface WriteOp<T> {
        void write(SuiteEventSerializer target, T data);
    }

    private interface ReadOp<T> {
        T read(IpcBuffer source);
    }

    private static IpcBuffer newIpcBuffer() {
        return new IpcBuffer(new AllocatedByteBufferSequence(100, 30 * 1000));
    }
}
