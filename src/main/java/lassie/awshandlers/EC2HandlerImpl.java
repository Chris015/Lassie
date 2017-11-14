package lassie.awshandlers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static lassie.Application.DRY_RUN;

public class EC2HandlerImpl implements Ec2Handler {
    private static final Logger logger = LogManager.getLogger(EC2HandlerImpl.class);
    private AmazonEC2 ec2;

    public void instantiateEC2Client(String accessKeyId, String secretAccessKey, String region) {
        logger.trace("Instantiating EC2 client in region: {}", region);
        BasicAWSCredentials basicCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(basicCredentials);
        this.ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(region)
                .build();
        logger.trace("EC2 client instantiated");
    }

    public void tagResource(String id, String key, String value) {
        if (DRY_RUN) {
            logger.info("Dry run: {}. Did not tag: {} with {}: {}", DRY_RUN, id, key, value);
            return;
        }
        CreateTagsRequest tagsRequest = new CreateTagsRequest()
                .withResources(id)
                .withTags(new Tag(key, value));
        ec2.createTags(tagsRequest);
        logger.info("Tagged: {} with key: {} value: {}", id, key, value);
    }

    public String getTagValueForInstanceWithId(String tagKey, String instanceId) {
        List<Instance> instances = getInstances();
        if (instances.stream().noneMatch(instance -> instance.getInstanceId().equals(instanceId))) {
            throw new IllegalArgumentException("Instance with id " + instanceId + " not found");
        }

        Instance instance = filterInstanceByID(instances, instanceId);
        if (instance == null) {
            throw new IllegalArgumentException("Instance with id " + instanceId + " not found");
        }

        for (Tag t : instance.getTags()) {
            if (t.getKey().equals(tagKey)) {
                return t.getValue();
            }
        }
        throw new IllegalArgumentException("Instance: " + instanceId + " does not have a tag with key " + tagKey);
    }

    private Instance filterInstanceByID(List<Instance> instances, String instanceId) {
        Instance instance = null;
        for (Instance i : instances) {
            if (i.getInstanceId().equals(instanceId)) {
                instance = i;
            }
        }
        return instance;
    }

    public List<String> getIdsForInstancesWithoutTag(String tag) {
        logger.trace("Getting instances without tags");
        List<String> untaggedInstanceIds = new ArrayList<>();
        List<Instance> instances = getInstances();

        for (Instance instance : instances) {
            if (instance.getTags().stream().noneMatch(t -> t.getKey().equals(tag))) {
                untaggedInstanceIds.add(instance.getInstanceId());
            }
        }


        logger.info("Found {} instances without: {} on AWS", untaggedInstanceIds.size(), tag);
        untaggedInstanceIds.forEach(logger::info);
        return untaggedInstanceIds;
    }

    private List<Instance> getInstances() {
        List<Instance> instances = new ArrayList<>();
        boolean done = false;
        while (!done) {
            DescribeInstancesRequest request = new DescribeInstancesRequest();
            DescribeInstancesResult response = ec2.describeInstances(request);

            List<Reservation> reservations = response.getReservations();
            reservations.forEach(reservation -> instances.addAll(reservation.getInstances()));

            request.setNextToken(response.getNextToken());
            if (response.getNextToken() == null) {
                done = true;
            }
        }
        return instances;
    }

    public List<String> getIdsForSecurityGroupsWithoutTag(String tag) {
        logger.trace("Describing Security groups");
        List<String> untaggedSecurityGroupIds = new ArrayList<>();
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        DescribeSecurityGroupsResult response = ec2.describeSecurityGroups(request);
        for (SecurityGroup securityGroup : response.getSecurityGroups()) {
            if (securityGroup.getTags().stream().noneMatch(t -> t.getKey().equals(tag))) {
                untaggedSecurityGroupIds.add(securityGroup.getGroupId());
            }
        }
        logger.info("Found {} Security groups without: {} on AWS", untaggedSecurityGroupIds.size(), tag);
        untaggedSecurityGroupIds.forEach(logger::info);
        return untaggedSecurityGroupIds;
    }

    public List<String> getIdsForVolumesWithoutTag(String tag) {
        logger.trace("Describing volumes");
        List<String> untaggedVolumesIds = new ArrayList<>();
        boolean done = false;
        while (!done) {
            DescribeVolumesRequest request = new DescribeVolumesRequest();
            DescribeVolumesResult result = ec2.describeVolumes(request);
            for (Volume volume : result.getVolumes()) {
                if (volume.getTags().stream().noneMatch(t -> t.getKey().equals(tag))) {
                    untaggedVolumesIds.add(volume.getVolumeId());
                }
            }
            request.setNextToken(result.getNextToken());
            if (result.getNextToken() == null) {
                done = true;
            }
        }
        logger.info("Found {} EBS volumes without: {} on AWS", untaggedVolumesIds.size(), tag);
        untaggedVolumesIds.forEach(logger::info);
        return untaggedVolumesIds;
    }

    public boolean volumeIsAttachedToInstance(String volumeId) {
        Volume volume = getVolumeWithId(volumeId);
        return volume.getAttachments().size() > 0;

    }

    public String getIdForInstanceVolumeIsAttachedTo(String volumeId) {
        Volume volume = getVolumeWithId(volumeId);
        return volume.getAttachments().get(0).getInstanceId();
    }

    private Volume getVolumeWithId(String volumeId) {
        boolean done = false;
        while (!done) {
            DescribeVolumesRequest request = new DescribeVolumesRequest();
            DescribeVolumesResult result = ec2.describeVolumes(request);

            List<Volume> volumes = result.getVolumes();
            for (Volume volume : volumes) {
                if (volume.getVolumeId().equals(volumeId)) {
                    return volume;
                }
            }
            request.setNextToken(result.getNextToken());
            if (result.getNextToken() == null) {
                done = true;
            }
        }
        throw new IllegalArgumentException("Volume with id " + volumeId + " not found");
    }
}

