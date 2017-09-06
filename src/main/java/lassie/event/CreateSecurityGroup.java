package lassie.event;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import lassie.tag.Tag;

import java.util.ArrayList;
import java.util.List;

public class CreateSecurityGroup implements Event {

    private String name = "CreateSecurityGroup";
    private long launchTime;
    private String groupId;
    private List<Tag> tags;
    private String ownerId;

    private AmazonEC2 ec2;

    public CreateSecurityGroup(List<Tag> tags, String name, long launchTime, String groupId, String ownerId) {
        this.tags = tags;
        this.name = name;
        this.launchTime = launchTime;
        this.groupId = groupId;
        this.ownerId = ownerId;
    }



    public CreateSecurityGroup(List<Tag> tags, AmazonEC2 ec2) {
        this.tags = tags;
        this.ec2 = ec2;
    }

    @Override
    public List<Event> findEventsWithoutTag(Tag tag) {
        List<SecurityGroup> securityGroups = describeSecurityGroup();
        return filterEventsWithoutTag(securityGroups, tag);
    }

    private List<Event> filterEventsWithoutTag(List<SecurityGroup> securityGroups, Tag tag) {

        List<Event> untaggedEvents = new ArrayList<>();
        List<Tag> tags = new ArrayList<>();
        tags.add(tag);

        for (SecurityGroup securityGroup : securityGroups) {
            if(securityGroup.getTags().stream().noneMatch(t -> t.getKey().equals(tag.getName()))){
                untaggedEvents.add(new CreateSecurityGroup(
                        tags,
                        this.name,
                        0,
                        securityGroup.getGroupId(),
                        securityGroup.getOwnerId()
                ));
            }
        }
        return untaggedEvents;
    }

    private List<SecurityGroup> describeSecurityGroup() {
        List<SecurityGroup> securityGroups = new ArrayList<>();

            DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
            DescribeSecurityGroupsResult response = ec2.describeSecurityGroups(request);

            securityGroups.addAll(response.getSecurityGroups());

        return securityGroups;
    }

    @Override
    public long getLaunchTime() {
        return this.launchTime;
    }

    @Override
    public String getId() {
        return this.groupId;
    }

    @Override
    public List<Tag> getTags() {
        return this.tags;
    }

    @Override
    public String getOwnerId() {
        return this.ownerId;
    }

    @Override
    public void tagEvent() {
        for (Tag tag : tags) {
            tag.tagEvent(this);
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getArnJsonPath() {
        return "$..Records[?(@.responseElements.groupId==" + "\'" + this.groupId + "\')].userIdentity.arn";
    }
}
