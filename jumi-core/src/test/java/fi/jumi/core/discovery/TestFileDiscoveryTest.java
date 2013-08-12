// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.discovery;

import fi.jumi.core.config.SuiteConfigurationBuilder;
import fi.jumi.core.suite.SuiteFactory;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

public class TestFileDiscoveryTest {

    @Rule
    public final TemporaryFolder tempDir = new TemporaryFolder();

    @Test
    public void looks_for_tests_from_directories_on_classpath() throws IOException {
        Path libraryJar = tempDir.newFile("library.jar").toPath();
        Path folder1 = tempDir.newFolder("folder1").toPath();
        Path folder2 = tempDir.newFolder("folder2").toPath();

        SuiteConfigurationBuilder suite = new SuiteConfigurationBuilder()
                .setIncludedTestsPattern("glob:the pattern")
                .addToClasspath(libraryJar)
                .addToClasspath(folder1)
                .addToClasspath(folder2);

        List<Path> classesDirectories = SuiteFactory.getClassDirectories(suite.freeze());
        assertThat(classesDirectories, contains(folder1, folder2));
    }
}
