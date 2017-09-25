package lassie.awshandlers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.redshift.AmazonRedshift;
import com.amazonaws.services.redshift.AmazonRedshiftClientBuilder;
import com.amazonaws.services.redshift.model.*;
import lassie.Application;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static lassie.Application.DRY_RUN;

public class RedshiftHandler {
    private static final Logger log = Logger.getLogger(RedshiftHandler.class);
    private AmazonRedshift redshift;

    public void instantiateRedshiftClient(String accessKeyId, String secretAccessKey, String region) {
        log.info("Instantiating RedShift client");
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        this.redshift = AmazonRedshiftClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(region)
                .build();
        log.info("RedShift client instantiated");
    }

    public void tagResource(String id, String key, String value) {
        if (DRY_RUN) {
            log.info("Dry run: " + DRY_RUN + ". Did not tag: " + id + " with " + key + ": " + value);
            return;
        }
        Tag tag = new Tag();
        tag.setKey(key);
        tag.setValue(value);
        CreateTagsRequest tagsRequest = new CreateTagsRequest();
        tagsRequest.withResourceName(id);
        tagsRequest.withTags(tag);
        redshift.createTags(tagsRequest);
        log.info("Tagged: " + id + " with key: " + key + " value: " + value);
    }

    public List<String> getIdsForUntaggedRedshiftClustersWithoutTag(String tag) {
        log.info("Describing RedShift clusters");
        List<String> untaggedCluserIds = new ArrayList<>();
        DescribeClustersRequest request = new DescribeClustersRequest();
        DescribeClustersResult response = redshift.describeClusters(request);
        for (Cluster cluster : response.getClusters()) {
            if (!hasTag(cluster, tag)) {
                untaggedCluserIds.add(cluster.getClusterIdentifier());
            }
        }
        log.info("Found " + untaggedCluserIds.size() + " RedShift clusters without + " + tag);
        return untaggedCluserIds;
    }

    private boolean hasTag(Cluster cluster, String tag) {
        log.trace(tag + " found: " + cluster.getTags().stream().anyMatch(t -> t.getKey().equals(tag)));
        return cluster.getTags().stream().anyMatch(t -> t.getKey().equals(tag));
    }
}
