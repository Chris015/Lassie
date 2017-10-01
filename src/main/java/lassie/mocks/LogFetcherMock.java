package lassie.mocks;

import lassie.LogFetcher;
import lassie.config.Account;
import lassie.model.Log;

import java.util.ArrayList;
import java.util.List;

public class LogFetcherMock implements LogFetcher {
    public static List<Log> logs = new ArrayList<>();

    @Override
    public List<Log> getLogs(String startDate, List<Account> accounts) {
        return logs;
    }

    @Override
    public void createTmpFolders() {

    }

    @Override
    public void clearLogs() {

    }
}
