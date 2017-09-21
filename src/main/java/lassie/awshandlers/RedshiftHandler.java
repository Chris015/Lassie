package lassie.awshandlers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.redshift.AmazonRedshift;
import com.amazonaws.services.redshift.AmazonRedshiftClientBuilder;
import com.amazonaws.services.redshift.model.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

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
        Tag tag = new Tag();
        tag.setKey(key);
        tag.setValue(value);
        CreateTagsRequest tagsRequest = new CreateTagsRequest();
        tagsRequest.withResourceName(id);
        tagsRequest.withTags(tag);
        redshift.createTags(tagsRequest);
    }

    public boolean clusterHasTag(String id, String tag) {
        List<Cluster> clustersWithoutTag = getClustersWithoutTag(tag);
        return clustersWithoutTag.stream().noneMatch(cluster -> cluster.getClusterIdentifier().equals(id));
    }

    private List<Cluster> getClustersWithoutTag(String tag) {
        log.info("Describing RedShift clusters");
        List<Cluster> clusters = new ArrayList<>();
        DescribeClustersRequest request = new DescribeClustersRequest();
        DescribeClustersResult response = redshift.describeClusters(request);
        for (Cluster cluster : response.getClusters()) {
            if (!hasTag(cluster, tag)) {
                clusters.add(cluster);
            }
        }
        log.info("Found " + clusters.size() + " RedShift clusters without + " + tag);
        return clusters;
    }

    private boolean hasTag(Cluster cluster, String tag) {
        log.trace(tag + " found: " + cluster.getTags().stream().anyMatch(t -> t.getKey().equals(tag)));
        return cluster.getTags().stream().anyMatch(t -> t.getKey().equals(tag));
    }
}
