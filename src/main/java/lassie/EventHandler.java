package lassie;

import lassie.config.EventConfig;
import lassie.event.Event;
import lassie.exception.UnsupportedEventException;
import lassie.exception.UnsupportedTagException;
import lassie.tag.Tag;

import java.util.ArrayList;
import java.util.List;

public class EventHandler {
    private EventInterpreter eventInterpreter;
    private List<Event> untaggedEvents;
    private List<EventConfig> configEvents;

    public EventHandler(EventInterpreter eventInterpreter, List<EventConfig> configEvents) {
        this.eventInterpreter = eventInterpreter;
        this.configEvents = configEvents;
    }

    public List<Event> fetchUntaggedEvents() {
        List<Event> events = interpretConfigEvents();
        untaggedEvents = new ArrayList<>();

        for (Event event : events) {
            for (Tag tag : event.getTags()) {
                List<Event> eventsWithoutTag = event.findEventsWithoutTag(tag);
                if (eventsWithoutTag != null) {
                    untaggedEvents.addAll(eventsWithoutTag);
                }
            }
        }
        System.out.println("UNTAGGED EVENTS");
        System.out.println(untaggedEvents.size());
        return untaggedEvents;
    }

    private List<Event> interpretConfigEvents() {
        System.out.println("INTERPRET CONFIG EVENTS");
        List<Event> events = new ArrayList<>();
        try {
            for (EventConfig configEvent : configEvents) {
                events.add(eventInterpreter.interpret(configEvent.getName(), configEvent.getTags()));
            }
        } catch (UnsupportedEventException | UnsupportedTagException e) {
            e.printStackTrace();
        }
        System.out.println(events.get(0).getName());
        return events;
    }

    public void tagEvents() {
        for (Event event : untaggedEvents) {
            event.tagEvent();
        }
    }
}
