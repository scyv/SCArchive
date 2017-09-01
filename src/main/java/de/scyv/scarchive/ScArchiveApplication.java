package de.scyv.scarchive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ScArchiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScArchiveApplication.class, args);
    }

    // @Component
    // public static class Fixture {
    // public Fixture(Authenticator authenticator, UserRepository userRepo) throws
    // NoSuchAlgorithmException {
    // final User user = new User();
    // user.setName("scyv");
    // authenticator.createCredentials(user, "test");
    // userRepo.save(user);
    // }
    // }

}
