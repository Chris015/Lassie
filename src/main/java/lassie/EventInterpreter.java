package lassie;

import com.amazonaws.services.ec2.AmazonEC2;
import lassie.exception.UnsupportedEventException;
import lassie.exception.UnsupportedTagException;
import lassie.event.Event;
import lassie.event.RunInstances;
import lassie.tag.Tag;

import java.util.ArrayList;
import java.util.List;

public class EventInterpreter {

    private AmazonEC2 ec2;
    private TagInterpreter tagInterpreter;

    public EventInterpreter(AmazonEC2 ec2, TagInterpreter tagInterpreter) {
        this.ec2 = ec2;
        this.tagInterpreter = tagInterpreter;
    }

    public Event interpret(String eventName, List<String> in_tags) throws UnsupportedEventException, UnsupportedTagException {

        List<Tag> tags = interpretTags(in_tags);

        switch (eventName) {
            case "RunInstances": {
                return new RunInstances(tags, ec2);
            }
            default: {
                throw new UnsupportedEventException(eventName + " is not supported");
            }
        }

    }

    private List<Tag> interpretTags(List<String> in_tags) throws UnsupportedTagException {
        List<Tag> tags = new ArrayList<>();
        for (String in_tag : in_tags) {
            tags.clear();
            tags.add(tagInterpreter.interpret(in_tag));
        }
        return tags;
    }
}
