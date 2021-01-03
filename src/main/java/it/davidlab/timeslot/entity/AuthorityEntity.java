package it.davidlab.timeslot.entity;


import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "AUTHORITIES")
@IdClass(AuthorityId.class)
public class AuthorityEntity {

    @Id @Column(length = 50, nullable = false)
    private String username;

    @Id @Column(length = 50, nullable = false)
    private String authority;

    protected AuthorityEntity(){}

    public String getUsername() {
        return username;
    }

    public String getAuthority() {
        return authority;
    }
}

