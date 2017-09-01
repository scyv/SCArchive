package de.scyv.scarchive.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.vaadin.annotations.Theme;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FileResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import de.scyv.scarchive.server.MetaData;
import de.scyv.scarchive.server.MetaDataService;
import de.scyv.scarchive.server.search.DocumentFinder;
import de.scyv.scarchive.server.search.Finding;

@Theme("scarchive")
@SpringUI(path = "")
public class SCArchiveUi extends UI {

    private static final Logger LOGGER = LoggerFactory.getLogger(SCArchiveUi.class);

    private static final long serialVersionUID = 1L;

    @Value("${scarchive.openlocal}")
    private Boolean openlocal;

    @Autowired
    private DocumentFinder finder;

    private TextField searchField;

    private VerticalLayout searchResult;

    private Label searchResultCountLabel;

    private VerticalLayout documentDetail;

    private final MetaDataService metaDataService;

    public SCArchiveUi(MetaDataService metaDataService) {
        this.metaDataService = metaDataService;
    }

    @Override
    public void init(VaadinRequest request) {
        getPage().setTitle("SCArchive");

        final VerticalLayout content = new VerticalLayout();
        final HorizontalLayout searchBar = createSearchBar();

        searchResult = new VerticalLayout();
        documentDetail = new VerticalLayout();
        final HorizontalSplitPanel documentContent = new HorizontalSplitPanel(searchResult, documentDetail);

        content.addComponents(searchBar, documentContent);
        setContent(content);

        findNewestEntries();

        content.addStyleName("sc-content");
        searchBar.addStyleName("sc-searchBar");
        documentContent.addStyleName("sc-splitPane");
        searchResult.addStyleName("sc-searchResult");

    }

    private HorizontalLayout createSearchBar() {
        final HorizontalLayout searchBar = new HorizontalLayout();
        final CssLayout searchForm = createSearchForm();
        searchBar.addComponent(searchForm);
        searchBar.addComponent(searchResultCountLabel);
        searchBar.setComponentAlignment(searchResultCountLabel, Alignment.MIDDLE_RIGHT);
        searchBar.setExpandRatio(searchForm, 8);
        searchBar.setExpandRatio(searchResultCountLabel, 2);
        return searchBar;
    }

    private CssLayout createSearchForm() {
        final CssLayout searchForm = new CssLayout();
        searchForm.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        searchForm.addStyleName("sc-searchForm");
        searchField = new TextField();
        searchField.setWidth(50f, Unit.PERCENTAGE);
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
        documentDetail.removeAllComponents();

        final Set<Finding> findings = finder.find(searchField.getValue());
        findings.forEach(finding -> {
            searchResult.addComponent(createResultComponent(finding));
        });
        if (findings.size() == 0) {
            searchResult.addComponent(new Label("Nichts gefunden :("));
        } else {
            updateDetailComponent(findings.iterator().next());
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
        if (findings.size() > 0) {
            updateDetailComponent(findings.iterator().next());
        }
    }

    private void updateDetailComponent(Finding finding) {
        documentDetail.removeAllComponents();
        createDetailComponent(finding);
    }

    private void createDetailComponent(Finding finding) {
        final MetaData data = finding.getMetaData();

        final CssLayout buttons = new CssLayout();
        buttons.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        final Button editButton = new Button("Bearbeiten");
        editButton.addStyleNames(ValoTheme.BUTTON_FRIENDLY, ValoTheme.BUTTON_SMALL);
        editButton.setIcon(VaadinIcons.PENCIL);
        final Button openButton = createOpenButton(data);

        buttons.addComponents(editButton, openButton);

        documentDetail.addComponent(new Label(data.getTitle()));
        documentDetail.addComponent(buttons);
        documentDetail.addComponent(new Label(data.getText()));

        // final BrowserFrame bf = new BrowserFrame();
        // bf.setSource(new
        // FileResource(metaDataService.getOriginalFilePath(data).toFile()));
        // documentDetail.addComponent(bf);

        editButton.addClickListener(event -> {
            getUI().addWindow(new EditMetaDataWindow(data, metaData -> {
                // TODO add binding to search result panel left
            }));
        });

    }

    private Component createResultComponent(Finding finding) {
        final MetaData data = finding.getMetaData();
        final Panel result = new Panel();
        result.setResponsive(true);
        final VerticalLayout info = new VerticalLayout();
        Image image = null;
        if (data.getThumbnailPaths().size() > 0) {
            final File imageFile = new File(data.getThumbnailPaths().get(0));
            if (imageFile.exists()) {
                image = new Image(null, new FileResource(imageFile));
                image.setWidth(180, Unit.PIXELS);
            }
        }
        final Label tagLabel = new Label();

        if (image != null) {
            info.addComponent(image);
        }
        info.addComponent(tagLabel);

        result.setContent(info);

        info.setMargin(false);

        result.addClickListener(event -> {
            updateDetailComponent(finding);
        });

        metaDataToUI(data, finding, result, tagLabel);
        return result;
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
