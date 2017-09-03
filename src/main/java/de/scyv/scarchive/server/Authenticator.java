package de.scyv.scarchive.server;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.scyv.scarchive.model.entities.User;
import de.scyv.scarchive.model.repositories.UserRepository;

/**
 * Tool for authenticating a user.
 */
@Component
public class Authenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Authenticator.class);

    private final UserRepository userRepo;

    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * Create the instance.
     *
     * @param userRepo
     *            the user repo to use.
     */
    public Authenticator(final UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    /**
     * Check whether the current user is logged in (Request Scoped!)
     *
     * @return true if the current user is logged in, false otherwise.
     */
    public boolean isCurrentUserLoggedIn() {
        return CurrentUser.get() > 0l;
    }

    /**
     * Check whether current user is admin.
     *
     * @return true if user is admin, false otherwise.
     */
    public boolean isCurrentUserAdmin() {
        final User currentUser = userRepo.findOne(CurrentUser.get());
        return currentUser != null && currentUser.isAdmin();
    }

    /**
     * Log a user in.
     *
     * When login was is successful, the current user can be accessed via
     * {@link CurrentUser}s methods.
     *
     * @param userName
     *            the username of the user to log in
     * @param password
     *            the password of the user to log in
     * @return true, if the login was successful, false otherwise
     */
    public boolean login(final String userName, final String password) {
        final User user = userRepo.findByName(userName.toLowerCase());
        if (user == null) {
            return false;
        }
        if (!user.isActive()) {
            return false;
        }
        boolean loginSuccess = false;
        try {
            final String checkHash = createHash(password, user.getPasswordSalt());
            if (checkHash.equals(user.getPasswordHash())) {
                loginSuccess = true;
                CurrentUser.set(user.getId());
            }
        } catch (final NoSuchAlgorithmException ex) {
            LOGGER.error("Cannot hash password as the hash algorithm is not supported.", ex);
        }

        return loginSuccess;

    }

    /**
     * Create credentials for a given user and store them in the users object.
     *
     * @param user
     *            the userobject to create the credentials for
     * @param password
     *            the password
     * @throws NoSuchAlgorithmException
     *             when SHA-512 or SHA1PRNG is not supported.
     */
    public void createCredentials(final User user, final String password) throws NoSuchAlgorithmException {
        final String salt = createRandomSalt();
        final String hash = createHash(password, salt);
        user.setPasswordHash(hash);
        user.setPasswordSalt(new String(salt));
    }

    private String createHash(final String password, final String salt) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(salt.getBytes());
        final byte[] digest = md.digest(password.getBytes(UTF8));
        return String.format("%064x", new java.math.BigInteger(1, digest));
    }

    private String createRandomSalt() throws NoSuchAlgorithmException {
        final SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        final byte[] salt = new byte[256];
        sr.nextBytes(salt);
        return new String(salt);
    }

}
