// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.daemon;

import fi.jumi.actors.*;
import fi.jumi.actors.eventizers.ComposedEventizerProvider;
import fi.jumi.actors.listeners.*;
import fi.jumi.core.config.*;
import fi.jumi.core.events.*;
import fi.jumi.core.network.*;
import fi.jumi.core.output.*;
import fi.jumi.core.suite.SuiteFactory;
import fi.jumi.core.util.PrefixedThreadFactory;
import fi.jumi.daemon.timeout.*;

import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.nio.charset.Charset;
import java.util.concurrent.*;

@ThreadSafe
public class Main {

    private static final SystemExit SHUTDOWN_ON_STARTUP_TIMEOUT = new SystemExit("timed out before anybody connected");
    private static final SystemExit SHUTDOWN_ON_IDLE_TIMEOUT = new SystemExit("timed out after everybody disconnected");
    private static final SystemExit SHUTDOWN_ON_USER_COMMAND = new SystemExit("ordered to shut down");

    public static void main(String[] args) throws IOException {
        System.out.println("Jumi " + DaemonArtifact.getVersion() + " starting up");

        DaemonConfiguration config = new DaemonConfigurationBuilder()
                .parseProgramArgs(args)
                .parseSystemProperties(System.getProperties())
                .freeze();

        // timeouts for shutting down this daemon process
        Timeout startupTimeout = new CommandExecutingTimeout(
                SHUTDOWN_ON_STARTUP_TIMEOUT, config.startupTimeout(), TimeUnit.MILLISECONDS
        );
        startupTimeout.start();
        Timeout idleTimeout = new CommandExecutingTimeout(
                SHUTDOWN_ON_IDLE_TIMEOUT, config.idleTimeout(), TimeUnit.MILLISECONDS
        );

        // logging configuration
        PrintStream logOutput = System.out;
        FailureHandler failureHandler = new PrintStreamFailureLogger(logOutput);
        MessageListener messageListener = config.logActorMessages()
                ? new PrintStreamMessageLogger(logOutput)
                : new NullMessageListener();

        // replacing System.out/err with the output capturer
        OutputCapturer outputCapturer = new OutputCapturer(System.out, System.err, Charset.defaultCharset());
        new OutputCapturerInstaller(new SystemOutErr()).install(outputCapturer);

        // thread pool configuration
        Executor actorsThreadPool = // messages already logged by the Actors implementation
                Executors.newCachedThreadPool(new PrefixedThreadFactory("jumi-actors-"));
        // TODO: make the number of test threads by default the number of CPUs + 1 or similar
        Executor testsThreadPool = messageListener.getListenedExecutor(
                Executors.newFixedThreadPool(4, new PrefixedThreadFactory("jumi-tests-")));

        // actors configuration
        // TODO: not all of these eventizers might be needed - create a statistics gathering EventizerProvider
        MultiThreadedActors actors = new MultiThreadedActors(
                actorsThreadPool,
                new ComposedEventizerProvider(
                        new StartableEventizer(),
                        new RunnableEventizer(),
                        new WorkerListenerEventizer(),
                        new TestFileFinderListenerEventizer(),
                        new SuiteListenerEventizer(),
                        new CommandListenerEventizer(),
                        new RunListenerEventizer()
                ),
                failureHandler,
                messageListener
        );

        // bootstrap the system
        ActorThread actorThread = actors.startActorThread();
        SuiteFactory suiteFactory = new SuiteFactory(actorThread, outputCapturer, testsThreadPool);

        NetworkClient client = new NettyNetworkClient();
        client.connect("127.0.0.1", config.launcherPort(),
                new DaemonNetworkEndpoint(suiteFactory, SHUTDOWN_ON_USER_COMMAND, startupTimeout, idleTimeout));
    }
}
