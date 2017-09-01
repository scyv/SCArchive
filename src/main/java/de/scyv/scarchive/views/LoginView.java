package de.scyv.scarchive.views;

import javax.annotation.PostConstruct;

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

import de.scyv.scarchive.server.Authenticator;

@SpringView(name = LoginView.VIEW_NAME)
public class LoginView extends VerticalLayout implements ScarchiveView {

    public static final String VIEW_NAME = "";

    private static final long serialVersionUID = 1L;

    private final Authenticator authenticator;

    public LoginView(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public boolean loginNeeded() {
        return false;
    }

    @PostConstruct
    public void init() {

        final Label heading = new Label("Login");
        heading.addStyleName(ValoTheme.LABEL_H1);
        this.addComponent(heading);

        final FormLayout loginForm = new FormLayout();
        final TextField userName = new TextField("Benutzername:");
        final PasswordField password = new PasswordField("Passwort:");

        final Button loginButton = new Button("Login");

        final Button goToRegisterLink = new Button("Registrieren");
        goToRegisterLink.addStyleName(ValoTheme.BUTTON_LINK);

        loginForm.addComponents(userName, password, loginButton, goToRegisterLink);
        this.addComponent(loginForm);

        loginButton.setDisableOnClick(true);
        loginButton.setClickShortcut(KeyCode.ENTER);
        loginButton.addClickListener(event -> {
            try {
                if (authenticator.login(userName.getValue(), password.getValue())) {
                    getUI().getNavigator().navigateTo(DocumentsView.VIEW_NAME);
                } else {
                    password.clear();
                    userName.focus();
                    userName.selectAll();
                    Notification.show("Benutzername oder Password falsch. Bitte erneut versuchen.");
                }
            } finally {
                loginButton.setEnabled(true);
            }
        });

        goToRegisterLink.addClickListener(event -> {
            getUI().getNavigator().navigateTo(RegisterView.VIEW_NAME);
        });

        userName.focus();
    }
}
