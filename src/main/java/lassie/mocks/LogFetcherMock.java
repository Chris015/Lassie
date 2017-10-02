package lassie.mocks;

import lassie.LogFetcher;
import lassie.config.Account;
import lassie.model.Log;

import java.util.ArrayList;
import java.util.List;

public class LogFetcherMock implements LogFetcher {

    @Override
    public void addLogsToAccount(String startDate, List<Account> accounts) {}

    @Override
    public void createTmpFolders() {

    }

    @Override
    public void clearLogs() {

    }
}
