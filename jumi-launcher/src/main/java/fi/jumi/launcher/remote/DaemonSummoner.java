// Copyright © 2011-2015, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.launcher.remote;

import fi.jumi.actors.ActorRef;
import fi.jumi.actors.generator.GenerateEventizer;
import fi.jumi.core.config.*;

@GenerateEventizer(targetPackage = "fi.jumi.launcher.events")
public interface DaemonSummoner {

    void connectToDaemon(SuiteConfiguration suiteConfiguration,
                         DaemonConfiguration daemonConfiguration,
                         ActorRef<DaemonListener> listener);
}
