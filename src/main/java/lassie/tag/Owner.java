package lassie.tag;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.jayway.jsonpath.JsonPath;
import lassie.LogPersister;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.List;

public class Owner implements Tag {

    private String name = "Owner";
    private AmazonEC2 ec2;
    private LogPersister logPersister;

    public Owner(AmazonEC2 ec2, LogPersister logPersister) {
        this.ec2 = ec2;
        this.logPersister = logPersister;
    }

    @Override
    public void tagEvent(String eventName, String instanceId) {
        List<String> jsonFiles = logPersister.fetchUnzippedFiles();
        for (String json : jsonFiles) {
            String owner = findRecordsWithEventName(eventName, json, instanceId);

            if (owner != null) {

                CreateTagsRequest tagsRequest = new CreateTagsRequest()
                        .withResources(instanceId)
                        .withTags(new com.amazonaws.services.ec2.model.Tag(this.name, owner));
                ec2.createTags(tagsRequest);
            }
        }
    }

    private String findRecordsWithEventName(String eventName, String json, String instanceId) {
        String jsonExp =  "$..Records[?(@.responseElements.instancesSet.items[0].instanceId=="
                + "\'" +instanceId + "\')].userIdentity.arn";
        JSONArray result  = JsonPath.read(json, jsonExp);

        if (result.size() == 0) {
            return null;
        }

        String owner = String.valueOf(result.get(0));
        return owner.substring(owner.lastIndexOf('/') + 1, owner.length());
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
