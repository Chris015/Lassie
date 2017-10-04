package lassie;

import lassie.config.Account;
import lassie.model.Log;

import java.util.List;

public interface LogFetcher {
    void addLogsToAccount(String startDate, List<Account> accounts);

    void createTmpFolders();

    void clearLogs();
}
