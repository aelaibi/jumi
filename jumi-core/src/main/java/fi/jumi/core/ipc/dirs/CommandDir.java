// Copyright © 2011-2014, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.ipc.dirs;

import fi.jumi.core.util.Boilerplate;

import javax.annotation.concurrent.Immutable;
import java.nio.file.Path;

@Immutable
public final class CommandDir {

    private final Path path;

    public CommandDir(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public Path getRequestPath() {
        return path.resolve("request");
    }

    public Path getResponsePath() {
        return path.resolve("response");
    }

    @Override
    public String toString() {
        return Boilerplate.toString(getClass(), path);
    }
}
