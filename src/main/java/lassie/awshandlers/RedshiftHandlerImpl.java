package lassie.awshandlers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.redshift.AmazonRedshift;
import com.amazonaws.services.redshift.AmazonRedshiftClientBuilder;
import com.amazonaws.services.redshift.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static lassie.Application.DRY_RUN;

public class RedshiftHandlerImpl  implements RedshiftHandler {
    private static final Logger logger = LogManager.getLogger(RedshiftHandlerImpl.class);
    private AmazonRedshift redshift;

    public void instantiateRedshiftClient(String accessKeyId, String secretAccessKey, String region) {
        logger.info("Instantiating Redshift client in region: {}", region);
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        this.redshift = AmazonRedshiftClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(region)
                .build();
        logger.info("RedShift client instantiated");
    }

    public void tagResource(String id, String key, String value) {
        if (DRY_RUN) {
            logger.info("Dry run: {}. Did not tag: {} with {}: {}", DRY_RUN, id, key, value);
            return;
        }
        Tag tag = new Tag();
        tag.setKey(key);
        tag.setValue(value);
        CreateTagsRequest tagsRequest = new CreateTagsRequest();
        tagsRequest.withResourceName(id);
        tagsRequest.withTags(tag);
        redshift.createTags(tagsRequest);
        logger.info("Tagged: {} with key: {} value: {}", id, key, value);
    }

    public List<String> getIdsForUntaggedRedshiftClustersWithoutTag(String tag) {
        logger.info("Describing RedShift clusters");
        List<String> untaggedCluserIds = new ArrayList<>();
        DescribeClustersRequest request = new DescribeClustersRequest();
        DescribeClustersResult response = redshift.describeClusters(request);
        for (Cluster cluster : response.getClusters()) {
            if (!hasTag(cluster, tag)) {
                untaggedCluserIds.add(cluster.getClusterIdentifier());
            }
        }
        logger.info("Found {} RedShift clusters without {}", untaggedCluserIds.size(), tag);
        untaggedCluserIds.forEach(logger::info);
        return untaggedCluserIds;
    }

    private boolean hasTag(Cluster cluster, String tag) {
        logger.debug(tag + " found: " + cluster.getTags().stream().anyMatch(t -> t.getKey().equals(tag)));
        return cluster.getTags().stream().anyMatch(t -> t.getKey().equals(tag));
    }
}
