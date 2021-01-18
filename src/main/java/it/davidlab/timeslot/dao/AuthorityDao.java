package it.davidlab.timeslot.dao;


import javax.persistence.*;

@Entity
@Table(name = "AUTHORITIES")
@IdClass(AuthorityId.class)
public class AuthorityDao {

    @Id @Column(length = 50, nullable = false)
    private String username;

    @Id @Column(length = 50, nullable = false)
    private String authority;

    public AuthorityDao(){}

    public AuthorityDao(String username, String authority) {
        this.username = username;
        this.authority = authority;
    }

    public String getUsername() {
        return username;
    }

    public String getAuthority() {
        return authority;
    }
}

