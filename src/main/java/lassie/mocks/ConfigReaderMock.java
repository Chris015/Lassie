package lassie.mocks;

import lassie.config.Account;
import lassie.config.ConfigReader;

import java.util.ArrayList;
import java.util.List;

public class ConfigReaderMock implements ConfigReader {
    public static List<Account> accounts = new ArrayList<>();

    @Override
    public List<Account> getAccounts() {
        return accounts;
    }

    @Override
    public boolean getDryRun() {
        return false;
    }
}
