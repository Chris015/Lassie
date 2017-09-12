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

public class ClusterTagger implements ResourceTagger {
    private AmazonRedshift redshiftClient;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(List<Log> logs) {
        for (Log log : logs) {
            instantiateRedshiftClient(log);
            parseJson(log, log.getFilePaths());
            filterTaggedResources(log);
            tag(log);
        }
    }

    private void instantiateRedshiftClient(Log log) {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(log.getAccount().getAccessKeyId(), log.getAccount().getSecretAccessKey());
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        redshiftClient = AmazonRedshiftClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(log.getAccount().getRegions().get(0))
                .build();
    }

    private void parseJson(Log log, List<String> filePaths) {
        for (String filePath : filePaths) {
            try {
                String json = JsonPath.parse(new File(filePath))
                        .read("$..Records[?(@.eventName == 'CreateCluster' && @.responseElements != null)]")
                        .toString();
                GsonBuilder gsonBuilder = new GsonBuilder();
                JsonDeserializer<Event> deserializer = (jsonElement, type, context) -> {
                    String clusterId = jsonElement
                            .getAsJsonObject().get("requestParameters")
                            .getAsJsonObject().get("clusterIdentifier").getAsString();
                    String arn = "arn:aws:redshift:"
                            + log.getAccount().getRegions().get(0) + ":"
                            + log.getAccount().getAccountId() + ":cluster:"
                            + clusterId;
                    String owner = jsonElement.getAsJsonObject().get("userIdentity").getAsJsonObject().get("arn").getAsString();
                    return new Event(arn, owner);

                };

                gsonBuilder.registerTypeAdapter(Event.class, deserializer);

                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createSecurityGroups = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createSecurityGroups);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<Cluster> describeCluster(Log log) {
        List<Cluster> clusters = new ArrayList<>();

        DescribeClustersRequest request = new DescribeClustersRequest();
        DescribeClustersResult response = redshiftClient.describeClusters(request);

        for (Cluster cluster : response.getClusters()) {
            if (cluster.getTags().stream().noneMatch(t -> t.getKey().equals(log.getAccount().getOwnerTag()))) {
                clusters.add(cluster);
            }
        }
        return clusters;
    }

    private void filterTaggedResources(Log log) {
        List<Event> untaggedEvents = new ArrayList<>();
        List<Cluster> clustersWithoutTags = describeCluster(log);

        for (Cluster clustersWithoutTag : clustersWithoutTags) {
            for (Event event : events) {
                String clusterId = clustersWithoutTag.getClusterIdentifier();
                String eventId = event.getId();
                eventId = eventId.substring(eventId.lastIndexOf(':') + 1, eventId.length());
                if (clusterId.equals(eventId)) {
                    untaggedEvents.add(event);
                }
            }
        }
        this.events = untaggedEvents;
    }

    private void tag(Log log) {
        for (Event event : events) {
            String ownerTag = log.getAccount().getOwnerTag();
            String owner = event.getOwner();
            Tag tag = new Tag();
            tag.setKey(ownerTag);
            tag.setValue(owner);
            CreateTagsRequest tagsRequest = new CreateTagsRequest();
            tagsRequest.withResourceName(event.getId());
            tagsRequest.withTags(tag);
            redshiftClient.createTags(tagsRequest);
            System.out.println("Tagged: " + event.getId() +
                    " with key: " + log.getAccount().getOwnerTag() +
                    " value: " + event.getOwner());
        }
    }

}
