package lassie.config;

import java.util.List;

public class AccountsConfig {
    private boolean dryRun;
    private List<Account> accounts;

    public boolean isDryRun() {
        return dryRun;
    }

    public List<Account> getAccounts() {
        return accounts;
    }
}
