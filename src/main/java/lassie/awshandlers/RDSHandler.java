package lassie.awshandlers;

import java.util.List;

public interface RDSHandler {
    void instantiateRDSClient(String accessKeyId, String secretAccessKey, String region);

    void tagResource(String id, String key, String value);

    List<String> getIdsForDBInstancesWithoutTag(String tag);
}
