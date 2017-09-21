package lassie.awshandlers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class EC2Handler {
    private final static Logger log = Logger.getLogger(EC2Handler.class);
    private AmazonEC2 ec2;

    public void instantiateEC2Client(String accessKeyId, String secretAccessKey, String region) {
        log.info("Instantiating EC2 client");
        BasicAWSCredentials basicCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(basicCredentials);
        this.ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(region)
                .build();
        log.info("EC2 client instantiated");
    }

    public void tagResource(String id, String key, String value) {
        CreateTagsRequest tagsRequest = new CreateTagsRequest()
                .withResources(id)
                .withTags(new Tag(key, value));
        ec2.createTags(tagsRequest);
    }

    public boolean instanceHasTag(String id, String tag) {
        List<Instance> untaggedInstances = getInstancesWithoutTag(tag);
        return untaggedInstances.stream().noneMatch(instance -> instance.getInstanceId().equals(id));
    }

    public boolean volumeHasTag(String id, String tag) {
        List<Volume> untaggedVolumes = getVolumesWithoutTags(tag);
        return untaggedVolumes.stream().noneMatch(volume -> volume.getVolumeId().equals(id));
    }

    public boolean securityGroupHasTag(String id, String tag) {
        List<SecurityGroup> untaggedSecurityGroups = getSecurityGroupsWithoutTag(tag);
        return untaggedSecurityGroups.stream().noneMatch(securityGroup -> securityGroup.getGroupId().equals(id));
    }



    private List<Instance> getInstancesWithoutTag(String tag) {
        log.info("Getting instances without tags");

        List<Instance> untaggedInstances = new ArrayList<>();
        List<Instance> instances = new ArrayList<>();

        boolean done = false;
        while (!done) {
            DescribeInstancesRequest request = new DescribeInstancesRequest();
            DescribeInstancesResult response = ec2.describeInstances(request);

            List<Reservation> reservations = response.getReservations();
            reservations.forEach(reservation -> instances.addAll(reservation.getInstances()));

            for (Instance instance : instances) {
                if(instance.getTags().stream().noneMatch(t -> t.getKey().equals(tag))) {
                    untaggedInstances.add(instance);
                }
            }

            request.setNextToken(response.getNextToken());
            if (response.getNextToken() == null) {
                done = true;
            }
        }
        log.info("Getting instances without tags complete");
        return untaggedInstances;
    }

    private List<SecurityGroup> getSecurityGroupsWithoutTag(String tag) {
        log.info("Describing Security groups");
        List<SecurityGroup> securityGroups = new ArrayList<>();
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        DescribeSecurityGroupsResult response = ec2.describeSecurityGroups(request);
        for (SecurityGroup securityGroup : response.getSecurityGroups()) {
            if (securityGroup.getTags().stream().noneMatch(t -> t.getKey().equals(tag))) {
                securityGroups.add(securityGroup);
            }
        }
        log.info("Found " + securityGroups.size() + " Security groups without tagResource");
        return securityGroups;
    }

    private List<Volume> getVolumesWithoutTags(String tag) {
        log.info("Describing volumes");
        List<Volume> volumesWithoutTags = new ArrayList<>();
        boolean done = false;
        while (!done) {
            DescribeVolumesRequest request = new DescribeVolumesRequest();
            DescribeVolumesResult result = ec2.describeVolumes(request);
            for (Volume volume : result.getVolumes()) {
                if (volume.getTags().stream().noneMatch(t -> t.getKey().equals(tag))) {
                    volumesWithoutTags.add(volume);
                }
            }
            request.setNextToken(result.getNextToken());
            if (result.getNextToken() == null) {
                done = true;
            }
        }
        log.info("Found " + volumesWithoutTags.size() + " EBS volumes without " + tag);
        return volumesWithoutTags;
    }
}

