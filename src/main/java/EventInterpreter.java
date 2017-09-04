import com.amazonaws.services.ec2.AmazonEC2;
import exceptions.UnsupportedEventException;
import model.Event;
import model.RunInstance;
import model.Tag;

import java.util.List;

public class EventInterpreter {

    private AmazonEC2 ec2;

    public Event interpret(String eventName, List<Tag> tags) throws UnsupportedEventException {
        switch (eventName) {
            case "RunInstance": {
                return new RunInstance(eventName, tags, ec2);
            }
            default: {
                throw new UnsupportedEventException(eventName + " is not supported");
            }
        }

    }
}
