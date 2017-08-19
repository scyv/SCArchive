package de.scyv.scarchive.ui;

import java.io.File;
import java.io.IOException;
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
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.BrowserFrame;
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

import de.scyv.scarchive.search.DocumentFinder;
import de.scyv.scarchive.search.Finding;
import de.scyv.scarchive.server.MetaData;
import de.scyv.scarchive.server.MetaDataService;

@Theme("valo")
@SpringUI(path = "")
public class SCArchiveUi extends UI {

    private static final Logger LOGGER = LoggerFactory.getLogger(SCArchiveUi.class);

    private static final long serialVersionUID = 1L;

    @Value("${scarchive.openlocal}")
    private Boolean openlocal;

    @Autowired
    private DocumentFinder finder;

    private CssLayout searchForm;

    private TextField searchField;

    private VerticalLayout searchResult;

    private Label searchResultCountLabel;

    private CssLayout documentDetail;

    private final MetaDataService metaDataService;

    public SCArchiveUi(MetaDataService metaDataService) {
        this.metaDataService = metaDataService;
    }

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

        searchButton.setClickShortcut(KeyCode.ENTER);
        searchButton.addClickListener(event -> {
            searchButton.setEnabled(false);
            runSearch(searchField.getValue());
            searchButton.setEnabled(true);
        });

        searchResult = new VerticalLayout();
        searchResult.setMargin(false);
        searchResult.setSizeFull();

        documentDetail = new CssLayout();

        final HorizontalSplitPanel documentContent = new HorizontalSplitPanel(searchResult, documentDetail);
        documentContent.setSizeFull();

        content.addComponents(searchBar, documentContent);

        setContent(content);

        findNewestEntries();

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
        documentDetail.addComponent(createDetailComponent(finding));
    }

    private Component createDetailComponent(Finding finding) {
        final MetaData data = finding.getMetaData();

        final VerticalLayout detail = new VerticalLayout();
        detail.setSizeFull();
        final CssLayout buttons = new CssLayout();
        buttons.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        final Button editButton = new Button("Bearbeiten");
        editButton.addStyleNames(ValoTheme.BUTTON_FRIENDLY, ValoTheme.BUTTON_SMALL);
        editButton.setIcon(VaadinIcons.PENCIL);
        final Button openButton = createOpenButton(data);

        buttons.addComponents(editButton, openButton);

        detail.addComponent(buttons);

        final BrowserFrame bf = new BrowserFrame();
        bf.setSource(new FileResource(metaDataService.getOriginalFilePath(data).toFile()));
        bf.setSizeFull();
        detail.addComponent(bf);

        editButton.addClickListener(event -> {
            getUI().addWindow(new EditMetaDataWindow(data, metaData -> {
                // TODO add binding to search result panel left
            }));
        });

        return detail;
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
                try {
                    Runtime.getRuntime().exec("open " + data.getFilePath());
                } catch (final IOException ex) {
                    LOGGER.error("Cannot open " + data.getFilePath(), ex);
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
