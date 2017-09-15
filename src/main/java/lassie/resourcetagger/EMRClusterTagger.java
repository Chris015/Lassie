package lassie.resourcetagger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClientBuilder;
import com.amazonaws.services.elasticmapreduce.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.Log;
import lassie.config.Account;
import lassie.event.Event;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EMRClusterTagger implements ResourceTagger {
    private final Logger log = Logger.getLogger(EMRClusterTagger.class);
    private AmazonElasticMapReduce emr;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(List<Log> logs) {
        for (Log log : logs) {
            instantiateEmrInstance(log.getAccount());
            parseJson(log.getFilePaths());
            filterTaggedResources(log.getAccount().getOwnerTag());
            tag(log.getAccount().getOwnerTag());
        }
    }

    private void instantiateEmrInstance(Account account) {
        log.info("Instantiating EMR client");
        AWSCredentials awsCreds = new BasicAWSCredentials(account.getAccessKeyId(), account.getSecretAccessKey());
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        this.emr = AmazonElasticMapReduceClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(account.getRegions().get(0))
                .build();
        log.info("EMR client instantiated");
    }

    private void parseJson(List<String> filePaths) {
        log.info("Parsing json");
        String jsonPath = "$..Records[?(@.eventName == 'RunJobFlow' && @.responseElements != null)]";
        for (String filePath : filePaths) {
            try {
                String json = JsonPath.parse(new File(filePath))
                        .read(jsonPath)
                        .toString();
                GsonBuilder gsonBuilder = new GsonBuilder();
                JsonDeserializer<Event> deserializer = (jsonElement, type, context) -> {
                    String id = jsonElement
                            .getAsJsonObject().get("responseElements")
                            .getAsJsonObject().get("jobFlowId")
                            .getAsString();
                    String owner = jsonElement.getAsJsonObject().get("userIdentity")
                            .getAsJsonObject().get("arn")
                            .getAsString();
                    log.info("EMR cluster event created. Id: " + id + " Owner: " + owner);
                    return new Event(id, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createDBInstanceEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {}.getType());
                events.addAll(createDBInstanceEvents);
            } catch (IOException e) {
                log.error("Could not parse json", e);
                e.printStackTrace();
            }
        }
        log.info("Parsing json complete");
    }

    private void filterTaggedResources(String ownerTag) {
        log.info("Filtering tagged EMR Clusters");
        List<Cluster> clustersWithoutTags = describeClusters(ownerTag);
        List<Event> untaggedResources = new ArrayList<>();
        for (Cluster cluster : clustersWithoutTags) {
            for (Event event : events) {
                if (cluster.getId().equals(event.getId())) {
                    untaggedResources.add(event);
                }
            }
        }
        this.events = untaggedResources;
        for (Event event : events) {
            System.out.println(event.getId() + " " + event.getOwner());
        }
        log.info("Done filtering tagged EMR clusters");
    }

    private List<Cluster> describeClusters(String ownerTag) {
        log.info("Describing EMR clusters");
        List<Cluster> clusters = new ArrayList<>();
        ListClustersResult listClustersResult = emr.listClusters();
        for (ClusterSummary clusterSummary : listClustersResult.getClusters()) {
            DescribeClusterRequest request = new DescribeClusterRequest().withClusterId(clusterSummary.getId());
            DescribeClusterResult result = emr.describeCluster(request);
            if (isClusterActive(result.getCluster())) {
                if (!hasTag(result.getCluster(), ownerTag)) {
                    clusters.add(result.getCluster());
                }
            }
        }
        log.info("Done describing EMR clusters");
        return clusters;
    }

    private boolean isClusterActive(Cluster cluster) {
        log.info("Checking if cluster is active");
        String clusterState = cluster.getStatus().getState();
        if (clusterState.equals(ClusterState.TERMINATED.name())) {
            log.trace("Cluster " + cluster + " is terminated");
            return false;
        } else if (clusterState.equals(ClusterState.TERMINATED_WITH_ERRORS.name())) {
            log.trace("Cluster " + cluster + " is terminated with errors");
            return false;
        } else if (clusterState.equals(ClusterState.TERMINATING.name())) {
            log.trace("Cluster " + cluster + " is terminating");
            return false;
        }
        log.info("Cluster is active");
        return true;
    }

    private boolean hasTag(Cluster cluster, String ownerTag) {
        return cluster.getTags().stream().anyMatch(t -> t.getKey().equals(ownerTag));
    }

    private void tag(String ownerTag) {
        log.info("Tagging EMR clusters");
        for (Event event : events) {
            DescribeClusterRequest request = new DescribeClusterRequest().withClusterId(event.getId());
            DescribeClusterResult result = emr.describeCluster(request);
            List<Tag> tags = result.getCluster().getTags();
            tags.add(new Tag(ownerTag, event.getOwner()));
            AddTagsRequest tagsRequest = new AddTagsRequest(event.getId(), tags);
            emr.addTags(tagsRequest);
            log.info("Tagged: " + event.getId()
                    + " with key: " + ownerTag
                    + " value: " + event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Tagging EMR clusters complete");
    }
}

