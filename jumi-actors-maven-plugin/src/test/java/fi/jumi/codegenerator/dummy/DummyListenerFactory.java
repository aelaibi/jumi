package fi.jumi.codegenerator.dummy;

import fi.jumi.actors.Event;
import fi.jumi.actors.ListenerFactory;
import fi.jumi.actors.MessageSender;
import fi.jumi.codegenerator.DummyListener;

public class DummyListenerFactory implements ListenerFactory<DummyListener> {

    public Class<DummyListener> getType() {
        return DummyListener.class;
    }

    public DummyListener newFrontend(MessageSender<Event<DummyListener>> target) {
        return new DummyListenerToEvent(target);
    }

    public MessageSender<Event<DummyListener>> newBackend(DummyListener target) {
        return new EventToDummyListener(target);
    }
}
