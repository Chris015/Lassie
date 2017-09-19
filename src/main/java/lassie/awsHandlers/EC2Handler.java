package lassie.awsHandlers;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class EC2Handler {
    private static Logger log = Logger.getLogger(EC2Handler.class);
    private AmazonEC2 ec2;

    public void setEc2Client(AmazonEC2 ec2) {
        this.ec2 = ec2;
    }

    public void tagResource(String id, Tag tag) {
        CreateTagsRequest tagsRequest = new CreateTagsRequest()
                .withResources(id)
                .withTags(tag);
        ec2.createTags(tagsRequest);
    }


    public boolean instanceHasTag(String id, String tag) {
        List<Instance> untaggedInstances = getInstancesWithoutTag(tag);
        return untaggedInstances.stream().noneMatch(instance -> instance.getInstanceId().equals(id));
    }

    private List<Instance> getInstancesWithoutTag(String tag) {
        List<Instance> instances = getInstances();
        return filterOutInstancesWithoutTag(instances, tag);
    }

    private List<Instance> getInstances() {
        log.info("Describing instances");
        List<Instance> instances = new ArrayList<>();
        List<Instance> runningInstances = new ArrayList<>();
        boolean done = false;
        while (!done) {
            DescribeInstancesRequest request = new DescribeInstancesRequest();
            DescribeInstancesResult response = ec2.describeInstances(request);

            List<Reservation> reservations = response.getReservations();
            reservations.forEach(reservation -> instances.addAll(reservation.getInstances()));

            for (Instance instance : instances) {
                if (!instance.getState().getName().equals(InstanceStateName.Terminated.name())) {
                    runningInstances.add(instance);
                }
            }

            request.setNextToken(response.getNextToken());
            if (response.getNextToken() == null) {
                done = true;
            }
        }
        log.info("Describe instances complete");
        return runningInstances;
    }

    private List<Instance> filterOutInstancesWithoutTag(List<Instance> instances, String tag) {
        List<Instance> untaggedInstances = new ArrayList<>();
        for (Instance instance : instances) {
            if (instance.getTags().stream().noneMatch(t -> t.getKey().equals(tag))) {
                untaggedInstances.add(instance);
            }
        }
        return untaggedInstances;
    }
}

