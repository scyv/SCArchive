package de.scyv.scarchive.ui;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.annotations.Theme;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FileResource;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import de.scyv.scarchive.search.DocumentFinder;
import de.scyv.scarchive.search.Finding;
import de.scyv.scarchive.server.MetaData;

@Theme("valo")
@SpringUI(path = "")
public class SCArchiveUi extends UI {

    private static final Logger LOGGER = LoggerFactory.getLogger(SCArchiveUi.class);

    private static final long serialVersionUID = 1L;

    @Autowired
    private DocumentFinder finder;

    private CssLayout searchForm;

    private TextField searchField;

    private VerticalLayout searchResult;

    private Label searchResultCountLabel;

    @Override
    public void init(VaadinRequest request) {
        getPage().setTitle("SCArchive");

        Responsive.makeResponsive(this);

        final VerticalLayout content = new VerticalLayout();

        final HorizontalLayout searchBar = new HorizontalLayout();
        searchBar.setSizeFull();
        searchForm = new CssLayout();
        searchForm.setSizeFull();
        searchForm.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        searchBar.addComponent(searchForm);
        searchField = new TextField();
        searchField.setWidth(50f, Unit.PERCENTAGE);
        searchField.setPlaceholder("Suchbegriff");
        final Button searchButton = new Button("Suche");

        searchResultCountLabel = new Label("");
        searchResultCountLabel.addStyleName(ValoTheme.LABEL_SMALL);

        searchForm.addComponents(searchField, searchButton);

        searchBar.addComponent(searchResultCountLabel);
        searchBar.setComponentAlignment(searchResultCountLabel, Alignment.MIDDLE_RIGHT);
        searchBar.setExpandRatio(searchForm, 8);
        searchBar.setExpandRatio(searchResultCountLabel, 2);
        searchResult = new VerticalLayout();
        searchResult.setMargin(false);

        searchButton.setClickShortcut(KeyCode.ENTER);
        searchButton.addClickListener(event -> {
            searchButton.setEnabled(false);
            runSearch(searchField.getValue());
            searchButton.setEnabled(true);
        });

        content.addComponents(searchBar, searchResult);

        setContent(content);

        findNewestEntries();

    }

    private void runSearch(String searchString) {
        if (searchString.trim().isEmpty()) {
            return;
        }
        searchResultCountLabel.setValue("...");
        searchResult.removeAllComponents();
        final Set<Finding> findings = finder.find(searchField.getValue());
        findings.forEach(finding -> {
            searchResult.addComponent(createResultComponent(finding));
        });
        if (findings.size() == 0) {
            searchResult.addComponent(new Label("Nichts gefunden :("));
        }

        updateSearchResultCount(findings.size());

    }

    private void updateSearchResultCount(int results) {
        searchResultCountLabel.setValue(results + " Ergebnisse.");
    }

    private void findNewestEntries() {
        searchResultCountLabel.setValue("...");
        searchResult.removeAllComponents();
        final Set<Finding> findings = finder.findNewest();
        findings.forEach(finding -> {
            searchResult.addComponent(createResultComponent(finding));
        });
        updateSearchResultCount(findings.size());
    }

    private Component createResultComponent(Finding finding) {
        final MetaData data = finding.getMetaData();
        final Panel result = new Panel();
        result.setResponsive(true);
        final HorizontalLayout row = new HorizontalLayout();
        final VerticalLayout info = new VerticalLayout();
        Image image = null;
        if (data.getThumbnailPaths().size() > 0) {
            final File imageFile = new File(data.getThumbnailPaths().get(0));
            if (imageFile.exists()) {
                image = new Image(null, new FileResource(imageFile));
                image.setWidth(180, Unit.PIXELS);
            }
        }
        final Label contextLabel = new Label(finding.getContext());
        contextLabel.setContentMode(ContentMode.TEXT);
        contextLabel.setWidth(60f, Unit.PERCENTAGE);
        final Label tagLabel = new Label();

        final CssLayout buttons = new CssLayout();
        buttons.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        final Button editButton = new Button("Bearbeiten");
        editButton.addStyleNames(ValoTheme.BUTTON_FRIENDLY, ValoTheme.BUTTON_SMALL);
        editButton.setIcon(VaadinIcons.PENCIL);
        final Button openButton = new Button("Öffnen");
        openButton.addStyleNames(ValoTheme.BUTTON_SMALL);
        openButton.setIcon(VaadinIcons.DOWNLOAD);
        buttons.addComponents(editButton, openButton);

        info.addComponent(buttons);
        if (image != null) {
            info.addComponent(image);
        }
        info.addComponent(tagLabel);
        row.addComponent(info);
        row.addComponent(contextLabel);
        result.setContent(row);

        row.setMargin(true);
        info.setMargin(false);

        editButton.addClickListener(event -> {
            getUI().addWindow(new EditMetaDataWindow(data, metaData -> {
                metaDataToUI(metaData, finding, result, tagLabel, contextLabel);
            }));
        });

        openButton.addClickListener(event -> {
            try {
                Runtime.getRuntime().exec("open " + data.getFilePath());
            } catch (final IOException ex) {
                LOGGER.error("Cannot open " + data.getFilePath(), ex);
            }
        });

        metaDataToUI(data, finding, result, tagLabel, contextLabel);
        return result;
    }

    private void metaDataToUI(MetaData metaData, Finding finding, Panel panel, Label tagLabel, Label contextLabel) {
        final SimpleDateFormat sdf = new SimpleDateFormat();

        panel.setCaption(sdf.format(metaData.getLastUpdateMetaData()) + " - " + metaData.getTitle());
        tagLabel.setValue(String.join(", ", metaData.getTags()));
        contextLabel.setValue(finding.getContext());
    }

}
