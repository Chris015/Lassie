package lassie.event;

public class RunJobFlow implements Event {
    private String id;
    private String owner;

    public RunJobFlow(String id, String owner) {
        this.id = id;
        resolve(owner);
    }

    private void resolve(String arn) {
        this.owner = arn.substring(arn.lastIndexOf('/') + 1, arn.length());
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
