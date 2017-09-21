package lassie.awsHandlers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.rds.model.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RDSHandler {
    private static final Logger log = Logger.getLogger(RDSHandler.class);
    private AmazonRDS rds;

    public void instantiateRDSClient(String accessKeyId, String secretAccessKey, String region) {
        log.info("Instantiating RDS client");
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        this.rds = AmazonRDSClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(region)
                .build();
        log.info("RDS client instantiated");
    }

    public List<DBInstance> describeRDSInstances(String ownerTag) {
        log.info("Describing DB instances");
        List<DBInstance> dbInstancesWithoutOwner = new ArrayList<>();
        DescribeDBInstancesResult describeDBInstancesResult = rds.describeDBInstances(new DescribeDBInstancesRequest());
        for (DBInstance dbInstance : describeDBInstancesResult.getDBInstances()) {
            ListTagsForResourceRequest request = new ListTagsForResourceRequest()
                    .withResourceName(dbInstance.getDBInstanceArn());
            ListTagsForResourceResult response = rds.listTagsForResource(request);
            if (!hasTag(response, ownerTag)) {
                dbInstancesWithoutOwner.add(dbInstance);
            }
        }
        log.info("Found " + dbInstancesWithoutOwner.size() + " DB instances without " + ownerTag);
        return dbInstancesWithoutOwner;
    }

    private boolean hasTag(ListTagsForResourceResult response, String tag) {
        log.trace(tag + " found: " + response.getTagList().stream().anyMatch(t -> t.getKey().equals(tag)));
        return response.getTagList().stream().anyMatch(t -> t.getKey().equals(tag));
    }

    public void tagResource(String id, String key, String value) {
        Tag tag = new Tag();
        tag.setKey(key);
        tag.setValue(value);
        AddTagsToResourceRequest tagsRequest = new AddTagsToResourceRequest()
                .withResourceName(id)
                .withTags(tag);
        rds.addTagsToResource(tagsRequest);
    }
}
