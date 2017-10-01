package lassie;

import lassie.config.Account;
import lassie.model.Log;

import java.util.List;

public interface LogFetcher {
    List<Log> getLogs(String startDate, List<Account> accounts);

    void createTmpFolders();

    void clearLogs();
}
