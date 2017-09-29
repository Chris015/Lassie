package lassie.awshandlers;

import java.util.List;

public interface RedshiftHandler {
    void instantiateRedshiftClient(String accessKeyId, String secretAccessKey, String region);

    void tagResource(String id, String key, String value);

    List<String> getIdsForUntaggedRedshiftClustersWithoutTag(String tag);
}
