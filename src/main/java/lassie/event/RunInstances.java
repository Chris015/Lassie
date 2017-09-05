package lassie.event;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import lassie.tag.Tag;

import java.util.ArrayList;
import java.util.List;

public class RunInstances implements Event {

    private String name = "RunInstances";
    private long launchTime;
    private String instanceId;
    private List<Tag> tags;
    private String ownerId;

    private AmazonEC2 ec2;

    public RunInstances() {
    }

    public RunInstances(String name, List<Tag> tags, AmazonEC2 ec2) {
        this.name = name;
        this.tags = tags;
        this.ec2 = ec2;
    }

    public RunInstances(List<Tag> tags, AmazonEC2 ec2) {
        this.tags = tags;
        this.ec2 = ec2;
    }

    public RunInstances(List<Tag> tags, String name, long launchTime, String instanceId, String ownerId) {
        this.tags = tags;
        this.name = name;
        this.launchTime = launchTime;
        this.instanceId = instanceId;
        this.ownerId = ownerId;
    }

    //ToDo extract the logic to a different class?
    @Override
    public List<Event> findEventsWithoutTag(Tag tag) {
        List<Instance> instances = describeInstances();
        return filterInstancesWithoutTag(instances, tag);
    }

    private List<Instance> describeInstances() {
        List<Instance> instances = new ArrayList<>();

        boolean done = false;

        while (!done) {
            DescribeInstancesRequest request = new DescribeInstancesRequest();
            DescribeInstancesResult response = ec2.describeInstances(request);
            for (Reservation reservation : response.getReservations()) {
                instances.addAll(reservation.getInstances());
            }

            request.setNextToken(response.getNextToken());

            if (response.getNextToken() == null) {
                done = true;
            }
        }
        return instances;
    }

    private List<Event> filterInstancesWithoutTag(List<Instance> instances, Tag tag) {
        List<Event> untaggedEvents = new ArrayList<>();
        List<Tag> tags = new ArrayList<>();
        tags.add(tag);


        for (Instance instance : instances) {
            if (instance.getTags().stream().noneMatch(t -> t.getKey().equals(tag.getName()))) {
                untaggedEvents.add(new RunInstances(
                        tags,
                        this.name,
                        instance.getLaunchTime().getTime(),
                        instance.getInstanceId(),
                        instance.getNetworkInterfaces().get(0).getOwnerId()));
                System.out.println(untaggedEvents.get(0).getTags().get(0).getName());
            }
        }
        return untaggedEvents;
    }

    @Override
    public void tagEvent() {
        for (Tag tag : tags) {
            tag.tagEvent(this.name, this.instanceId);
        }
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public long getLaunchTime() {
        return launchTime;
    }


    @Override
    public List<Tag> getTags() {
        return tags;
    }

    @Override
    public String getOwnerId() {
        return ownerId;
    }

}
