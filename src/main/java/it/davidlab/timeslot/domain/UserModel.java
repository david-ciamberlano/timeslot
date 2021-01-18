package it.davidlab.timeslot.domain;

public class UserModel {
    private String username;
    private String password;
    private String algoAddress;
    private String algoPassphrase;
    private boolean administrator;

    public UserModel(String username, String password, String algoAddress,
                     String algoPassphrase, boolean administrator) {
        this.username = username;
        this.password = password;
        this.algoAddress = algoAddress;
        this.algoPassphrase = algoPassphrase;
        this.administrator = administrator;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAlgoAddress() {
        return algoAddress;
    }

    public String getAlgoPassphrase() {
        return algoPassphrase;
    }

    public boolean isAdministrator() {
        return administrator;
    }
}
