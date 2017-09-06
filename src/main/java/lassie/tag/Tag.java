package lassie.tag;

import lassie.event.Event;

public interface Tag {

    void tagEvent(Event event);

    String getName();

}