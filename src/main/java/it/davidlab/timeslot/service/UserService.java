package it.davidlab.timeslot.service;

import it.davidlab.timeslot.dao.AccountDao;
import it.davidlab.timeslot.dao.AuthorityDao;
import it.davidlab.timeslot.dao.UserDao;
import it.davidlab.timeslot.domain.UserModel;
import it.davidlab.timeslot.repository.AccountRepo;
import it.davidlab.timeslot.repository.AuthorityRepo;
import it.davidlab.timeslot.repository.UserRepo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private AccountRepo accountRepo;
    private UserRepo userRepo;
    private AuthorityRepo authorityRepo;

    public UserService(UserRepo userRepo, AccountRepo accountRepo, AuthorityRepo authorityRepo) {
        this.accountRepo = accountRepo;
        this.userRepo = userRepo;
        this.authorityRepo = authorityRepo;
    }

    @Transactional
    public void createUser(UserModel userModel) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String encodedPassword = encoder.encode(userModel.getPassword());

        AccountDao account = new AccountDao(userModel.getUsername(),
                userModel.getAlgoAddress(), userModel.getAlgoPassphrase());
        accountRepo.save(account);

        UserDao user = new UserDao(userModel.getUsername(), encodedPassword, true, account);
        userRepo.save(user);

        String authority = userModel.isAdministrator() ? "ROLE_ADMIN" : "ROLE_USER";
        authorityRepo.save(new AuthorityDao(userModel.getUsername(), authority));

    }
}
