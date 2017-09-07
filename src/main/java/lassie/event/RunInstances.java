package lassie.event;

public class RunInstances implements Event{
    private String instanceId;
    private String userName;

    public RunInstances(String instanceId, String userName) {
        this.instanceId = instanceId;
        this.userName = userName;
    }

    @Override
    public String getId() {
        return this.instanceId;
    }

    @Override
    public String getOwner() {
        return this.userName;
    }
}
