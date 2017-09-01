package de.scyv.scarchive.views;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import de.scyv.scarchive.model.repositories.UserRepository;
import de.scyv.scarchive.server.Authenticator;

/**
 * TODO this view is not accessible directly. Maybe we do not need it at all.
 * TODO How do we SYNC this settings then?
 *
 *
 */
@SpringView(name = AdminView.VIEW_NAME)
public class AdminView extends VerticalLayout implements ScarchiveView {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminView.class);

    public static final String VIEW_NAME = "admin";

    private static final long serialVersionUID = 1L;

    @Value("${scarchive.documentPaths}")
    private String documentPaths;

    private final UserRepository userRepo;

    private final Authenticator authenticator;

    public AdminView(Authenticator authenticator, UserRepository userRepo) {
        this.authenticator = authenticator;
        this.userRepo = userRepo;
    }

    @PostConstruct
    public void init() {
        final Label heading = new Label("Administration");
        heading.addStyleName(ValoTheme.LABEL_H1);
        this.addComponent(heading);

        createUserPermissionMatrix();
    }

    @Override
    public void enter(ViewChangeEvent event) {
        if (!authenticator.isCurrentUserAdmin()) {
            getUI().getNavigator().navigateTo(LoginView.VIEW_NAME);
        }
    }

    @Override
    public boolean loginNeeded() {
        return true;
    }

    private void createUserPermissionMatrix() {
        userRepo.findAll().forEach(user -> {
            final HorizontalLayout row = new HorizontalLayout();
            final Label userName = new Label(user.getName());
            row.addComponent(userName);
            for (final String path : documentPaths.split(";")) {
                final CheckBox permissionCb = new CheckBox(path);
                row.addComponent(permissionCb);
            }
            this.addComponent(row);
        });
    }

}
