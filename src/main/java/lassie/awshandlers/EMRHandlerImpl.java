package lassie.awshandlers;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClientBuilder;
import com.amazonaws.services.elasticmapreduce.model.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static lassie.Application.DRY_RUN;

public class EMRHandlerImpl implements EMRHandler {
    private final static Logger log = Logger.getLogger(EMRHandlerImpl.class);
    private AmazonElasticMapReduce emr;

    public void instantiateEMRClient(String accessKeyId, String secretAccessKey, String region) {
        AWSCredentials basicCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(basicCredentials);
        this.emr = AmazonElasticMapReduceClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(region)
                .build();
    }

    public void tagResource(String id, String key, String value) {
        if (DRY_RUN) {
            log.info("Dry run: " + DRY_RUN + ". Did not tag: " + id + " with " + key + ": " + value);
            return;
        }
        DescribeClusterRequest request = new DescribeClusterRequest().withClusterId(id);
        DescribeClusterResult result = emr.describeCluster(request);
        List<Tag> tags = result.getCluster().getTags();
        tags.add(new Tag(key, value));
        AddTagsRequest tagsRequest = new AddTagsRequest(id, tags);
        emr.addTags(tagsRequest);
        log.info("Tagged: " + id + " with key: " + key + " value: " + value);
    }

    public List<String> getIdsForClustersWithoutTag(String tag) {
        log.info("Describing EMR clusters");
        List<String> untaggedClusterIds = new ArrayList<>();
        ListClustersResult listClustersResult = emr.listClusters();
        for (ClusterSummary clusterSummary : listClustersResult.getClusters()) {
            DescribeClusterRequest request = new DescribeClusterRequest().withClusterId(clusterSummary.getId());
            DescribeClusterResult result = emr.describeCluster(request);
            if (isClusterActive(result.getCluster())) {
                if (!hasTag(result.getCluster(), tag)) {
                    untaggedClusterIds.add(result.getCluster().getId());
                }
            }
        }
        log.info("Found " + untaggedClusterIds.size() + " clusters without " + tag);
        untaggedClusterIds.forEach(id -> log.info(id));
        return untaggedClusterIds;
    }

    private boolean isClusterActive(Cluster cluster) {
        String clusterState = cluster.getStatus().getState();
        if (clusterState.equals(ClusterState.TERMINATED.name())) {
            return false;
        } else if (clusterState.equals(ClusterState.TERMINATED_WITH_ERRORS.name())) {
            return false;
        } else if (clusterState.equals(ClusterState.TERMINATING.name())) {
            return false;
        }
        return true;
    }

    private boolean hasTag(Cluster cluster, String tag) {
        log.debug(tag + " found: " + cluster.getTags().stream().anyMatch(t -> t.getKey().equals(tag)));
        return cluster.getTags().stream().anyMatch(t -> t.getKey().equals(tag));
    }
}
