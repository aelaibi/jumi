// Copyright © 2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.codegenerator;

public interface MyListenerFactory<T> {

    Class<T> getType();

    T newFrontend(MyMessageSender<MyEvent<T>> target);

    MyMessageSender<MyEvent<T>> newBackend(T target);
}
