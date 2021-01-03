package it.davidlab.timeslot.repository;

import it.davidlab.timeslot.entity.AccountEntity;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepo extends CrudRepository<AccountEntity, String> {

    AccountEntity getByUsername(String username);
}
