package lassie;

import lassie.config.Account;
import lassie.config.ConfigReader;
import lassie.resourcetagger.ResourceTagger;
import lassie.resourcetagger.ResourceTaggerFactory;
import lassie.resourcetagger.UnsupportedResourceTypeException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Application {
    private ConfigReader configReader;
    private LogHandler logHandler;
    private ResourceTaggerFactory resourceTaggerFactory;
    private String fromDate;

    public Application() {
        this.configReader = new ConfigReader();
        this.logHandler = new LogHandler();
        this.resourceTaggerFactory = new ResourceTaggerFactory();
    }

    public void run(String[] args) {
        setFromDate(args);
        List<Account> accounts = configReader.getAccounts();
        List<Log> logs = logHandler.getLogs(fromDate, accounts);
        List<ResourceTagger> resourceTaggers = getResourceTaggers(accounts);
        resourceTaggers.forEach(resourceTagger -> resourceTagger.tagResources(logs));
        logHandler.clearLogs();
    }

    private void setFromDate(String[] args) {
        this.fromDate = (args.length==1) ? args[0] : LocalDate.now().minusDays(2).toString();
    }

    private List<ResourceTagger> getResourceTaggers(List<Account> accounts) {
        List<ResourceTagger> resourceTaggers = new ArrayList<>();
        List<String> resourceTypes = new ArrayList<>();

        for (Account account : accounts) {
            resourceTypes.addAll(account.getResourceTypes());
        }
        try {
            for (String resourceType : resourceTypes) {
                resourceTaggers.add(resourceTaggerFactory.getResourceTagger(resourceType));
            }
        } catch (UnsupportedResourceTypeException e) {
            e.printStackTrace();
        }
        return resourceTaggers;
    }
}
