// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.launcher.remote;

import fi.jumi.actors.ActorRef;
import fi.jumi.actors.eventizers.Event;
import fi.jumi.actors.queue.MessageSender;
import fi.jumi.core.CommandListener;
import fi.jumi.core.api.*;
import fi.jumi.core.config.*;
import fi.jumi.core.network.*;
import fi.jumi.core.util.SpyListener;
import fi.jumi.launcher.FakeProcess;
import fi.jumi.launcher.daemon.Steward;
import fi.jumi.launcher.process.*;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Test;

import java.io.*;

import static fi.jumi.core.util.AsyncAssert.assertEventually;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

public class ProcessStartingDaemonSummonerTest {

    private static final int TIMEOUT = 1000;

    private final Steward steward = mock(Steward.class);
    private final SpyProcessStarter processStarter = new SpyProcessStarter();
    private final SpyNetworkServer daemonConnector = new SpyNetworkServer();
    private final StringWriter outputListener = new StringWriter();

    private final ProcessStartingDaemonSummoner daemonSummoner = new ProcessStartingDaemonSummoner(
            steward,
            processStarter,
            daemonConnector,
            new WriterOutputStream(outputListener)
    );

    private final SuiteConfiguration dummySuiteConfig = new SuiteConfiguration();
    private final DaemonConfiguration dummyDaemonConfig = new DaemonConfiguration();
    private final ActorRef<DaemonListener> dummyListener = ActorRef.wrap(mock(DaemonListener.class));

    @Test
    public void tells_to_daemon_the_socket_to_contact() {
        daemonConnector.portToReturn = 123;

        daemonSummoner.connectToDaemon(dummySuiteConfig, dummyDaemonConfig, dummyListener);

        DaemonConfiguration daemonConfig = parseDaemonArguments(processStarter.lastArgs);
        assertThat(daemonConfig.launcherPort(), is(123));
    }

    @Test
    public void tells_to_output_listener_what_the_daemon_prints() {
        processStarter.processToReturn.inputStream = new ByteArrayInputStream("hello".getBytes());

        daemonSummoner.connectToDaemon(dummySuiteConfig, dummyDaemonConfig, dummyListener);

        assertEventually(outputListener, hasToString("hello"), TIMEOUT);
    }

    @Test
    public void reports_an_internal_error_if_the_daemon_fails_to_connect_within_a_timeout() throws InterruptedException {
        processStarter.processToReturn.inputStream = new ByteArrayInputStream("dummy JVM failure".getBytes());

        SpyListener<SuiteListener> spy = new SpyListener<>(SuiteListener.class);
        SuiteListener expect = spy.getListener();

        expect.onSuiteStarted();
        expect.onInternalError("Failed to start the test runner daemon process",
                StackTrace.copyOf(new RuntimeException("Could not connect to the daemon: timed out after 0 ms")));
        expect.onSuiteFinished();

        spy.replay();
        DaemonConfiguration daemon = new DaemonConfigurationBuilder()
                .startupTimeout(0)
                .freeze();
        daemonSummoner.connectToDaemon(dummySuiteConfig, daemon, actorRef(new FakeDaemonListener(expect)));

        Thread.sleep(100); // XXX
        spy.verify();
    }

    // TODO: unit test that the events from the daemon are sent to the listener


    // helpers

    private static ActorRef<DaemonListener> actorRef(DaemonListener proxy) {
        return ActorRef.wrap(proxy);
    }

    private static DaemonConfiguration parseDaemonArguments(String[] args) {
        return new DaemonConfigurationBuilder()
                .parseProgramArgs(args)
                .freeze();
    }

    private static class SpyProcessStarter implements ProcessStarter {

        public String[] lastArgs;
        public FakeProcess processToReturn = new FakeProcess();

        @Override
        public Process startJavaProcess(JvmArgs jvmArgs) throws IOException {
            this.lastArgs = jvmArgs.programArgs.toArray(new String[0]);
            return processToReturn;
        }
    }

    private static class SpyNetworkServer implements NetworkServer {

        public int portToReturn = 1;

        @Override
        public <In, Out> int listenOnAnyPort(NetworkEndpointFactory<In, Out> endpointFactory) {
            return portToReturn;
        }

        @Override
        public void close() throws IOException {
        }
    }

    private static class FakeDaemonListener implements DaemonListener {

        private final SuiteListener suiteListener;

        private FakeDaemonListener(SuiteListener suiteListener) {
            this.suiteListener = suiteListener;
        }

        @Override
        public void onConnected(NetworkConnection connection, MessageSender<Event<CommandListener>> sender) {
        }

        @Override
        public void onMessage(Event<SuiteListener> message) {
            message.fireOn(suiteListener);
        }

        @Override
        public void onDisconnected() {
        }
    }
}
