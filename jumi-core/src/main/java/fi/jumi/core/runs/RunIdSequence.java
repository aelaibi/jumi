// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.runs;

import fi.jumi.core.api.RunId;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.atomic.AtomicInteger;

@ThreadSafe
public class RunIdSequence {

    private final AtomicInteger nextId = new AtomicInteger(RunId.FIRST_ID);

    public RunId nextRunId() {
        int currentId = nextId.getAndIncrement();
        return new RunId(currentId);
    }
}
