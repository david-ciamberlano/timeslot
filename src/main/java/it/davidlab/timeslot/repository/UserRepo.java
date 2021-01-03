package it.davidlab.timeslot.repository;

import it.davidlab.timeslot.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;

public interface UserRepo extends CrudRepository<UserEntity, String> {

    UserEntity getByUsername(String username);

}
