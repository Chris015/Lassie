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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EMRClusterTagger implements ResourceTagger {
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
        AWSCredentials awsCreds = new BasicAWSCredentials(account.getAccessKeyId(), account.getSecretAccessKey());
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        this.emr = AmazonElasticMapReduceClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(account.getRegions().get(0))
                .build();
    }

    private void parseJson(List<String> filePaths) {
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
                    return new Event(id, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createDBInstanceEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {}.getType());
                events.addAll(createDBInstanceEvents);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void filterTaggedResources(String ownerTag) {
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
    }

    private List<Cluster> describeClusters(String ownerTag) {
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
        return clusters;
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

    private boolean hasTag(Cluster cluster, String ownerTag) {
        return cluster.getTags().stream().noneMatch(t -> t.getKey().equals(ownerTag));
    }

    private void tag(String ownerTag) {
        for (Event event : events) {
            DescribeClusterRequest request = new DescribeClusterRequest().withClusterId(event.getId());
            DescribeClusterResult result = emr.describeCluster(request);
            List<Tag> tags = result.getCluster().getTags();
            tags.add(new Tag(ownerTag, event.getOwner()));
            AddTagsRequest tagsRequest = new AddTagsRequest(event.getId(), tags);
            emr.addTags(tagsRequest);
            System.out.println("Tagged: " + event.getId()
                    + " with key: " + ownerTag
                    + " value: " + event.getOwner());
        }
    }
}

