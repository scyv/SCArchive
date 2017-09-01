package de.scyv.scarchive.server;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;

/**
 * Class for retrieving and setting the name of the current user of the current
 * session. All methods of this class require that a {@link VaadinRequest} is
 * bound to the current thread.
 *
 * @see com.vaadin.server.VaadinService#getCurrentRequest()
 */
public final class CurrentUser {

    /**
     * The attribute key used to store the username in the session.
     */
    public static final String CURRENT_USER_SESSION_ATTRIBUTE_KEY = CurrentUser.class.getCanonicalName();

    private CurrentUser() {
    }

    /**
     * Returns the name of the current user stored in the current session, or -1 if
     * no user name is stored.
     *
     * @throws IllegalStateException
     *             if the current session cannot be accessed.
     */
    public static Long get() {
        final Long currentUser = (Long) getCurrentRequest().getWrappedSession()
                .getAttribute(CURRENT_USER_SESSION_ATTRIBUTE_KEY);
        if (currentUser == null) {
            return -1L;
        } else {
            return currentUser;
        }
    }

    /**
     * Sets the name of the current user and stores it in the current session. Using
     * a {@code null} username will remove the username from the session.
     *
     * @throws IllegalStateException
     *             if the current session cannot be accessed.
     */
    public static void set(Long currentUser) {
        if (currentUser == null) {
            getCurrentRequest().getWrappedSession().removeAttribute(CURRENT_USER_SESSION_ATTRIBUTE_KEY);
        } else {
            getCurrentRequest().getWrappedSession().setAttribute(CURRENT_USER_SESSION_ATTRIBUTE_KEY, currentUser);
        }
    }

    private static VaadinRequest getCurrentRequest() {
        final VaadinRequest request = VaadinService.getCurrentRequest();
        if (request == null) {
            throw new IllegalStateException("No request bound to current thread");
        }
        return request;
    }
}
