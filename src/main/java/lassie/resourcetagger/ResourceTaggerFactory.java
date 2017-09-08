package lassie.resourcetagger;

public class ResourceTaggerFactory {

    public ResourceTagger getResourceTagger(String resourceType) throws UnsupportedResourceTypeException {
        switch (resourceType) {
            case "RunInstances": return new RunInstancesTagger();
            case "CreateSecurityGroup": return new CreateSecurityGroupTagger();
            case "S3Bucket": return new S3BucketTagger();
        }
        throw new UnsupportedResourceTypeException(resourceType + " is not a supported");
    }

}
