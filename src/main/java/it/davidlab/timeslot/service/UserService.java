package it.davidlab.timeslot.service;

import it.davidlab.timeslot.dao.AccountDao;
import it.davidlab.timeslot.dao.AuthorityDao;
import it.davidlab.timeslot.dao.UserDao;
import it.davidlab.timeslot.domain.UserModel;
import it.davidlab.timeslot.repository.AccountRepo;
import it.davidlab.timeslot.repository.AuthorityRepo;
import it.davidlab.timeslot.repository.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    private AccountRepo accountRepo;
    private UserRepo userRepo;
    private AuthorityRepo authorityRepo;

    private AlgoService algoService;

    public UserService(UserRepo userRepo, AccountRepo accountRepo,
                       AuthorityRepo authorityRepo, AlgoService algoService) {
        this.accountRepo = accountRepo;
        this.userRepo = userRepo;
        this.authorityRepo = authorityRepo;
        this.algoService = algoService;
    }

    @Transactional
    public void createUser(UserModel userModel) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String encodedPassword = encoder.encode(userModel.getPassword());

        AccountDao account;
        if (userModel.getAlgoAddress().isBlank()) {
            try {
                account = algoService.getNewAccount(userModel.getUsername());
            }
            catch (Exception e) {
                //TODO manage exception in a better way
                logger.error(e.getMessage());
                throw new IllegalStateException("Can't create the new account");
            }
        }
        else {
            account = new AccountDao(userModel.getUsername(),
                    userModel.getAlgoAddress(), userModel.getAlgoPassphrase());
        }


        accountRepo.save(account);

        UserDao user = new UserDao(userModel.getUsername(), encodedPassword, true, account);
        userRepo.save(user);

        String authority = userModel.isAdministrator() ? "ROLE_ADMIN" : "ROLE_USER";
        authorityRepo.save(new AuthorityDao(userModel.getUsername(), authority));

    }
}
