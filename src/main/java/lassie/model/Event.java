package lassie.model;

public class Event {
    private String id;
    private String owner;

    public Event(String id, String arn) {
        this.id = id;
        this.owner = resolveArn(arn);
    }

    private String resolveArn(String arn) {
        if (arn.contains("/")) {
            return arn.substring(arn.lastIndexOf('/') + 1, arn.length());
        }
        return arn.substring(arn.lastIndexOf(':') + 1, arn.length());
    }

    public String getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }
}
