package event;


import tag.Tag;

import java.util.List;

public interface Event {


    List<Event> findEventsWithoutTag(Tag tag);

    void tagEvent();

}