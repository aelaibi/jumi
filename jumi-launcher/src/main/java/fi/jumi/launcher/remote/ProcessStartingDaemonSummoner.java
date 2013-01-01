// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.launcher.remote;

import fi.jumi.actors.ActorRef;
import fi.jumi.actors.eventizers.Event;
import fi.jumi.core.CommandListener;
import fi.jumi.core.api.SuiteListener;
import fi.jumi.core.config.*;
import fi.jumi.core.network.*;
import fi.jumi.launcher.daemon.Steward;
import fi.jumi.launcher.process.*;
import org.apache.commons.io.IOUtils;

import javax.annotation.concurrent.*;
import java.io.*;
import java.nio.file.Paths;
import java.util.concurrent.*;

@NotThreadSafe
public class ProcessStartingDaemonSummoner implements DaemonSummoner {

    private final Steward steward;
    private final ProcessStarter processStarter;
    private final NetworkServer daemonConnector;

    private final OutputStream outputListener; // TODO: remove me

    public ProcessStartingDaemonSummoner(Steward steward,
                                         ProcessStarter processStarter,
                                         NetworkServer daemonConnector,
                                         OutputStream outputListener) {
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
        int port = daemonConnector.listenOnAnyPort(new OneTimeDaemonListenerFactory(listener));
        daemon = daemon.melt()
                .launcherPort(port)
                .freeze();

        try {
            JvmArgs jvmArgs = new JvmArgsBuilder()
                    .executableJar(steward.getDaemonJar(daemon.jumiHome()))
                    .workingDir(Paths.get(suite.workingDirectory()))
                    .jvmOptions(suite.jvmOptions())
                    .systemProperties(daemon.toSystemProperties())
                    .programArgs(daemon.toProgramArgs())
                    .toJvmArgs();
            Process process = processStarter.startJavaProcess(jvmArgs);
            copyInBackground(process.getInputStream(), outputListener); // TODO: write the output to a log file using OS pipes, read it from there with AppRunner
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void copyInBackground(final InputStream src, final OutputStream dest) {
        @NotThreadSafe
        class Copier implements Runnable {
            @Override
            public void run() {
                try {
                    IOUtils.copy(src, dest);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    IOUtils.closeQuietly(src);
                    IOUtils.closeQuietly(dest);
                }
            }
        }
        Thread t = new Thread(new Copier(), "Daemon Output Copier");
        t.setDaemon(true);
        t.start();
    }

    @ThreadSafe
    private static class OneTimeDaemonListenerFactory implements NetworkEndpointFactory<Event<SuiteListener>, Event<CommandListener>> {

        private final BlockingQueue<ActorRef<DaemonListener>> oneTimeListener = new ArrayBlockingQueue<>(1);

        public OneTimeDaemonListenerFactory(ActorRef<DaemonListener> listener) {
            this.oneTimeListener.add(listener);
        }

        @Override
        public NetworkEndpoint<Event<SuiteListener>, Event<CommandListener>> createEndpoint() {
            ActorRef<DaemonListener> listener = oneTimeListener.poll();
            if (listener == null) {
                throw new IllegalStateException("already connected once");
            }
            return listener.tell();
        }
    }
}
