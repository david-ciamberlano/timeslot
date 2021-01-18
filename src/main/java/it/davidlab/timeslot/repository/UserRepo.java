package it.davidlab.timeslot.repository;

import it.davidlab.timeslot.dao.UserDao;
import org.springframework.data.repository.CrudRepository;

public interface UserRepo extends CrudRepository<UserDao, String> {

    UserDao getByUsername(String username);

}
