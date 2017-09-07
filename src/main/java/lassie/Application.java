package lassie;

import lassie.config.Account;
import lassie.config.ConfigReader;

import java.util.Date;
import java.util.List;

public class Application {
    private ConfigReader configReader;
    private LogHandler logHandler;
    private List<Account> accounts;
    private DateFormatter dateFormatter;
    private long dayInMilliseconds = 86400000;
    private String fromDate;

    public Application() {
        this.configReader = new ConfigReader();
        this.logHandler = new LogHandler();
        this.dateFormatter = new DateFormatter();
        this.fromDate = dateFormatter.format(new Date().getTime() - dayInMilliseconds * 2);
    }

    public void run() {
        accounts = configReader.getAccounts();
        List<Log> logs = logHandler.getLogs(fromDate, accounts);
        for (Log log : logs) {
            System.out.println(log.getAccount().getRegions().get(0) + "\n" + log.getFilePath());
        }

    }
}
