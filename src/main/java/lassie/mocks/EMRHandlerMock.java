package lassie.mocks;

import com.amazonaws.services.elasticmapreduce.model.Cluster;
import com.amazonaws.services.elasticmapreduce.model.Tag;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EMRHandlerMock implements lassie.awshandlers.EMRHandler {
    private final static Logger log = Logger.getLogger(EMRHandlerMock.class);
    public static HashMap<String,Cluster> clusters = new HashMap<>();

    public EMRHandlerMock() {
        // tagged cluster, and an event is created
        Cluster cluster = new Cluster();
        cluster.setId("j-123c12b123a12");
        List<Tag> tags = new ArrayList<>();
        Tag tag = new Tag("Owner", "jane.doe");
        tags.add(tag);
        cluster.setTags(tags);
        clusters.put(cluster.getId(), cluster);

        //cluster without tag, and an event is created
        cluster = new Cluster();
        cluster.setId("j-321a21c321b21");
        cluster.setTags(new ArrayList<>());
        clusters.put(cluster.getId(), cluster);
    }

    @Override
    public void instantiateEMRClient(String accessKeyId, String secretAccessKey, String region) {

    }

    @Override
    public void tagResource(String id, String key, String value) {
        Tag tag = new Tag(key, value);
        List<Tag> tags = new ArrayList<>();
        tags.add(tag);
        Cluster cluster = clusters.get(id);
        cluster.setTags(tags);

        clusters.put(cluster.getId(), cluster);
        log.info("Tagged: " + id + " with key: " + key + " value: " + value);
    }

    @Override
    public List<String> getIdsForClustersWithoutTag(String tag) {
        List<String> clustersWithoutTag = new ArrayList<>();
        for (Cluster cluster : clusters.values()) {
            List<Tag> tags = cluster.getTags();
            if (tags.stream().noneMatch(t -> t.getKey().equals(tag))) {
                clustersWithoutTag.add(cluster.getId());
            }
        }

        return clustersWithoutTag;
    }
}
