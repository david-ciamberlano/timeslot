package it.davidlab.timeslot.repository;

import it.davidlab.timeslot.dao.AccountDao;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

public interface AccountRepo extends CrudRepository<AccountDao, String> {

    AccountDao getByUsername(String username);
}
