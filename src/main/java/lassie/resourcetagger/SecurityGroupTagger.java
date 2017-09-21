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
import lassie.model.Log;
import lassie.config.Account;
import lassie.model.Event;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SecurityGroupTagger implements ResourceTagger {
    private final static Logger log = Logger.getLogger(SecurityGroupTagger.class);
    private AmazonEC2 ec2;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(List<Log> logs) {
        for (Log log : logs) {
            instantiateEc2Client(log.getAccount());
            parseJson(log.getFilePaths());
            filterTaggedResources(log.getAccount().getOwnerTag());
            tag(log.getAccount().getOwnerTag());
        }
    }

    private void instantiateEc2Client(Account account) {
        log.info("Instantiating EC2 client");
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(account.getAccessKeyId(),
                account.getSecretAccessKey());
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        this.ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(account.getRegions().get(0))
                .build();
        log.info("EC2 client instantiated");
    }

    private void parseJson(List<String> filePaths) {
        log.info("Parsing json");
        String jsonPath = "$..Records[?(@.eventName == 'CreateSecurityGroup' && @.responseElements != null)]";
        for (String filePath : filePaths) {
            try {
                String json = JsonPath.parse(new File(filePath)).read(jsonPath).toString();
                GsonBuilder gsonBuilder = new GsonBuilder();
                JsonDeserializer<Event> deserializer = (jsonElement, type, context) -> {
                    String id = jsonElement
                            .getAsJsonObject().get("responseElements")
                            .getAsJsonObject().get("groupId")
                            .getAsString();
                    String owner = jsonElement
                            .getAsJsonObject().get("userIdentity")
                            .getAsJsonObject().get("arn")
                            .getAsString();
                    log.info("Security group model created. Id: "+ id + " Owner: " + owner);
                    return new Event(id, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createSecurityGroupEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createSecurityGroupEvents);
            } catch (IOException e) {
                log.error("Could not parse json: ", e);
                e.printStackTrace();
            }
        }
        log.info("Done parsing json");
    }

    private void filterTaggedResources(String ownerTag) {
        log.info("Filtering tagged Security groups");
        List<Event> untaggedEvents = new ArrayList<>();
        List<SecurityGroup> securityGroupsWithoutTag = describeSecurityGroup(ownerTag);
        for (SecurityGroup securityGroup : securityGroupsWithoutTag) {
            for (Event event : events) {
                String groupId = securityGroup.getGroupId();
                String eventId = event.getId();
                if (groupId.equals(eventId)) {
                    untaggedEvents.add(event);
                }
            }
        }
        log.info("Done filtering tagged Security groups");
        this.events = untaggedEvents;
    }

    private List<SecurityGroup> describeSecurityGroup(String ownerTag) {
        log.info("Describing Security groups");
        List<SecurityGroup> securityGroups = new ArrayList<>();
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        DescribeSecurityGroupsResult response = ec2.describeSecurityGroups(request);
        for (SecurityGroup securityGroup : response.getSecurityGroups()) {
            if (!hasTag(securityGroup, ownerTag)) {
                securityGroups.add(securityGroup);
            }
        }
        log.info("Found " + securityGroups.size() + " Security groups without tag");
        return securityGroups;
    }

    private boolean hasTag(SecurityGroup securityGroup, String tag) {
        log.trace(tag + " found: " +  securityGroup.getTags().stream().anyMatch(t -> t.getKey().equals(tag)));
        return securityGroup.getTags().stream().anyMatch(t -> t.getKey().equals(tag));
    }

    private void tag(String ownerTag) {
        log.info("Tagging Security groups");
        for (Event event : events) {
            CreateTagsRequest tagsRequest = new CreateTagsRequest()
                    .withResources(event.getId())
                    .withTags(new Tag(ownerTag, event.getOwner()));
            ec2.createTags(tagsRequest);
            log.info("Tagged: " + event.getId() +
                    " with key: " + ownerTag +
                    " value: " + event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Done tagging Security groups");
    }
}
