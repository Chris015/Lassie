package lassie.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lassie.model.Log;

import java.util.ArrayList;
import java.util.List;

public class Account {
    private String name;
    private String ownerTag;
    private String accessKeyId;
    private String secretAccessKey;
    private String accountId;
    @JsonProperty("cloudTrailRoot")
    private S3Url s3Url;
    private String bucketRegion;
    private List<String> resourceTypes;
    private List<String> regions;
    private List<Log> logs = new ArrayList<>();

    public Account() {
    }

    public Account(String name, String ownerTag, String accessKeyId, String secretAccessKey, String accountId, S3Url s3Url, String bucketRegion, List<String> resourceTypes, List<String> regions) {
        this.name = name;
        this.ownerTag = ownerTag;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.accountId = accountId;
        this.s3Url = s3Url;
        this.bucketRegion = bucketRegion;
        this.resourceTypes = resourceTypes;
        this.regions = regions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addLog(Log log) {
        this.logs.add(log);
    }

    public List<Log> getLogs() {
        return this.logs;
    }

    public String getOwnerTag() {
        return ownerTag;
    }

    public void setOwnerTag(String ownerTag) {
        this.ownerTag = ownerTag;
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

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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

    public List<String> getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(List<String> resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public List<String> getRegions() {
        return regions;
    }

    public void setRegions(List<String> regions) {
        this.regions = regions;
    }
}