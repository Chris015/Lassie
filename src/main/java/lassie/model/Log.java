package lassie.model;

import java.util.List;

public class Log {
    private String region;
    private List<String> filePaths;

    public Log(String region, List<String> filePaths) {
        this.region = region;
        this.filePaths = filePaths;
    }

    public String getRegion() {
        return region;
    }

    public List<String> getFilePaths() {
        return filePaths;
    }
}
