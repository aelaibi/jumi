// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.files;

import fi.jumi.actors.ActorRef;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.nio.file.*;

@NotThreadSafe
public class FileNamePatternTestClassFinder implements TestClassFinder {

    private final PathMatcher matcher;
    private final Path baseDir;

    public FileNamePatternTestClassFinder(PathMatcher matcher, Path baseDir) {
        this.matcher = matcher;
        this.baseDir = baseDir;
    }

    @Override
    public void findTestClasses(final ActorRef<TestClassFinderListener> listener) {
        @NotThreadSafe
        class ClassFindingFileVisitor extends RelativePathMatchingFileVisitor {
            public ClassFindingFileVisitor(PathMatcher matcher, Path baseDir) {
                super(matcher, baseDir);
            }

            @Override
            protected void fileFound(Path relativePath) {
                listener.tell().onTestClassFound(relativePath);
            }
        }

        try {
            Files.walkFileTree(baseDir, new ClassFindingFileVisitor(matcher, baseDir));
        } catch (IOException e) {
            throw new RuntimeException("Failed to traverse " + baseDir, e);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + matcher + ", " + baseDir + ")";
    }
}
