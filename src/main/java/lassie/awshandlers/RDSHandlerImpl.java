package lassie.awshandlers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.rds.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static lassie.Application.DRY_RUN;

public class RDSHandlerImpl implements RDSHandler {
    private static final Logger logger = LogManager.getLogger(RDSHandlerImpl.class);
    private AmazonRDS rds;

    public void instantiateRDSClient(String accessKeyId, String secretAccessKey, String region) {
        logger.trace("Instantiating RDS client in region: {}", region);
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        this.rds = AmazonRDSClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(region)
                .build();
        logger.trace("RDS client instantiated");
    }

    public void tagResource(String id, String key, String value) {
        if (DRY_RUN) {
            logger.info("Dry run: {}. Did not tag: {} with {}: {}", DRY_RUN, id, key, value);
            return;
        }
        Tag tag = new Tag();
        tag.setKey(key);
        tag.setValue(value);
        AddTagsToResourceRequest tagsRequest = new AddTagsToResourceRequest()
                .withResourceName(id)
                .withTags(tag);
        rds.addTagsToResource(tagsRequest);
        logger.info("Tagged: {} with key: {} value: {}", id, key, value);
    }

    public List<String> getIdsForDBInstancesWithoutTag(String tag) {
        logger.trace("Describing DB instances");
        List<String> untaggedDbInstanceIds = new ArrayList<>();
        DescribeDBInstancesResult describeDBInstancesResult = rds.describeDBInstances(new DescribeDBInstancesRequest());
        for (DBInstance dbInstance : describeDBInstancesResult.getDBInstances()) {
            ListTagsForResourceRequest request = new ListTagsForResourceRequest()
                    .withResourceName(dbInstance.getDBInstanceArn());
            ListTagsForResourceResult response = rds.listTagsForResource(request);
            if (!hasTag(response, tag)) {
                untaggedDbInstanceIds.add(dbInstance.getDBInstanceArn());
            }
        }
        logger.info("Found {} DB instances without {} on AWS", untaggedDbInstanceIds.size(), tag);
        untaggedDbInstanceIds.forEach(logger::info);
        return untaggedDbInstanceIds;
    }

    private boolean hasTag(ListTagsForResourceResult response, String tag) {
        logger.debug(tag + " found: " + response.getTagList().stream().anyMatch(t -> t.getKey().equals(tag)));
        return response.getTagList().stream().anyMatch(t -> t.getKey().equals(tag));
    }
}
