package lassie.tag;

public interface Tag {

    void tagEvent(String eventName, String instanceId);

    String getName();

}