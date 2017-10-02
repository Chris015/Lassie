package lassie.mocks;

import com.amazonaws.services.ec2.model.*;
import org.apache.log4j.Logger;

import java.util.*;

public class EC2HandlerMock implements lassie.awshandlers.Ec2Handler {
    private final static Logger log = Logger.getLogger(EC2HandlerMock.class);
    public static HashMap<String, Instance> instances = new HashMap<>();
    public static HashMap<String, SecurityGroup> securityGroups = new HashMap<>();
    public static HashMap<String, Volume> volumes = new HashMap<>();

    public EC2HandlerMock() {
        populateInstancesData();
        populateSecurityGroupsData();
        populateVolumesData();
    }

    private void populateInstancesData() {
        // tagged instance, and an event is created
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("Owner", "john.doe"));

        Instance instance = new Instance();
        instance.setInstanceId("i-06e9aaf9760467624");
        instance.setTags(tags);
        instances.put(instance.getInstanceId(), instance);

        // instance without tag, and an event has been created
        instance = new Instance();
        instance.setInstanceId("i-07a5bbg4326310341");
        instance.setTags(new ArrayList<>());
        instances.put(instance.getInstanceId(), instance);
    }

    private void populateSecurityGroupsData() {
        // tagged security group, and an event is created
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("Owner", "jane.doe"));

        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setGroupId("s-9831ba2f192s2");
        securityGroup.setTags(tags);
        securityGroups.put(securityGroup.getGroupId(), securityGroup);

        // security group without tag, and an event has been created
        securityGroup = new SecurityGroup();
        securityGroup.setGroupId("s-1214cb1l323b1");
        securityGroup.setTags(new ArrayList<>());
        securityGroups.put(securityGroup.getGroupId(), securityGroup);
    }

    private void populateVolumesData() {
        // tagged volume, and an event is created
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("Owner", "jane.doe"));

        Volume volume = new Volume();
        volume.setVolumeId("v-109812b123a21");
        volume.setTags(tags);
        volumes.put(volume.getVolumeId(), volume);

        // volume without tag, and event has been created
        volume = new Volume();
        volume.setVolumeId("v-203412c121a31");
        volume.setTags(new ArrayList<>());
        volumes.put(volume.getVolumeId(), volume);

        // volume without tag, and event has not been created but has an attachment
        volume = new Volume();
        volume.setVolumeId("v-313821c242b32");
        volume.setTags(new ArrayList<>());

        List<VolumeAttachment> volumeAttachments = new ArrayList<>();
        VolumeAttachment volumeAttachment = new VolumeAttachment();
        volumeAttachment.setInstanceId("i-06e9aaf9760467624");
        volumeAttachments.add(volumeAttachment);
        volume.setAttachments(volumeAttachments);
        volumes.put(volume.getVolumeId(), volume);
    }

    @Override
    public void instantiateEC2Client(String accessKeyId, String secretAccessKey, String region) {
    }

    @Override
    public void tagResource(String id, String key, String value) {
        Tag tag = new Tag(key, value);
        List<Tag> tags = new ArrayList<>();
        tags.add(tag);

        char resourceType = id.charAt(0);
        switch (resourceType) {
            case 'i': {
                Instance instance = instances.get(id);
                instance.setTags(tags);
                instances.put(id, instance);
            }
            break;
            case 'v': {
                Volume volume = volumes.get(id);
                volume.setTags(tags);
                volumes.put(volume.getVolumeId(), volume);
            }
            break;
            case 's': {
                SecurityGroup securityGroup = securityGroups.get(id);
                securityGroup.setTags(tags);
                securityGroups.put(securityGroup.getGroupId(), securityGroup);
            }
            break;
            default:
                return;
        }
        log.info("Tagged: " + id + " with key: " + key + " value: " + value);
    }

    @Override
    public String getTagValueForInstanceWithId(String tagKey, String instanceId) {
        Instance instance = instances.get(instanceId);
        List<Tag> tags = instance.getTags();
        for (Tag tag : tags) {
            if (tag.getKey().equals(tagKey))
                return tag.getValue();
        }
        return null;
    }

    @Override
    public List<String> getIdsForInstancesWithoutTag(String tag) {
        List<String> instancesWithoutTag = new ArrayList<>();
        for (Instance instance : instances.values()) {
            List<Tag> tags = instance.getTags();
            if (tags.stream().noneMatch(t -> t.getKey().equals(tag))) {
                instancesWithoutTag.add(instance.getInstanceId());
            }
        }

        return instancesWithoutTag;
    }

    @Override
    public List<String> getIdsForSecurityGroupsWithoutTag(String tag) {
        List<String> securityGroupsWithoutTag = new ArrayList<>();
        for (SecurityGroup securityGroup : securityGroups.values()) {
            List<Tag> tags = securityGroup.getTags();
            if (tags.stream().noneMatch(t -> t.getKey().equals(tag))) {
                securityGroupsWithoutTag.add(securityGroup.getGroupId());
            }
        }
        return securityGroupsWithoutTag;
    }

    @Override
    public List<String> getIdsForVolumesWithoutTag(String tag) {
        List<String> volumesWithoutTag = new ArrayList<>();
        for (Volume volume : volumes.values()) {
            List<Tag> tags = volume.getTags();
            if (tags.stream().noneMatch(t -> t.getKey().equals(tag))) {
                volumesWithoutTag.add(volume.getVolumeId());
            }

        }
        return volumesWithoutTag;
    }

    @Override
    public boolean volumeIsAttachedToInstance(String volumeId) {
        Volume volume = volumes.get(volumeId);
        return volume.getAttachments().size() > 0;
    }

    @Override
    public String getIdForInstanceVolumeIsAttachedTo(String volumeId) {
        Volume volume = volumes.get(volumeId);
        List<VolumeAttachment> attachments = volume.getAttachments();
        return attachments.get(0).getInstanceId();
    }
}
