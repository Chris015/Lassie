package lassie.mocks;

import java.util.List;

public class RDSHandlerMock implements lassie.awshandlers.RDSHandler {
    @Override
    public void instantiateRDSClient(String accessKeyId, String secretAccessKey, String region) {

    }

    @Override
    public void tagResource(String id, String key, String value) {

    }

    @Override
    public List<String> getIdsForDBInstancesWithoutTag(String tag) {
        return null;
    }
}
