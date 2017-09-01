package de.scyv.scarchive.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Viewport;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.annotation.SpringViewDisplay;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

import de.scyv.scarchive.server.Authenticator;
import de.scyv.scarchive.views.LoginView;
import de.scyv.scarchive.views.ScarchiveView;

@Theme("scarchive")
@SpringUI()
@Viewport("user-scalable=no,initial-scale=1.0")
@SpringViewDisplay
public class SCArchiveUi extends UI implements ViewDisplay {

    private static final Logger LOGGER = LoggerFactory.getLogger(SCArchiveUi.class);

    private static final long serialVersionUID = 1L;

    private final Authenticator authenticator;

    public SCArchiveUi(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Value("${scarchive.openlocal}")
    private Boolean openlocal;

    @Override
    public void init(VaadinRequest request) {
        getPage().setTitle("SCArchive");
        if (!authenticator.isCurrentUserLoggedIn()) {
            getNavigator().navigateTo(LoginView.VIEW_NAME);
        }
    }

    @Override
    public void showView(View view) {
        if (view instanceof ScarchiveView) {
            if (!((ScarchiveView) view).loginNeeded() || authenticator.isCurrentUserLoggedIn()) {
                setContent((Component) view);
            } else {
                LOGGER.warn("User is not logged in. Redirecting to login page.");
                getNavigator().navigateTo(LoginView.VIEW_NAME);
            }

        } else {
            LOGGER.error("All views must implement " + ScarchiveView.class.getName());
        }
    }

}
