package lassie.resourcetagger;

public class ResourceTaggerFactory {

    public ResourceTagger getResourceTagger(String resourceType) throws UnsupportedResourceTypeException {
        switch (resourceType.toUpperCase()) {
            case "RUNINSTANCES": return new RunInstancesTagger();
            case "CREATESECURITYGROUP": return new CreateSecurityGroupTagger();
            case "CREATEBUCKET": return new S3BucketTagger();
        }
        throw new UnsupportedResourceTypeException(resourceType + " is not a supported");
    }

}
