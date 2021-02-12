package it.davidlab.timeslot.dao;


import javax.persistence.*;

@Entity
@Table(name = "USERS")
public class UserDao {

    @Id @Column(length = 50, nullable = false)
    private String username;

    @Column(length = 80, nullable = false)
    private String password;

    @Column(nullable = false, columnDefinition = "TINYINT", length = 1)
    private boolean enabled;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "username", referencedColumnName = "username")
    private AccountDao account;

    protected UserDao(){}

    public UserDao(String username, String password, boolean enabled, AccountDao account) {
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.account = account;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public AccountDao getAccount() {
        return account;
    }



}
