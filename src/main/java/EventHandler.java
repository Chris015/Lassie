import config.EventConfig;
import event.Event;
import exception.UnsupportedEventException;
import exception.UnsupportedTagException;
import tag.Tag;

import java.util.ArrayList;
import java.util.List;

public class EventHandler {
    private EventInterpreter eventInterpreter;
    private List<EventConfig> configEvents;

    public EventHandler(EventInterpreter eventInterpreter, List<EventConfig> configEvents) {
        this.eventInterpreter = eventInterpreter;
        this.configEvents = configEvents;
    }

    public List<Event> fetchUntaggedEvents() {
        List<Event> untaggedEvents = new ArrayList<>();
        List<Event> events = interpretConfigEvents();
        for (Event event : events) {
            for (Tag tag : event.getTags()) {
                List<Event> eventsWithoutTag = event.findEventsWithoutTag(tag);
                if(eventsWithoutTag != null ) {
                    untaggedEvents.addAll(eventsWithoutTag);
                }
            }
        }
        for (Event untaggedEvent : untaggedEvents) {
            System.out.println(untaggedEvent.getLaunchTime() + " " + untaggedEvent.getInstanceId());
        }
        return untaggedEvents;
    }

    private List<Event> interpretConfigEvents() {
        List<Event> events = new ArrayList<>();
        try {
            for (EventConfig configEvent : configEvents) {
                events.add(eventInterpreter.interpret(configEvent.getName(), configEvent.getTags()));
            }
        } catch (UnsupportedEventException | UnsupportedTagException e) {
            e.printStackTrace();
        }
        return events;
    }

}
