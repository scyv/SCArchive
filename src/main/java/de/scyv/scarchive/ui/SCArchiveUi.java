package de.scyv.scarchive.ui;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.annotations.Theme;
import com.vaadin.server.FileResource;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
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

import de.scyv.scarchive.search.DocumentFinder;
import de.scyv.scarchive.search.Finding;
import de.scyv.scarchive.server.MetaData;

@Theme("valo")
@SpringUI(path = "")
public class SCArchiveUi extends UI {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Autowired
	private DocumentFinder finder;

	private CssLayout searchForm;

	private TextField searchField;

	private VerticalLayout searchResult;

	@Override
	public void init(VaadinRequest request) {
		getPage().setTitle("SCArchive");

		Responsive.makeResponsive(this);

		final VerticalLayout content = new VerticalLayout();

		searchForm = new CssLayout();
		searchField = new TextField();
		searchField.setPlaceholder("Suchbegriff");
		final Button searchButton = new Button("Suche");
		searchForm.addComponents(searchField, searchButton);

		searchResult = new VerticalLayout();
		searchResult.setMargin(false);
		searchResult.setVisible(false);

		searchButton.addClickListener(event -> {
			runSearch(searchField.getValue());
		});

		content.addComponents(searchForm, searchResult);

		setContent(content);
	}

	private void runSearch(String searchString) {
		final CssLayout searchResultList = new CssLayout();
		searchResult.removeAllComponents();
		searchResult.addComponent(searchResultList);
		final List<Finding> findings = finder.find(searchField.getValue());
		findings.forEach(finding -> {
			searchResultList.addComponent(createResultComponent(finding));
		});
		if (findings.size() == 0) {
			searchResultList.addComponent(new Label("Nichts gefunden :("));
		}

		searchResult.setVisible(true);
	}

	private Component createResultComponent(Finding finding) {
		final MetaData data = finding.getMetaData();
		final Panel result = new Panel(data.getTitle());
		result.addClickListener(event -> {
			getUI().addWindow(new EditMetaDataWindow(data));
		});
		final HorizontalLayout row = new HorizontalLayout();
		final VerticalLayout info = new VerticalLayout();
		row.addComponent(new Image(null, new FileResource(new File(data.getThumbnailPaths().get(0)))));
		row.addComponent(info);
		row.addComponent(new Label(finding.getContext()));
		info.addComponent(new Label(String.join(", ", data.getTags())));
		info.addComponent(new Label(data.getFilePath().toString()));
		result.setContent(row);
		return result;

	}

}
