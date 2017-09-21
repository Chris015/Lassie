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
import lassie.awshandlers.EC2Handler;
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
    EC2Handler ec2Handler;

    public SecurityGroupTagger(EC2Handler ec2Handler) {
        this.ec2Handler = ec2Handler;
    }

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
        ec2Handler.instantiateEC2Client(account.getAccessKeyId(), account.getSecretAccessKey(), account.getRegions().get(0));
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

        for (Event event : events) {
            if(!ec2Handler.securityGroupHasTag(event.getId(), ownerTag)) {
                untaggedEvents.add(event);
            }
        }
        log.info("Done filtering tagged Security groups");
        this.events = untaggedEvents;
    }

    private void tag(String ownerTag) {
        log.info("Tagging Security groups");
        for (Event event : events) {
            ec2Handler.tagResource(event.getId(), ownerTag, event.getOwner());
            log.info("Tagged: " + event.getId() + " with key: " + ownerTag + " value: " + event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Done tagging Security groups");
    }
}
