package it.davidlab.timeslot.entity;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "ACCOUNT")
public class AccountEntity {


    @Id @Column(length = 50, nullable = false)
    private String username;
    @Column(length = 80, nullable = false) private String address;
    @Column(length = 250, nullable = false) private String passphrase;

    public AccountEntity() {
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
