package de.scyv.scarchive.views;

import java.security.NoSuchAlgorithmException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import de.scyv.scarchive.model.entities.User;
import de.scyv.scarchive.model.repositories.UserRepository;
import de.scyv.scarchive.server.Authenticator;

@SpringView(name = RegisterView.VIEW_NAME)
public class RegisterView extends VerticalLayout implements ScarchiveView {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterView.class);

    public static final String VIEW_NAME = "register";

    private static final long serialVersionUID = 1L;

    private final Authenticator authenticator;

    private final UserRepository userRepo;

    private final TextField userName = new TextField("Benutzername:");
    private final PasswordField password = new PasswordField("Passwort:");
    private final PasswordField passwordRepeat = new PasswordField("Passwort Wiederholen:");

    public RegisterView(Authenticator authenticator, UserRepository userRepo) {
        this.authenticator = authenticator;
        this.userRepo = userRepo;
    }

    @Override
    public boolean loginNeeded() {
        return false;
    }

    @PostConstruct
    public void init() {

        final Label heading = new Label("Registrierung");
        heading.addStyleName(ValoTheme.LABEL_H1);
        this.addComponent(heading);

        final FormLayout loginForm = new FormLayout();

        final Button registerButton = new Button("Benutzerkonto anlegen");

        loginForm.addComponents(userName, password, passwordRepeat, registerButton);
        this.addComponent(loginForm);

        registerButton.setDisableOnClick(true);
        registerButton.setClickShortcut(KeyCode.ENTER);
        registerButton.addClickListener(event -> {
            try {
                final String normalizedUserName = userName.getValue().trim().toLowerCase();

                if (userAlreadyExist(normalizedUserName)) {
                    Notification.show("Der gewählte Benutzername ist ungültig oder existiert bereits.");
                    return;
                }

                if (!passwordsEqual()) {
                    Notification.show("Die Passwörter stimmen nicht überein. Bitte versuchen Sie es erneut.");
                    return;
                }

                createNewUser(normalizedUserName);
                getUI().getNavigator().navigateTo(DocumentsView.VIEW_NAME);

            } catch (final NoSuchAlgorithmException nsae) {
                LOGGER.error("Cannot create credentials", nsae);
            } finally {
                registerButton.setEnabled(true);
            }
        });

        userName.focus();
    }

    private void createNewUser(String normalizedUserName) throws NoSuchAlgorithmException {
        final User user = new User();
        user.setName(normalizedUserName);
        authenticator.createCredentials(user, password.getValue());
        user.setActive(false);
        if (userRepo.count() == 0) {
            user.setAdmin(true);
            user.setActive(true);
        }
        userRepo.save(user);
    }

    private boolean passwordsEqual() {
        return password.getValue().equals(passwordRepeat.getValue());
    }

    private boolean userAlreadyExist(String normalizedUserName) {
        return userRepo.findByName(normalizedUserName) != null;
    }
}
