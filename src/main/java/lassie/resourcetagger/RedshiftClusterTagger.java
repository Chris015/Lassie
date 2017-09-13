package lassie.resourcetagger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.redshift.AmazonRedshift;
import com.amazonaws.services.redshift.AmazonRedshiftClientBuilder;
import com.amazonaws.services.redshift.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.Log;
import lassie.event.Event;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RedshiftClusterTagger implements ResourceTagger {
    private AmazonRedshift redshift;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(List<Log> logs) {
        for (Log log : logs) {
            instantiateRedshiftClient(log);
            parseJson(log, log.getFilePaths());
            filterTaggedResources(log.getAccount().getOwnerTag());
            tag(log.getAccount().getOwnerTag());
        }
    }

    private void instantiateRedshiftClient(Log log) {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(log.getAccount().getAccessKeyId(),
                log.getAccount().getSecretAccessKey());
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        redshift = AmazonRedshiftClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(log.getAccount().getRegions().get(0))
                .build();
    }

    private void parseJson(Log log, List<String> filePaths) {
        String jsonPath = "$..Records[?(@.eventName == 'CreateCluster' && @.responseElements != null)]";
        for (String filePath : filePaths) {
            try {
                String json = JsonPath.parse(new File(filePath)).read(jsonPath).toString();
                GsonBuilder gsonBuilder = new GsonBuilder();
                JsonDeserializer<Event> deserializer = (jsonElement, type, context) -> {
                    String clusterId = jsonElement
                            .getAsJsonObject().get("requestParameters")
                            .getAsJsonObject().get("clusterIdentifier")
                            .getAsString();
                    String arn = "arn:aws:redshift:"
                            + log.getAccount().getRegions().get(0) + ":"
                            + log.getAccount().getAccountId() + ":cluster:"
                            + clusterId;
                    String owner = jsonElement
                            .getAsJsonObject().get("userIdentity")
                            .getAsJsonObject().get("arn")
                            .getAsString();
                    return new Event(arn, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createClusterEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createClusterEvents);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<Cluster> describeCluster(String ownerTag) {
        List<Cluster> clusters = new ArrayList<>();
        DescribeClustersRequest request = new DescribeClustersRequest();
        DescribeClustersResult response = redshift.describeClusters(request);
        for (Cluster cluster : response.getClusters()) {
            if (!hasTag(cluster, ownerTag)) {
                clusters.add(cluster);
            }
        }
        return clusters;
    }

    private boolean hasTag(Cluster cluster, String tag) {
        return cluster.getTags().stream().noneMatch(t -> t.getKey().equals(tag));
    }

    private void filterTaggedResources(String ownerTag) {
        List<Event> untaggedEvents = new ArrayList<>();
        List<Cluster> clustersWithoutTag = describeCluster(ownerTag);
        for (Cluster cluster : clustersWithoutTag) {
            for (Event event : events) {
                String clusterId = cluster.getClusterIdentifier();
                String eventId = event.getId();
                eventId = eventId.substring(eventId.lastIndexOf(':') + 1, eventId.length());
                if (clusterId.equals(eventId)) {
                    untaggedEvents.add(event);
                }
            }
        }
        this.events = untaggedEvents;
    }

    private void tag(String ownerTag) {
        for (Event event : events) {
            Tag tag = new Tag();
            tag.setKey(ownerTag);
            tag.setValue(event.getOwner());
            CreateTagsRequest tagsRequest = new CreateTagsRequest();
            tagsRequest.withResourceName(event.getId());
            tagsRequest.withTags(tag);
            redshift.createTags(tagsRequest);
            System.out.println("Tagged: " + event.getId() +
                    " with key: " + ownerTag +
                    " value: " + event.getOwner());
        }
    }

}
