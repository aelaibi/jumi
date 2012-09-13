// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.output;

import java.io.PrintStream;

public interface OutErr {

    PrintStream out();

    PrintStream err();

    void setOut(PrintStream out);

    void setErr(PrintStream err);
}
