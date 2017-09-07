package lassie.resourcetagger;

import lassie.Log;

import java.util.List;

public interface ResourceTagger {
    void tagResources(List<Log> logs);
}
