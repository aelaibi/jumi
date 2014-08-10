// Copyright © 2011-2014, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.ipc.dirs;

import fi.jumi.core.Timeouts;
import fi.jumi.core.ipc.*;
import fi.jumi.core.util.TestingExecutor;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matcher;
import org.junit.*;
import org.junit.rules.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import static java.nio.file.StandardWatchEventKinds.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class DirectoryObserverTest {

    @Rule
    public final TemporaryFolder tempDir = new TemporaryFolder();

    @Rule
    public final Timeout timeout = Timeouts.forUnitTest();

    @Rule
    public final TestingExecutor executor = new TestingExecutor();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final BlockingQueue<Path> noticedFiles = new LinkedBlockingQueue<>();
    private Path directory;
    private DirectoryObserver observer;
    private Future<?> observerFuture;

    @Before
    public void setup() {
        directory = tempDir.getRoot().toPath();
        observer = new DirectoryObserver(directory, noticedFiles::add);
    }

    @Test
    public void notices_all_existing_files() throws Exception {
        tempDir.newFile("existing1");
        tempDir.newFile("existing2");

        startObserverAsynchronously();

        assertThat(takeAtLeast(2, noticedFiles), containsFiles("existing1", "existing2"));
    }

    @Test
    public void notices_new_files_as_they_are_created() throws Exception {
        startObserver();

        tempDir.newFile("created1");
        tempDir.newFile("created2");

        assertThat(takeAtLeast(2, noticedFiles), containsFiles("created1", "created2"));
    }

    @Test
    public void notices_new_files_even_when_the_notification_event_is_missed_due_to_an_overflow() throws Exception {
        FakeWatchService watchService = new FakeWatchService();
        observer = new DirectoryObserver(directory, noticedFiles::add) {
            @Override
            protected WatchService watchDirectory(Path directory, WatchEvent.Kind<?>... events) {
                return watchService;
            }
        };
        startObserver();
        tempDir.newFile("created1");
        tempDir.newFile("created2");

        watchService.publish(
                new FakeWatchKey()
                        .addEvent(ENTRY_CREATE, Paths.get("created1"))
                        .addEvent(OVERFLOW));

        assertThat(takeAtLeast(2, noticedFiles), containsFiles("created1", "created2"));
    }

    @Test
    public void stops_if_the_directory_becomes_inaccessible() throws Exception {
        startObserver();

        FileUtils.deleteDirectory(directory.toFile());

        observerFuture.get(); // should not timeout
    }

    @Test
    public void requires_the_directory_to_exist_before_observing_it() {
        Path noSuchDirectory = directory.resolve("no-such-directory");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Does not exist: " + noSuchDirectory);
        new DirectoryObserver(noSuchDirectory, noticedFiles::add);
    }

    @Test
    public void requires_the_path_to_be_a_directory() throws IOException {
        Path regularFile = tempDir.newFile("regular-file").toPath();

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Not a directory: " + regularFile);
        new DirectoryObserver(regularFile, noticedFiles::add);
    }


    // helpers

    private void startObserver() throws Exception {
        tempDir.newFile("existing");
        startObserverAsynchronously();
        takeAtLeast(1, noticedFiles); // wait for observer to start, skip existing files
    }

    private void startObserverAsynchronously() {
        observerFuture = executor.submit(observer);
    }

    private static List<Path> takeAtLeast(int count, BlockingQueue<Path> src) throws InterruptedException {
        List<Path> taken = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            taken.add(src.take());
        }
        Path p;
        while ((p = src.poll()) != null) {
            taken.add(p);
        }
        return taken;
    }

    private Matcher<Iterable<? extends Path>> containsFiles(String... filenames) {
        Path[] expected = new Path[filenames.length];
        for (int i = 0; i < filenames.length; i++) {
            expected[i] = directory.resolve(filenames[i]);
        }
        return containsInAnyOrder(expected);
    }
}
