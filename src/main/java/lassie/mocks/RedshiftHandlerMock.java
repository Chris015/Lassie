package lassie.mocks;

import com.amazonaws.services.redshift.model.Cluster;
import com.amazonaws.services.redshift.model.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RedshiftHandlerMock implements lassie.awshandlers.RedshiftHandler {
    private static final Logger logger = LogManager.getLogger(RedshiftHandlerMock.class);
    public static HashMap<String, Cluster> clusters = new HashMap<>();

    public RedshiftHandlerMock() {
        // tagged cluster, and an event is created
        Cluster cluster = new Cluster();
        cluster.setClusterIdentifier("arn:aws:redshift:ap-south-1:12345:cluster:r-109812b123a21");
        List<Tag> tags = new ArrayList<>();
        Tag tag = new Tag();
        tag.setKey("Owner");
        tag.setValue("jane.doe");
        tags.add(tag);
        cluster.setTags(tags);
        clusters.put(cluster.getClusterIdentifier(), cluster);

        // cluster without tag, and an event has been created
        cluster = new Cluster();
        cluster.setClusterIdentifier("arn:aws:redshift:ap-south-1:12345:cluster:r-203412c121a31");
        cluster.setTags(new ArrayList<>());
        clusters.put(cluster.getClusterIdentifier(), cluster);
    }

    @Override
    public void instantiateRedshiftClient(String accessKeyId, String secretAccessKey, String region) {

    }

    @Override
    public void tagResource(String id, String key, String value) {
        List<Tag> tags = new ArrayList<>();
        Tag tag = new Tag();
        tag.setKey(key);
        tag.setValue(value);
        tags.add(tag);

        Cluster cluster = clusters.get(id);
        cluster.setTags(tags);

        logger.info("Tagged: {} with key: {} value: {}", id, key, value);
    }

    @Override
    public List<String> getIdsForUntaggedRedshiftClustersWithoutTag(String tag) {
        List<String> clustersWithoutTag = new ArrayList<>();
        for (Cluster cluster : clusters.values()) {
            List<Tag> tags = cluster.getTags();
            if (tags.stream().noneMatch(t -> t.getKey().equals(tag))) {
                clustersWithoutTag.add(cluster.getClusterIdentifier());
            }
        }
        return clustersWithoutTag;
    }
}
