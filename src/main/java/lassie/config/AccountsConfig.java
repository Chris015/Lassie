package lassie.config;

import java.util.ArrayList;
import java.util.List;

public class AccountsConfig {
    List<AccountConfig> accounts = new ArrayList<>();

    public AccountsConfig() {
    }

    public List<AccountConfig> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountConfig> accounts) {
        this.accounts = accounts;
    }
}
