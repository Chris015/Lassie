package lassie.event;

public class CreateLoadBalancer implements Event{
    private String id;
    private String owner;

    public CreateLoadBalancer(String id, String owner) {
        this.id = id;
        this.owner = owner;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getOwner() {
        return this.owner;
    }
}
