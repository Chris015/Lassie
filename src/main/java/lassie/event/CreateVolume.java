package lassie.event;

public class CreateVolume implements Event {
    private String id;
    private String owner;

    public CreateVolume(String id, String arn) {
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
