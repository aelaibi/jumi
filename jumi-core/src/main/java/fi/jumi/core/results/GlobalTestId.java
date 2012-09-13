// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.results;

import fi.jumi.api.drivers.TestId;

import javax.annotation.concurrent.Immutable;

@Immutable
class GlobalTestId {

    private final String testClass;
    private final TestId testId;

    public GlobalTestId(String testClass, TestId testId) {
        assert testClass != null;
        assert testId != null;
        this.testClass = testClass;
        this.testId = testId;
    }

    @Override
    public boolean equals(Object other) {
        GlobalTestId that = (GlobalTestId) other;
        return this.testClass.equals(that.testClass) &&
                this.testId.equals(that.testId);
    }

    @Override
    public int hashCode() {
        int result = testClass.hashCode();
        result = 31 * result + testId.hashCode();
        return result;
    }
}
