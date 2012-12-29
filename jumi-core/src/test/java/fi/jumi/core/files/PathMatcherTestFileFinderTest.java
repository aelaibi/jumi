// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.files;

import fi.jumi.actors.ActorRef;
import fi.jumi.core.files.dummies.DummyTest;
import org.junit.Test;

import java.net.*;
import java.nio.file.*;

import static org.mockito.Mockito.*;

public class PathMatcherTestFileFinderTest {

    private final Path classesDirectory = getClassesDirectory(DummyTest.class);
    private final TestFileFinderListener listener = mock(TestFileFinderListener.class);

    @Test
    public void finds_all_classes_from_a_directory_matching_a_pattern() {
        PathMatcher matcher = classesDirectory.getFileSystem().getPathMatcher("glob:fi/jumi/core/files/dummies/*Test.class");
        PathMatcherTestFileFinder finder = new PathMatcherTestFileFinder(matcher, classesDirectory);

        finder.findTestFiles(ActorRef.wrap(listener));

        verify(listener).onTestFileFound(Paths.get("fi/jumi/core/files/dummies/DummyTest.class"));
        verify(listener).onTestFileFound(Paths.get("fi/jumi/core/files/dummies/AnotherDummyTest.class"));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void does_nothing_when_finds_no_matches() {
        PathMatcher matcher = classesDirectory.getFileSystem().getPathMatcher("glob:NoMatches");
        PathMatcherTestFileFinder finder = new PathMatcherTestFileFinder(matcher, classesDirectory);

        finder.findTestFiles(ActorRef.wrap(listener));

        verifyNoMoreInteractions(listener);
    }


    private static Path getClassesDirectory(Class<?> clazz) {
        try {
            URL classFile = clazz.getResource(clazz.getSimpleName() + ".class");
            Path path = Paths.get(classFile.toURI());

            int directoryDepth = clazz.getName().split("\\.").length;
            for (int i = 0; i < directoryDepth; i++) {
                path = path.getParent();
            }
            return path;

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
