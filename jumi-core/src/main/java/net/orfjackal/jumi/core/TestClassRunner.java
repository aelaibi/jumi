// Copyright © 2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.jumi.core;

import net.orfjackal.jumi.api.drivers.*;
import net.orfjackal.jumi.core.actors.Actors;

import java.util.concurrent.*;

public class TestClassRunner implements Runnable, WorkersListener {

    private final Class<?> testClass;
    private final Class<? extends Driver> driverClass;
    private final SuiteListener listener;
    private final Executor executor;
    private final Actors actors;
    private WorkerCountingExecutor workers;

    public TestClassRunner(Class<?> testClass,
                           Class<? extends Driver> driverClass,
                           SuiteListener listener,
                           ExecutorService executor,
                           Actors actors) {
        this.testClass = testClass;
        this.driverClass = driverClass;
        this.listener = listener;
        this.executor = executor;
        this.actors = actors;
    }

    public void run() {
        listener.onTestClassStarted(testClass);

        WorkersListener workerListener = actors.bindSecondaryInterface(WorkersListener.class, this);
        workers = new WorkerCountingExecutor(workerListener, actors, executor);

        SuiteNotifier notifier = new TestClassState(listener, testClass).getSuiteNotifier();
        workers.execute(new DriverRunner(notifier));
    }

    public void onAllWorkersFinished() {
        listener.onTestClassFinished(testClass);
    }


    private class DriverRunner implements Runnable {
        private final SuiteNotifier suiteNotifier;

        public DriverRunner(SuiteNotifier suiteNotifier) {
            this.suiteNotifier = suiteNotifier;
        }

        public void run() {
            // TODO: pass an executor which keeps a count of how many workers there are (WorkerCountingExecutor?)
            newDriverInstance().findTests(testClass, suiteNotifier, null);
        }

        private Driver newDriverInstance() {
            try {
                return driverClass.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
