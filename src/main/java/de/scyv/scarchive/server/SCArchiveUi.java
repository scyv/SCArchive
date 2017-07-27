package de.scyv.scarchive.server;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.annotations.Theme;
import com.vaadin.server.FileResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@Theme("valo")
@SpringUI(path = "")
public class SCArchiveUi extends UI {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Autowired
	DocumentFinder finder;

	@Override
	public void init(VaadinRequest request) {
		getPage().setTitle("SCArchive");

		VerticalLayout content = new VerticalLayout();

		FormLayout searchForm = new FormLayout();
		final TextField searchField = new TextField("");
		searchField.setPlaceholder("Suchbegriff");
		searchField.addStyleName(ValoTheme.TEXTAREA_HUGE);
		Button searchButton = new Button("Suche");
		searchForm.addComponents(searchField, searchButton);

		Panel searchResult = new Panel("Suchergebnis");
		searchResult.setVisible(false);

		searchButton.addClickListener(event -> {
			CssLayout searchResultList = new CssLayout();
			searchResult.setContent(searchResultList);
			finder.find(searchField.getValue()).forEach(data -> {
				searchResultList.addComponent(
						new Image(data.getFilePath().toString(), new FileResource(data.getThumbnailPath().toFile())));
			});
			searchResult.setVisible(true);

		});

		content.addComponents(searchForm, searchResult);

		setContent(content);
	}

}
