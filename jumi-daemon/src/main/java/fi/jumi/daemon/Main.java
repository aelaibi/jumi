// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.daemon;

import fi.jumi.actors.*;
import fi.jumi.actors.eventizers.ComposedEventizerProvider;
import fi.jumi.actors.listeners.*;
import fi.jumi.core.*;
import fi.jumi.core.events.*;
import fi.jumi.core.util.PrefixedThreadFactory;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.*;

import javax.annotation.concurrent.*;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.concurrent.*;

@ThreadSafe
public class Main {

    public static void main(String[] args) {
        exitWhenNotAnymoreInUse();

        // program parameters
        int launcherPort = Integer.parseInt(args[0]);

        // logging configuration
        PrintStream logOutput = System.out;
        FailureHandler failureHandler = new PrintStreamFailureHandler(logOutput);
        MessageListener messageListener = SystemProperties.logActorMessages()
                ? new PrintStreamMessageLogger(logOutput)
                : new NullMessageListener();

        // thread pool configuration
        Executor actorsThreadPool = // messages already logged by the Actors implementation
                Executors.newCachedThreadPool(new PrefixedThreadFactory("jumi-actors-"));
        // TODO: do not create unlimited numbers of threads; make it by default CPUs+1 or something
        Executor testsThreadPool = messageListener.getListenedExecutor(
                Executors.newCachedThreadPool(new PrefixedThreadFactory("jumi-tests-")));

        // actors configuration
        MultiThreadedActors actors = new MultiThreadedActors(
                actorsThreadPool,
                new ComposedEventizerProvider(
                        new StartableEventizer(),
                        new RunnableEventizer(),
                        new TestClassFinderListenerEventizer(),
                        new SuiteListenerEventizer(),
                        new CommandListenerEventizer(),
                        new TestClassListenerEventizer()
                ),
                failureHandler,
                messageListener
        );

        // bootstrap the system
        ActorThread actorThread = actors.startActorThread();
        ActorRef<CommandListener> coordinator =
                actorThread.bindActor(CommandListener.class, new TestRunCoordinator(actorThread, testsThreadPool));
        connectToLauncher(launcherPort, coordinator);
    }

    private static void connectToLauncher(int launcherPort, final ActorRef<CommandListener> coordinator) {
        ChannelFactory factory = new OioClientSocketChannelFactory(Executors.newCachedThreadPool());
        ClientBootstrap bootstrap = new ClientBootstrap(factory);

        @ThreadSafe
        class MyChannelPipelineFactory implements ChannelPipelineFactory {
            @Override
            public ChannelPipeline getPipeline() {
                return Channels.pipeline(
                        new ObjectEncoder(),
                        new ObjectDecoder(ClassResolvers.softCachingResolver(Main.class.getClassLoader())),
                        //new LoggingHandler(InternalLogLevel.INFO), // TODO: remove this debug code
                        new JumiDaemonHandler(coordinator));
            }
        }
        bootstrap.setPipelineFactory(new MyChannelPipelineFactory());

        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);

        bootstrap.connect(new InetSocketAddress("localhost", launcherPort));
    }

    private static void exitWhenNotAnymoreInUse() {
        // TODO: implement timeouts etc. which will automatically close down the daemon once the launcher is no more

        @Immutable
        class DelayedSystemExit implements Runnable {
            @Override
            public void run() {
                int delayMillis = 2 * 1000;
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(getClass().getName()
                        + ": the system has been running " + delayMillis + " ms; exiting the JVM now");
                System.exit(0);
            }
        }
        Thread t = new Thread(new DelayedSystemExit());
        t.setDaemon(true);
        t.start();
    }
}
