package lassie.awshandlers;

import java.util.List;

public interface EMRHandler {
    void instantiateEMRClient(String accessKeyId, String secretAccessKey, String region);

    void tagResource(String id, String key, String value);

    List<String> getIdsForClustersWithoutTag(String tag);
}
