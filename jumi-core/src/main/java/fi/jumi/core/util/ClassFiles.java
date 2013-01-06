// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.util;

import javax.annotation.concurrent.Immutable;

@Immutable
public class ClassFiles {

    public static String classNameToPath(String className) {
        return className.replace('.', '/') + ".class";
    }

    public static String pathToClassName(String path) {
        return path.substring(0, path.lastIndexOf(".class")).replace('/', '.');
    }
}
