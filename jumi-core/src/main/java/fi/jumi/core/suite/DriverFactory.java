// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.suite;

import fi.jumi.actors.ActorThread;
import fi.jumi.api.drivers.*;
import fi.jumi.core.api.SuiteListener;
import fi.jumi.core.drivers.*;
import fi.jumi.core.output.OutputCapturer;
import fi.jumi.core.runs.*;
import fi.jumi.core.util.ClassFiles;

import javax.annotation.concurrent.NotThreadSafe;
import java.nio.file.Path;
import java.util.concurrent.Executor;

@NotThreadSafe
public class DriverFactory {

    private final ActorThread actorThread;
    private final OutputCapturer outputCapturer;
    private final DriverFinder driverFinder;
    private final RunIdSequence runIdSequence;
    private final SuiteListener suiteListener;
    private final ClassLoader testClassLoader;

    public DriverFactory(ActorThread actorThread, OutputCapturer outputCapturer, DriverFinder driverFinder, RunIdSequence runIdSequence, SuiteListener suiteListener, ClassLoader testClassLoader) {
        this.actorThread = actorThread;
        this.outputCapturer = outputCapturer;
        this.driverFinder = driverFinder;
        this.runIdSequence = runIdSequence;
        this.suiteListener = suiteListener;
        this.testClassLoader = testClassLoader;
    }

    public DriverRunner createDriverRunner(Path testFile, Executor testExecutor) {
        Class<?> testClass = loadTestClass(testClassLoader, testFile);
        Driver driver = driverFinder.findTestClassDriver(testClass);

        SuiteNotifier suiteNotifier = new DefaultSuiteNotifier(
                actorThread.bindActor(RunListener.class,
                        new DuplicateOnTestFoundEventFilter(
                                new SuiteListenerAdapter(suiteListener, testClass))),
                runIdSequence,
                outputCapturer
        );

        return new DriverRunner(driver, testClass, suiteNotifier, testExecutor);
    }

    private static Class<?> loadTestClass(ClassLoader testClassLoader, Path testFile) {
        try {
            return testClassLoader.loadClass(ClassFiles.pathToClassName(testFile));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot load class from " + testFile, e);
        }
    }
}
