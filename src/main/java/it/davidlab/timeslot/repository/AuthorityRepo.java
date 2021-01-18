package it.davidlab.timeslot.repository;

import it.davidlab.timeslot.dao.AuthorityDao;
import org.springframework.data.repository.CrudRepository;

public interface AuthorityRepo extends CrudRepository<AuthorityDao, String> {

}
