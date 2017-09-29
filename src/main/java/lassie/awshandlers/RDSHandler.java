package lassie.awshandlers;

import com.amazonaws.services.rds.model.*;

import java.util.List;

public interface RDSHandler {
    void instantiateRDSClient(String accessKeyId, String secretAccessKey, String region);

    void tagResource(String id, String key, String value);

    List<String> getIdsForDBInstancesWithoutTag(String tag);

    boolean hasTag(ListTagsForResourceResult response, String tag);
}
