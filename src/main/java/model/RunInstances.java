package model;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

import java.util.ArrayList;
import java.util.List;

public class RunInstances implements Event {

    private String name = "RunInstances";
    private long launchTime;
    private String instanceId;
    private List<Tag> tags;

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

    public RunInstances(String name, long launchTime, String instanceId) {
        this.name = name;
        this.launchTime = launchTime;
        this.instanceId = instanceId;
    }

    @Override
    public List<Event> findEventsWithoutTag(Tag tag) {
        List<Event> untaggedEvents = new ArrayList<>();

        boolean done = false;

        while (!done) {
            DescribeInstancesRequest request = new DescribeInstancesRequest();
            DescribeInstancesResult response = ec2.describeInstances(request);

            for (Reservation reservation : response.getReservations()) {
                for (Instance instance : reservation.getInstances()) {

                    if (instance.getTags().stream().noneMatch(t -> t.getKey().equals(tag.getName()))) {
                        untaggedEvents.add(new RunInstances(this.name, instance.getLaunchTime().getTime(), instance.getInstanceId()));
                    }
                }

            }

            request.setNextToken(response.getNextToken());

            if (response.getNextToken() == null) {
                done = true;
            }

            return null;
        }
        return untaggedEvents;
    }

    @Override
    public void tagEvent() {
        for (Tag tag : tags) {
            tag.tagEvent(this.instanceId);
        }
    }

    public long getLaunchTime() {
        return launchTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }
}
