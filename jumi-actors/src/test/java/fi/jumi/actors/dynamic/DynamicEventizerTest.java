// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.actors.dynamic;

import fi.jumi.actors.Event;
import fi.jumi.actors.mq.*;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.io.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class DynamicEventizerTest {

    private final DynamicEventizer<DummyListener> eventizer = new DynamicEventizer<DummyListener>(DummyListener.class);
    private final MessageQueue<Event<DummyListener>> queue = new MessageQueue<Event<DummyListener>>();
    private final DummyListener frontend = eventizer.newFrontend(queue);

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void method_calls_are_converted_into_event_objects() {
        frontend.onSomething("param");

        assertThat(queue.poll(), is(notNullValue()));
    }

    @Test
    public void event_objects_are_converted_back_into_method_calls() {
        DummyListener target = mock(DummyListener.class);
        MessageSender<Event<DummyListener>> backend = eventizer.newBackend(target);

        frontend.onSomething("param");
        Event<DummyListener> event = queue.poll();
        backend.send(event);

        verify(target).onSomething("param");
    }

    @Test
    public void event_objects_are_serializable() throws Exception {
        DummyListener target = mock(DummyListener.class);
        MessageSender<Event<DummyListener>> backend = eventizer.newBackend(target);

        frontend.onSomething("param");
        Event<DummyListener> original = queue.poll();
        Event<DummyListener> deserialized = deserialize(serialize(original));

        backend.send(deserialized);
        verify(target).onSomething("param");
    }

    @Test
    public void the_events_have_descriptive_toString_methods() {
        frontend.onSomething("param");
        assertThat(queue.poll().toString(), is("DummyListener.onSomething(param)"));

        frontend.multipleArguments(1, 2);
        assertThat(queue.poll().toString(), is("DummyListener.multipleArguments(1, 2)"));

        frontend.zeroArguments();
        assertThat(queue.poll().toString(), is("DummyListener.zeroArguments()"));
    }

    @Test
    public void listeners_must_contain_only_void_methods() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("listeners may contain only void methods, but onSomething had return type java.lang.String");

        new DynamicEventizer<ListenerWithNonVoidMethods>(ListenerWithNonVoidMethods.class);
    }

    @Test
    public void listeners_must_be_interfaces() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("listeners must be interfaces, but got class");

        new DynamicEventizer<ListenerWhichIsNotAnInterface>(ListenerWhichIsNotAnInterface.class);
    }


    // helper methods

    private static ByteArrayOutputStream serialize(Event<DummyListener> original) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(buffer);
        out.writeObject(original);
        out.close();
        return buffer;
    }

    @SuppressWarnings({"unchecked"})
    private static Event<DummyListener> deserialize(ByteArrayOutputStream buffer) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
        return (Event<DummyListener>) in.readObject();
    }

    // test data

    private interface DummyListener {
        void onSomething(String parameter);

        void multipleArguments(int one, int two);

        void zeroArguments();
    }

    private interface ListenerWithNonVoidMethods {
        @SuppressWarnings({"UnusedDeclaration"})
        String onSomething(String parameter);
    }

    private static abstract class ListenerWhichIsNotAnInterface {
    }
}