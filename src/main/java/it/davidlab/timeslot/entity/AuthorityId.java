package it.davidlab.timeslot.entity;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class AuthorityId implements Serializable {
    private String username;
    private String authority;
}
