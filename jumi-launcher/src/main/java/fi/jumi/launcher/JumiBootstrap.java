// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.launcher;

import fi.jumi.core.config.*;
import fi.jumi.launcher.ui.*;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

@NotThreadSafe
public class JumiBootstrap {

    public static void main(String[] args) throws Exception {
        try {
            new JumiBootstrap().runTestClasses(args);
        } catch (AssertionError e) {
            System.exit(1);
        }
    }

    private int testThreadsCount = 0;
    private boolean passingTestsVisible = false;
    private Appendable out = System.out;
    private OutputStream debugOutput = null;

    public JumiBootstrap setTestThreadsCount(int testThreadsCount) {
        this.testThreadsCount = testThreadsCount;
        return this;
    }

    public JumiBootstrap setPassingTestsVisible(boolean passingTestsVisible) {
        this.passingTestsVisible = passingTestsVisible;
        return this;
    }

    public JumiBootstrap setOut(Appendable out) {
        this.out = out;
        return this;
    }

    public JumiBootstrap enableDebugMode() {
        return enableDebugMode(System.err);
    }

    public JumiBootstrap enableDebugMode(OutputStream debugOutput) {
        this.debugOutput = debugOutput;
        return this;
    }

    public void runTestsMatchingDefaultPattern() throws IOException, InterruptedException {
        runSuite(commonConfiguration()
                .freeze());
    }

    /**
     * @param syntaxAndPattern Same format as in {@link java.nio.file.FileSystem#getPathMatcher(String)}
     */
    public void runTestsMatching(String syntaxAndPattern) throws IOException, InterruptedException {
        runSuite(commonConfiguration()
                .setIncludedTestsPattern(syntaxAndPattern)
                .freeze());
    }

    public void runTestClasses(Class<?>... testClasses) throws IOException, InterruptedException {
        runTestClasses(toClassNames(testClasses));
    }

    private static String[] toClassNames(Class<?>[] classes) {
        String[] names = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            names[i] = classes[i].getName();
        }
        return names;
    }

    public void runTestClasses(String... testClasses) throws IOException, InterruptedException {
        runSuite(commonConfiguration()
                .setTestClasses(testClasses)
                .freeze());
    }

    private static SuiteConfigurationBuilder commonConfiguration() {
        return new SuiteConfigurationBuilder()
                .addJvmOptions("-ea")
                .setClassPath(currentClasspath());
    }

    public static Path[] currentClasspath() {
        Path javaHome = Paths.get(System.getProperty("java.home"));

        List<Path> classpath = new ArrayList<>();
        String pathSeparator = System.getProperty("path.separator");
        for (String library : System.getProperty("java.class.path").split(Pattern.quote(pathSeparator))) {
            Path libraryPath = Paths.get(library);
            if (!libraryPath.startsWith(javaHome)) {
                classpath.add(libraryPath);
            }
        }
        return classpath.toArray(new Path[classpath.size()]);
    }

    public void runSuite(SuiteConfiguration suite) throws IOException, InterruptedException {
        DaemonConfiguration daemon = new DaemonConfigurationBuilder()
                .setTestThreadsCount(testThreadsCount)
                .setLogActorMessages(debugOutput != null)
                .freeze();

        try (JumiLauncher launcher = createLauncher()) {
            launcher.start(suite, daemon);

            TextUI ui = new TextUI(launcher.getEventStream(), new PlainTextPrinter(out));
            ui.setPassingTestsVisible(passingTestsVisible);
            ui.updateUntilFinished();

            if (ui.hasFailures()) {
                throw new AssertionError("There were test failures");
            }
        }
    }

    private JumiLauncher createLauncher() {
        @NotThreadSafe
        class MyJumiLauncherBuilder extends JumiLauncherBuilder {
            @Override
            protected OutputStream createDaemonOutputListener() {
                if (debugOutput != null) {
                    return debugOutput;
                } else {
                    return super.createDaemonOutputListener();
                }
            }
        }
        return new MyJumiLauncherBuilder().build();
    }
}
