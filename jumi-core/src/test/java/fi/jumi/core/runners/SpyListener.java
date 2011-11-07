// Copyright © 2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.runners;

import fi.jumi.api.drivers.TestId;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;

public class SpyListener implements TestClassRunnerListener {

    // TODO: refactor this into a generic class, using dynamic proxies

    static final String ERROR_MARKER = "     ^^^^^^^^^^^^^^";

    private final List<Call> expectations = new ArrayList<Call>();
    private final List<Call> actualCalls = new ArrayList<Call>();
    private List<Call> current = expectations;

    public void onTestFound(TestId id, String name) {
        current.add(new Call("onTestFound", id, name));
    }

    public void onTestStarted(TestId id) {
        current.add(new Call("onTestStarted", id));
    }

    public void onFailure(TestId id, Throwable cause) {
        current.add(new Call("onFailure", id, cause));
    }

    public void onTestFinished(TestId id) {
        current.add(new Call("onTestFinished", id));
    }

    public void onTestClassFinished() {
        current.add(new Call("onTestClassFinished"));
    }

    public void replay() {
        if (current != expectations) {
            throw new IllegalStateException("replay() has already been called");
        }
        current = actualCalls;
    }

    public void verify() {
        String message = "not all expectations were met\n";

        message += "Expected:\n";
        for (int i = 0; i < expectations.size(); i++) {
            message += listItem(i, expectations);
            if (!matchesAt(i)) {
                message += ERROR_MARKER + "\n";
            }
        }

        message += "but was:\n";
        for (int i = 0; i < actualCalls.size(); i++) {
            message += listItem(i, actualCalls);
            if (!matchesAt(i)) {
                message += ERROR_MARKER + "\n";
            }
        }

        assertThat(message, actualCalls.equals(expectations));
    }

    private boolean matchesAt(int i) {
        return i < actualCalls.size() &&
                i < expectations.size() &&
                expectations.get(i).equals(actualCalls.get(i));
    }

    private static String listItem(int i, List<Call> list) {
        return "  " + (i + 1) + ". " + list.get(i) + "\n";
    }

    private class Call {
        private final String methodName;
        private final Object[] args;

        public Call(String methodName, Object... args) {
            this.methodName = methodName;
            this.args = args;
        }

        public String toString() {
            String args = Arrays.toString(this.args);
            return methodName + "(" + args.substring(1, args.length() - 1) + ")";
        }

        public boolean equals(Object obj) {
            Call that = (Call) obj;
            return this.methodName.equals(that.methodName) && argsMatch(that);
        }

        private boolean argsMatch(Call that) {
            if (this.args.length != that.args.length) {
                return false;
            }
            for (int i = 0; i < this.args.length; i++) {
                Object arg1 = this.args[i];
                Object arg2 = that.args[i];
                if (arg1 instanceof Throwable) {
                    if (!sameTypeAndMessage((Throwable) arg1, (Throwable) arg2)) {
                        return false;
                    }
                } else if (!arg1.equals(arg2)) {
                    return false;
                }
            }
            return true;
        }

        private boolean sameTypeAndMessage(Throwable t1, Throwable t2) {
            return t1.getClass().equals(t2.getClass()) &&
                    t1.getMessage().equals(t2.getMessage());
        }
    }
}
