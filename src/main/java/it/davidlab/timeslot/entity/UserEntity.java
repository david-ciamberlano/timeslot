package it.davidlab.timeslot.entity;


import javax.persistence.*;

@Entity
@Table(name = "USERS")
public class UserEntity {

    @Id @Column(length = 50, nullable = false) private String username;
    @Column(length = 50, nullable = false) private String password;
    @Column(nullable = false, columnDefinition = "TINYINT", length = 1) private boolean enabled;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "username", referencedColumnName = "username")
    private AccountEntity account;

    protected UserEntity(){}

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public AccountEntity getAccount() {
        return account;
    }



}
