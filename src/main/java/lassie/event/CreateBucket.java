package lassie.event;

public class CreateBucket implements Event {
    private String id;
    private String owner;

    public CreateBucket(String id, String arn) {
        this.id = id;
        resolve(arn);
    }

    private void resolve(String arn) {
        this.owner = arn.substring(arn.lastIndexOf('/') +1, arn.length());
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
