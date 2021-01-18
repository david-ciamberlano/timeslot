package it.davidlab.timeslot.dao;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class AuthorityId implements Serializable {
    private String username;
    private String authority;
}
