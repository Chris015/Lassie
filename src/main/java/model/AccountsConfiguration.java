package model;

import java.util.List;

public class AccountsConfiguration {

    private List<Account> accounts;

    public AccountsConfiguration() {
    }

    public AccountsConfiguration(List<Account> accounts) {
        this.accounts = accounts;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }
}
