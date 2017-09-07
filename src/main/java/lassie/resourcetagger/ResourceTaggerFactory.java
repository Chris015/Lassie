package lassie.resourcetagger;

public class ResourceTaggerFactory {

    public ResourceTagger getResourceTagger(String resourceType) throws UnsupportedResourceTypeException {
        switch (resourceType) {
            case "RunInstances": return new RunInstancesTagger();
        }
        throw new UnsupportedResourceTypeException(resourceType + " is not a supported");
    }

}
