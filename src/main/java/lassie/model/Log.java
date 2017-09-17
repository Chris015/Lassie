package lassie.model;

import lassie.config.Account;

import java.util.List;

public class Log {
    private Account account;
    private List<String> filePaths;

    public Log(Account account, List<String> filePaths) {
        this.account = account;
        this.filePaths = filePaths;
    }

    public Account getAccount() {
        return account;
    }

    public List<String> getFilePaths() {
        return filePaths;
    }
}
