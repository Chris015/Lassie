package lassie.config;

import java.util.ArrayList;
import java.util.List;

public class Config {
    private boolean dryRun;
    private String ownerTag;
    private List<String> resourceTypes;
    private List<String> regions;
    private List<Account> accounts;

    public Config() {
        this.dryRun = true;
        this.resourceTypes = new ArrayList<>();
        this.regions = new ArrayList<>();
        this.accounts = new ArrayList<>();
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public String getOwnerTag() {
        return ownerTag;
    }

    public List<String> getResourceTypes() {
        return resourceTypes;
    }

    public List<String> getRegions() {
        return regions;
    }

    public List<Account> getAccounts() {
        return accounts;
    }
}
