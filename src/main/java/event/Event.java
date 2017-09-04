package event;


import tag.Tag;

import java.util.List;

public interface Event {


    List<Event> findEventsWithoutTag(Tag tag);

    long getLaunchTime();

    String getInstanceId();

    List<Tag> getTags();

    String getOwnerId();

    void tagEvent();


}
