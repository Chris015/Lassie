package lassie.mocks;

import java.util.List;

public class RedshiftHandlerMock implements lassie.awshandlers.RedshiftHandler {
    @Override
    public void instantiateRedshiftClient(String accessKeyId, String secretAccessKey, String region) {

    }

    @Override
    public void tagResource(String id, String key, String value) {

    }

    @Override
    public List<String> getIdsForUntaggedRedshiftClustersWithoutTag(String tag) {
        return null;
    }
}
