package lassie.resourcetagger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.Log;
import lassie.event.CreateSecurityGroup;
import lassie.event.Event;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreateSecurityGroupTagger implements ResourceTagger {
    private AmazonEC2 ec2;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(List<Log> logs) {
        System.out.println("Parsing json");
        for (Log log : logs) {
            instantiateEc2Client(log);
            parseJson(log);
            filterTaggedResources(log);
            tag(log);
        }
    }

    private void instantiateEc2Client(Log log) {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(log.getAccount().getAccessKeyId(), log.getAccount().getSecretAccessKey());
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        this.ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(log.getAccount().getRegions().get(0))
                .build();
    }

    private void parseJson(Log log) {
        try {
            String json = JsonPath.parse(new File(log.getFilePath()))
                    .read("$..Records[?(@.eventName == 'CreateSecurityGroup' && @.responseElements != null)]")
                    .toString();
            GsonBuilder gsonBuilder = new GsonBuilder();
            JsonDeserializer<CreateSecurityGroup> deserializer = (jsonElement, type, context) -> {
                String id = jsonElement
                        .getAsJsonObject().get("responseElements")
                        .getAsJsonObject().get("groupId").getAsString();
                String owner = jsonElement.getAsJsonObject().get("userIdentity").getAsJsonObject().get("arn").getAsString();
                return new CreateSecurityGroup(id, owner);
            };

            gsonBuilder.registerTypeAdapter(CreateSecurityGroup.class, deserializer);

            Gson gson = gsonBuilder.setLenient().create();
            List<CreateSecurityGroup> createSecurityGroups = gson.fromJson(
                    json, new TypeToken<List<CreateSecurityGroup>>() {}.getType());
            events.addAll(createSecurityGroups);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void filterTaggedResources(Log log) {
        List<Event> untaggedEvents = new ArrayList<>();
        List<SecurityGroup> securityGroupsWithoutTags = describeSecurityGroup(log);

        for (SecurityGroup securityGroupsWithoutTag : securityGroupsWithoutTags) {
            for (Event event : events) {
                String groupId = securityGroupsWithoutTag.getGroupId();
                String eventId = event.getId();
                if (groupId.equals(eventId)){
                    untaggedEvents.add(event);
                }
            }
        }
        this.events = untaggedEvents;
    }



    private List<SecurityGroup> describeSecurityGroup(Log log) {
        List<SecurityGroup> securityGroups = new ArrayList<>();

        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        DescribeSecurityGroupsResult response = ec2.describeSecurityGroups(request);

        for (SecurityGroup securityGroup : response.getSecurityGroups()) {
            if (securityGroup.getTags().stream().noneMatch(t -> t.getKey().equals(log.getAccount().getOwnerTag()))) {
                securityGroups.add(securityGroup);
            }
        }

        return securityGroups;

    }

    private void tag(Log log){
        for (Event event : events) {
            CreateTagsRequest tagsRequest = new CreateTagsRequest()
                    .withResources(event.getId())
                    .withTags(new Tag(log.getAccount().getOwnerTag(), event.getOwner()));
            ec2.createTags(tagsRequest);
            System.out.println("Tagged: " + event.getId() +
                    " with key: " + log.getAccount().getOwnerTag() +
                    " value: " + event.getOwner());
        }
    }
}
