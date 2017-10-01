package lassie.mocks;

import java.util.List;

public class EMRHandlerMock implements lassie.awshandlers.EMRHandler {
    @Override
    public void instantiateEMRClient(String accessKeyId, String secretAccessKey, String region) {

    }

    @Override
    public void tagResource(String id, String key, String value) {

    }

    @Override
    public List<String> getIdsForClustersWithoutTag(String tag) {
        return null;
    }
}
