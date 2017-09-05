package lassie.tag;

import com.amazonaws.services.ec2.AmazonEC2;
import com.jayway.jsonpath.JsonPath;
import lassie.LogPersister;

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
            findRecordsWithEventName(eventName, json, instanceId);
        }
    }

    private void findRecordsWithEventName(String eventName, String json, String instanceId) {
        String jsonExp =  "$..Records[?(@.responseElements.instancesSet.items[0].instanceId=="
                + "\'" +instanceId + "\')].userIdentity.arn";
        System.out.println(jsonExp);
        System.out.println("Json: " + json);
        String arn = JsonPath.read(jsonExp, json);
        System.out.println(arn);


    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
