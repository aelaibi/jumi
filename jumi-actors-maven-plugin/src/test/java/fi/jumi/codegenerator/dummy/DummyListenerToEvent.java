package fi.jumi.codegenerator.dummy;

import fi.jumi.actors.Event;
import fi.jumi.actors.MessageSender;
import fi.jumi.codegenerator.DummyListener;

public class DummyListenerToEvent implements DummyListener {

    private final MessageSender<Event<DummyListener>> sender;

    public DummyListenerToEvent(MessageSender<Event<DummyListener>> sender) {
        this.sender = sender;
    }

    public void onOther() {
        sender.send(new OnOtherEvent());
    }

    public void onSomething(String arg0, String arg1) {
        sender.send(new OnSomethingEvent(arg0, arg1));
    }
}
