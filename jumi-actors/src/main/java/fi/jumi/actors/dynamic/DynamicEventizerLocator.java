// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.actors.dynamic;

import fi.jumi.actors.eventizers.*;

import javax.annotation.concurrent.Immutable;

@Immutable
public class DynamicEventizerLocator implements EventizerLocator {

    @Override
    public <T> Eventizer<T> getEventizerForType(Class<T> type) {
        return new DynamicEventizer<T>(type);
    }
}
