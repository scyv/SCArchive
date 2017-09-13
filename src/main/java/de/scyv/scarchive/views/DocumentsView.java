package de.scyv.scarchive.views;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FileResource;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import de.scyv.scarchive.server.MetaData;
import de.scyv.scarchive.server.MetaDataService;
import de.scyv.scarchive.server.search.DocumentFinder;
import de.scyv.scarchive.server.search.Finding;
import de.scyv.scarchive.ui.EditMetaDataWindow;

@SpringView(name = DocumentsView.VIEW_NAME)
public class DocumentsView extends VerticalLayout implements ScarchiveView {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentsView.class);

    public static final String VIEW_NAME = "documents";

    private static final long serialVersionUID = 1L;

    @Value("${scarchive.openlocal}")
    private Boolean openlocal;

    @Autowired
    private DocumentFinder finder;

    private TextField searchField;

    private VerticalLayout searchResult;

    private Label searchResultCountLabel;

    private final MetaDataService metaDataService;

    public DocumentsView(MetaDataService metaDataService) {
        this.metaDataService = metaDataService;
    }

    @PostConstruct
    public void init() {

        final VerticalLayout content = new VerticalLayout();
        final CssLayout searchBar = createSearchBar();
        content.setMargin(false);
        searchResult = new VerticalLayout();
        searchResult.setMargin(false);

        content.addComponents(searchBar, searchResult);
        this.addComponent(content);

        findNewestEntries();

        content.addStyleName("sc-content");
        searchBar.addStyleName("sc-searchBar");
        searchResult.addStyleName("sc-searchResult");
    }

    @Override
    public boolean loginNeeded() {
        return true;
    }

    private CssLayout createSearchBar() {
        final CssLayout searchBar = new CssLayout();
        final CssLayout searchForm = createSearchForm();
        searchBar.addComponent(searchForm);
        searchBar.addComponent(searchResultCountLabel);
        searchBar.addStyleName("sc-searchBar");
        return searchBar;
    }

    private CssLayout createSearchForm() {
        final CssLayout searchForm = new CssLayout();
        searchForm.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        searchForm.addStyleName("sc-searchForm");
        searchField = new TextField();
        searchField.setPlaceholder("Suchbegriff");
        final Button searchButton = new Button("Suche");

        searchResultCountLabel = new Label("");
        searchResultCountLabel.addStyleName(ValoTheme.LABEL_SMALL);
        searchResultCountLabel.addStyleName("sc-searchResultCount");

        searchForm.addComponents(searchField, searchButton);
        searchButton.setClickShortcut(KeyCode.ENTER);
        searchButton.addClickListener(event -> {
            searchButton.setEnabled(false);
            runSearch(searchField.getValue());
            searchButton.setEnabled(true);
        });
        return searchForm;
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
        final VerticalLayout info = new VerticalLayout();
        final Label tagLabel = new Label();

        final Label filePathLabel = new Label();
        filePathLabel.addStyleName(ValoTheme.LABEL_SMALL);
        filePathLabel.setValue(metaDataService.getOriginalFilePath(finding.getMetaData()).toString());

        addButtons(data, info, finding, result, tagLabel);
        info.addComponent(filePathLabel);
        addThumbnail(data, info, 180);
        info.addComponent(tagLabel);

        result.setContent(info);

        info.setMargin(false);

        metaDataToUI(data, finding, result, tagLabel);
        return result;
    }

    private void addButtons(MetaData data, VerticalLayout info, Finding finding, Panel result, Label tagLabel) {
        final CssLayout buttons = new CssLayout();
        buttons.addStyleName("sc-documentButtons");
        buttons.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        final Button editButton = new Button("Bearbeiten");
        editButton.addStyleNames(ValoTheme.BUTTON_FRIENDLY, ValoTheme.BUTTON_SMALL);
        editButton.setIcon(VaadinIcons.PENCIL);
        final Button openButton = createOpenButton(data);

        buttons.addComponents(editButton, openButton);
        editButton.addClickListener(event -> {
            getUI().addWindow(new EditMetaDataWindow(data, metaData -> {
                metaDataToUI(metaData, finding, result, tagLabel);
            }));
        });
        info.addComponent(buttons);
    }

    private void addThumbnail(final MetaData data, final VerticalLayout info, int width) {
        Image image = null;
        if (data.getThumbnailPaths().size() > 0) {
            final File imageFile = Paths.get(data.getFilePath().getParent().toString(), data.getThumbnailPaths().get(0))
                    .toFile();
            if (imageFile.exists()) {
                image = new Image(null, new FileResource(imageFile));
                image.setWidth(width, Unit.PIXELS);
            }
        }
        if (image != null) {
            info.addComponent(image);
        }
    }

    private Button createOpenButton(MetaData data) {
        final Button openButton = new Button("Öffnen");
        openButton.addStyleNames(ValoTheme.BUTTON_SMALL);
        openButton.setIcon(VaadinIcons.DOWNLOAD);
        if (openlocal) {
            openButton.addClickListener(event -> {
                final Path fileToOpen = metaDataService.getOriginalFilePath(data);
                LOGGER.debug("Opening file locally: " + fileToOpen);
                try {
                    Runtime.getRuntime().exec("open " + fileToOpen);
                } catch (final IOException ex) {
                    LOGGER.error("Cannot open " + fileToOpen, ex);
                }
            });
        } else {
            final FileDownloader downloader = new FileDownloader(
                    new FileResource(metaDataService.getOriginalFilePath(data).toFile()));
            downloader.extend(openButton);
        }
        return openButton;
    }

    private void metaDataToUI(MetaData metaData, Finding finding, Panel panel, Label tagLabel) {
        final SimpleDateFormat sdf = new SimpleDateFormat();
        panel.setCaption(sdf.format(metaData.getLastUpdateMetaData()) + " - " + metaData.getTitle());
        tagLabel.setValue(String.join(", ", metaData.getTags()));
    }

}
