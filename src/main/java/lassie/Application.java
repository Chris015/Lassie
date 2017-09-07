package lassie;

import lassie.config.Account;
import lassie.config.ConfigReader;
import lassie.resourcetagger.ResourceTagger;
import lassie.resourcetagger.ResourceTaggerFactory;
import lassie.resourcetagger.UnsupportedResourceTypeException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Application {
    private ConfigReader configReader;
    private LogHandler logHandler;
    private List<Account> accounts;
    private DateFormatter dateFormatter;
    private long dayInMilliseconds = 86400000;
    private String fromDate;
    private ResourceTaggerFactory resourceTaggerFactory;

    public Application() {
        this.configReader = new ConfigReader();
        this.logHandler = new LogHandler();
        this.dateFormatter = new DateFormatter();
        this.fromDate = dateFormatter.format(new Date().getTime() - dayInMilliseconds * 1);
        this.resourceTaggerFactory = new ResourceTaggerFactory();
    }

    public void run() {
        accounts = configReader.getAccounts();
        List<Log> logs = logHandler.getLogs(fromDate, accounts);
        List<String> resourceTypes = new ArrayList<>();
        accounts.forEach(account -> resourceTypes.addAll(account.getResourceTypes()));
        List<ResourceTagger> resourceTaggers = new ArrayList<>();
        resourceTypes.forEach(resourceType -> {
            try {
                resourceTaggers.add(resourceTaggerFactory.getResourceTagger(resourceType));
            } catch (UnsupportedResourceTypeException e) {
                e.printStackTrace();
            }
        });

        for (ResourceTagger resourceTagger : resourceTaggers) {
            resourceTagger.tagResources(logs);
        }


    }
}
