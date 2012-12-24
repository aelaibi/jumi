// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
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
        new JumiBootstrap().runTestClasses(args);
    }

    private Appendable out = System.out;
    private OutputStream debugOutput = null;

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
        SuiteConfigurationBuilder suite = new SuiteConfigurationBuilder()
                .addJvmOptions("-ea")
                .testClasses(testClasses);

        for (Path path : currentClasspath()) {
            suite.addToClassPath(path);
        }

        runSuite(suite.freeze());
    }

    public static List<Path> currentClasspath() {
        Path javaHome = Paths.get(System.getProperty("java.home"));

        List<Path> classpath = new ArrayList<>();
        String pathSeparator = System.getProperty("path.separator");
        for (String library : System.getProperty("java.class.path").split(Pattern.quote(pathSeparator))) {
            Path libraryPath = Paths.get(library);
            if (!libraryPath.startsWith(javaHome)) {
                classpath.add(libraryPath);
            }
        }
        return classpath;
    }

    public void runSuite(SuiteConfiguration suite) throws IOException, InterruptedException {
        DaemonConfiguration daemon = new DaemonConfigurationBuilder()
                .logActorMessages(debugOutput != null)
                .freeze();

        try (JumiLauncher launcher = createLauncher()) {
            launcher.start(suite, daemon);

            TextUI ui = new TextUI(launcher.getEventStream(), new PlainTextPrinter(out));
            ui.updateUntilFinished();

            launcher.close();
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
