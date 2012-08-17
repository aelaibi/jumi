// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.daemon.timeout;

public class SpyTimeout implements Timeout {

    public boolean willTimeOut = false;

    @Override
    public void start() {
        willTimeOut = true;
    }

    @Override
    public void cancel() {
        willTimeOut = false;
    }
}
