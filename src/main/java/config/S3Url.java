package config;

public class S3Url {
    private String bucket;
    private String key;

    public S3Url() {
    }

    public S3Url(String theUrl) {
        resolve(theUrl);
    }

    private void resolve(String theUrl) {
        if (theUrl == null || !theUrl.matches("s3://[a-zA-Z0-9-]+/([a-zA-Z0-9-=./_\\$]*)")) {
            throw new IllegalArgumentException("Malformed URL: " + theUrl);
        }

        int aPos = theUrl.indexOf("/", 5);

        this.bucket = theUrl.substring(5, aPos);
        this.key = theUrl.substring(aPos+1);
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }
}
