package de.scyv.scarchive.server;

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

@Theme("valo")
@SpringUI(path = "")
public class SCArchiveUi extends UI {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Autowired
	private DocumentFinder finder;

	@Override
	public void init(VaadinRequest request) {
		getPage().setTitle("SCArchive");

		Responsive.makeResponsive(this);

		VerticalLayout content = new VerticalLayout();

		CssLayout searchForm = new CssLayout();
		final TextField searchField = new TextField();
		searchField.setPlaceholder("Suchbegriff");
		Button searchButton = new Button("Suche");
		searchForm.addComponents(searchField, searchButton);

		// Panel searchResult = new Panel("Suchergebnis");
		VerticalLayout searchResult = new VerticalLayout();
		searchResult.setVisible(false);

		searchButton.addClickListener(event -> {
			CssLayout searchResultList = new CssLayout();
			// searchResult.setContent(searchResultList);
			searchResult.removeAllComponents();
			searchResult.addComponent(searchResultList);
			List<MetaData> findings = finder.find(searchField.getValue());
			findings.forEach(data -> {
				searchResultList.addComponent(createResultComponent(data));
			});
			if (findings.size() == 0) {
				searchResultList.addComponent(new Label("Nichts gefunden :("));
			}

			searchResult.setVisible(true);
		});

		content.addComponents(searchForm, searchResult);

		setContent(content);
	}

	private Component createResultComponent(MetaData data) {
		Panel result = new Panel();
		HorizontalLayout row = new HorizontalLayout();
		VerticalLayout info = new VerticalLayout();

		row.addComponent(new Image(null, new FileResource(new File(data.getThumbnailPaths().get(0)))));
		row.addComponent(info);
		info.addComponent(new Label(data.getTitle()));
		info.addComponent(new Label(String.join(", ", data.getTags())));
		info.addComponent(new Label(data.getFilePath().toString()));
		result.setContent(row);
		return result;

	}

}
