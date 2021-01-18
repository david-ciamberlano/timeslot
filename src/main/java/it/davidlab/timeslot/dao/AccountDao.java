package it.davidlab.timeslot.dao;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "ACCOUNT")
public class AccountDao {


    @Id @Column(length = 50, nullable = false)
    private String username;
    @Column(length = 80, nullable = false) private String address;
    @Column(length = 250, nullable = false) private String passphrase;

    public AccountDao() {
    }

    public AccountDao(String username, String address, String passphrase) {
        this.username = username;
        this.address = address;
        this.passphrase = passphrase;
    }

    public String getUsername() {
        return username;
    }

    public String getAddress() {
        return address;
    }

    public String getPassphrase() {
        return passphrase;
    }
}
