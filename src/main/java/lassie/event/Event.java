package lassie.event;


import lassie.tag.Tag;

import java.util.List;

public interface Event {


    List<Event> findEventsWithoutTag(Tag tag);

    long getLaunchTime();

    String getId();

    List<Tag> getTags();

    String getOwnerId();

    void tagEvent();

    String getName();

    String getArnJsonPath();

}
