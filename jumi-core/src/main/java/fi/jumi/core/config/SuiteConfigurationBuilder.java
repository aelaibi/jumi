// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.config;

import javax.annotation.concurrent.NotThreadSafe;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;

@NotThreadSafe
public class SuiteConfigurationBuilder {

    private final List<URI> classPath;
    private final List<String> jvmOptions;
    private final List<String> testClasses;

    public SuiteConfigurationBuilder() {
        this(SuiteConfiguration.DEFAULTS);
    }

    SuiteConfigurationBuilder(SuiteConfiguration src) {
        classPath = new ArrayList<>(src.classPath());
        jvmOptions = new ArrayList<>(src.jvmOptions());
        testClasses = new ArrayList<>(src.testClasses());
    }

    public SuiteConfiguration freeze() {
        return new SuiteConfiguration(this);
    }


    // getters and setters

    public List<URI> classPath() {
        return classPath;
    }

    public SuiteConfigurationBuilder addToClassPath(Path file) {
        return addToClassPath(file.toUri());
    }

    public SuiteConfigurationBuilder addToClassPath(URI file) {
        classPath.add(file);
        return this;
    }

    public List<String> testClasses() {
        return testClasses;
    }

    public SuiteConfigurationBuilder testClasses(String... testClasses) {
        return testClasses(Arrays.asList(testClasses));
    }

    public SuiteConfigurationBuilder testClasses(List<String> testClasses) {
        this.testClasses.clear();
        this.testClasses.addAll(testClasses);
        return this;
    }

    public List<String> jvmOptions() {
        return jvmOptions;
    }

    public SuiteConfigurationBuilder addJvmOptions(String... jvmOptions) {
        this.jvmOptions.addAll(Arrays.asList(jvmOptions));
        return this;
    }
}
