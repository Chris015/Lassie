package lassie.model;

import java.util.List;

public class Log {
    private String region;
    private List<String> filePaths;
    private final String date;

    /**
     *
     * @param region the region cloud-trail logs are stored under in the S3Bucket
     * @param filePaths paths to the cloud-trail logs on your machine
     * @param date the date for which the cloud-trail logs were downloaded
     */
    public Log(String region, List<String> filePaths, String date) {
        this.region = region;
        this.filePaths = filePaths;
        this.date = date;
    }

    public String getRegion() {
        return region;
    }

    public List<String> getFilePaths() {
        return filePaths;
    }

    public String getDate() {
        return date;
    }
}
