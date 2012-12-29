// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.runners;

import fi.jumi.actors.ActorRef;
import fi.jumi.core.files.*;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class TestFileFinderRunner implements Runnable {

    private final TestFileFinder testFileFinder;
    private final ActorRef<TestFileFinderListener> finderListener;

    public TestFileFinderRunner(TestFileFinder testFileFinder, ActorRef<TestFileFinderListener> finderListener) {
        this.testFileFinder = testFileFinder;
        this.finderListener = finderListener;
    }

    @Override
    public void run() {
        testFileFinder.findTestFiles(finderListener);
    }
}
