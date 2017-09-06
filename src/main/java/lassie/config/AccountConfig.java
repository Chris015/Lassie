package lassie.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AccountConfig {
    private String accessKeyId;
    private String secretAccessKey;
    @JsonProperty("cloudTrailRoot")
    private S3Url s3Url;
    private String bucketRegion;
    private List<String> regions;
    private List<EventConfig> events;

    public AccountConfig() {
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public S3Url getS3Url() {
        return s3Url;
    }

    public void setS3Url(S3Url s3Url) {
        this.s3Url = s3Url;
    }

    public String getBucketRegion() {
        return bucketRegion;
    }

    public void setBucketRegion(String bucketRegion) {
        this.bucketRegion = bucketRegion;
    }

    public List<String> getRegions() {
        return regions;
    }

    public void setRegions(List<String> regions) {
        this.regions = regions;
    }

    public List<EventConfig> getEvents() {
        return events;
    }

    public void setEvents(List<EventConfig> events) {
        this.events = events;
    }
}
