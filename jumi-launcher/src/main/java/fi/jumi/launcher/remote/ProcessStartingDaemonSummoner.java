// Copyright © 2011-2014, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.launcher.remote;

import fi.jumi.actors.ActorRef;
import fi.jumi.actors.eventizers.Event;
import fi.jumi.actors.eventizers.dynamic.DynamicEventizer;
import fi.jumi.actors.queue.*;
import fi.jumi.core.api.*;
import fi.jumi.core.config.*;
import fi.jumi.core.events.suiteListener.*;
import fi.jumi.core.ipc.api.RequestListener;
import fi.jumi.core.network.*;
import fi.jumi.core.util.Boilerplate;
import fi.jumi.core.util.timeout.InitialMessageTimeout;
import fi.jumi.launcher.daemon.Steward;
import fi.jumi.launcher.process.*;
import org.apache.commons.io.IOUtils;

import javax.annotation.WillClose;
import javax.annotation.concurrent.*;
import java.io.*;
import java.nio.file.Paths;
import java.util.concurrent.*;

@NotThreadSafe
public class ProcessStartingDaemonSummoner implements DaemonSummoner {

    private static final DynamicEventizer<DaemonListener> eventizer = new DynamicEventizer<>(DaemonListener.class);

    private final Steward steward;
    private final ProcessStarter processStarter;
    private final NetworkServer daemonConnector;

    private final OutputStream outputListener; // TODO: remove me

    public ProcessStartingDaemonSummoner(Steward steward,
                                         ProcessStarter processStarter,
                                         NetworkServer daemonConnector,
                                         @WillClose OutputStream outputListener) {
        this.steward = steward;
        this.processStarter = processStarter;
        this.daemonConnector = daemonConnector;
        this.outputListener = outputListener;
    }

    @Override
    public void connectToDaemon(SuiteConfiguration suite,
                                DaemonConfiguration daemon,
                                ActorRef<DaemonListener> listener) {
        // XXX: should we handle multiple connections properly, even though we are expecting only one?
        int port = daemonConnector.listenOnAnyPort(
                new OneTimeDaemonListenerFactory(
                        withInitialMessageTimeout(listener.tell(), daemon.getStartupTimeout())));
        daemon = daemon.melt()
                .setDaemonDir(steward.createDaemonDir(daemon.getJumiHome()))
                .setLauncherPort(port)
                .freeze();

        try {
            JvmArgs jvmArgs = new JvmArgsBuilder()
                    .setExecutableJar(steward.getDaemonJar(daemon.getJumiHome()))
                    .setWorkingDir(Paths.get(suite.getWorkingDirectory()))
                    .setJvmOptions(suite.getJvmOptions())
                    .setSystemProperties(daemon.toSystemProperties())
                    .setProgramArgs(daemon.toProgramArgs())
                    .freeze();
            Process process = processStarter.startJavaProcess(jvmArgs);
            copyInBackground(process.getInputStream(), outputListener); // TODO: write the output to a log file using OS pipes, read it from there with AppRunner
        } catch (Exception e) {
            throw Boilerplate.rethrow(e);
        }
    }

    private static DaemonListener withInitialMessageTimeout(DaemonListener listener, long timeoutMillis) {
        return eventizer.newFrontend(
                new InitialMessageTimeout<>(
                        eventizer.newBackend(listener),
                        getTimeoutMessages(timeoutMillis),
                        timeoutMillis, TimeUnit.MILLISECONDS));
    }

    private static MessageReceiver<Event<DaemonListener>> getTimeoutMessages(long timeoutMillis) {
        MessageQueue<Event<DaemonListener>> timeoutMessages = new MessageQueue<>();
        DaemonListener listener = eventizer.newFrontend(timeoutMessages);
        listener.onMessage(new OnSuiteStartedEvent());
        listener.onMessage(new OnInternalErrorEvent("Failed to start the test runner daemon process",
                StackTrace.from(new RuntimeException("Could not connect to the daemon: timed out after " + timeoutMillis + " ms"))));
        listener.onMessage(new OnSuiteFinishedEvent());
        return timeoutMessages;
    }

    private static void copyInBackground(@WillClose InputStream src, @WillClose OutputStream dest) {
        // TODO: after removing me, update also ReleasingResourcesTest
        Thread t = new Thread(() -> {
            try {
                IOUtils.copy(src, dest);
            } catch (IOException e) {
                throw Boilerplate.rethrow(e);
            } finally {
                IOUtils.closeQuietly(src);
                IOUtils.closeQuietly(dest);
            }
        }, "Daemon Output Copier");
        t.setDaemon(true);
        t.start();
    }

    @ThreadSafe
    private static class OneTimeDaemonListenerFactory implements NetworkEndpointFactory<Event<SuiteListener>, Event<RequestListener>> {

        private final BlockingQueue<DaemonListener> oneTimeListener = new ArrayBlockingQueue<>(1);

        public OneTimeDaemonListenerFactory(DaemonListener listener) {
            this.oneTimeListener.add(listener);
        }

        @Override
        public NetworkEndpoint<Event<SuiteListener>, Event<RequestListener>> createEndpoint() {
            DaemonListener listener = oneTimeListener.poll();
            if (listener == null) {
                throw new IllegalStateException("already connected once");
            }
            return listener;
        }
    }
}
