// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.test;

import fi.jumi.launcher.JumiLauncher;
import org.junit.*;

import static fi.jumi.core.util.AsyncAssert.assertEventually;
import static fi.jumi.test.util.ProcessMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DaemonProcessTest {

    private static final int TIMEOUT = 2000;

    @Rule
    public final AppRunner app = new AppRunner();

    private JumiLauncher launcher;
    private Process daemonProcess;

    @Test(timeout = TIMEOUT)
    public void daemon_process_can_be_closed_by_sending_it_a_shutdown_command() throws Exception {
        startDaemonProcess();

        launcher.shutdownDaemon();

        assertEventually(daemonProcess, is(dead()), TIMEOUT / 2);
        // TODO: assert on the message that the daemon prints on shutdown?
    }

    @Ignore("not implemented")
    @Test(timeout = TIMEOUT)
    public void daemon_process_will_exit_after_a_timeout_after_all_clients_disconnect() throws Exception {
        startDaemonProcess();

        launcher.close();

        assertEventually(daemonProcess, is(dead()), TIMEOUT / 2);
    }

    private void startDaemonProcess() throws Exception {
        app.runTests("unimportant");
        daemonProcess = app.getDaemonProcess();
        launcher = app.getLauncher();
        assertThat(daemonProcess, is(alive()));
    }
}
