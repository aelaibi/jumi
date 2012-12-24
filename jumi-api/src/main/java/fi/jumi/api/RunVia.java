// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.api;

import fi.jumi.api.drivers.Driver;

import java.lang.annotation.*;

/**
 * Marks a class as a test and tells that which {@link Driver} to use for running the test.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface RunVia {

    Class<? extends Driver> value();
}
