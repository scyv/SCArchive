package de.scyv.scarchive.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Binder;
import com.vaadin.data.Converter;
import com.vaadin.data.Result;
import com.vaadin.data.ValidationException;
import com.vaadin.data.ValueContext;
import com.vaadin.server.FileResource;
import com.vaadin.server.Responsive;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.RichTextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import de.scyv.scarchive.server.MetaData;

/**
 * Window for editing the MetaData of a document/note.
 */
public class EditMetaDataWindow extends Window {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = LoggerFactory.getLogger(EditMetaDataWindow.class);

    private final MetaData metaData;
    private TextField title;
    private TextField tags;
    private RichTextArea text;

    private final Consumer<MetaData> saveCallback;

    /**
     * Create instance.
     *
     * @param metaData
     *            the metadata to edit. Not Null!
     */
    public EditMetaDataWindow(MetaData metaData, Consumer<MetaData> saveCallback) {
        super("Bearbeiten");
        this.metaData = metaData;
        this.saveCallback = saveCallback;

        Responsive.makeResponsive(this);
        setContent(buildEditForm());
        setModal(true);
        this.setSizeFull();
    }

    /**
     * Build the edit form.
     *
     * @return the Component to add to the content.
     */
    public Component buildEditForm() {
        final HorizontalLayout layout = new HorizontalLayout();
        layout.setMargin(true);
        layout.setSizeFull();
        final FormLayout editForm = new FormLayout();
        editForm.setSizeFull();
        if (metaData.getThumbnailPaths().size() > 0) {
            layout.addComponent(new Image(null, new FileResource(new File(metaData.getThumbnailPaths().get(0)))));
        }
        title = new TextField("Titel");
        tags = new TextField("Schlüsselwörter");
        text = new RichTextArea("Text");
        text.setSizeFull();
        title.setWidth(100f, Unit.PERCENTAGE);
        tags.setWidth(100f, Unit.PERCENTAGE);
        final Button saveButton = new Button("Speichern");
        saveButton.addStyleName(ValoTheme.BUTTON_FRIENDLY);
        editForm.addComponents(title, tags, text, saveButton);
        layout.addComponent(editForm);
        final Binder<MetaData> binder = createBinder();
        binder.readBean(metaData);
        addSaveClickListener(saveButton, binder);

        layout.setExpandRatio(editForm, 2);

        return layout;
    }

    private void addSaveClickListener(final Button saveButton, final Binder<MetaData> binder) {
        saveButton.addClickListener(event -> {
            try {
                binder.writeBean(metaData);
                final Path path = metaData.getFilePath();
                final Path metaDataPath = Paths.get(path.getParent().toString(), ".scarchive",
                        path.getFileName().toString() + ".json");
                try {
                    LOGGER.debug("Saving meta data to: " + metaDataPath);
                    metaData.saveToFile(metaDataPath);
                    Notification.show("Speichern erfolreich.", Type.TRAY_NOTIFICATION);
                    saveCallback.accept(metaData);
                } catch (final IOException ex) {
                    LOGGER.error("Cannot save meta data file to " + metaDataPath, ex);
                }
            } catch (final ValidationException ex) {
                LOGGER.error("Could not validate", ex);
                Notification.show("Validierung fehlgeschlagen. Speichern nicht möglich.", Type.TRAY_NOTIFICATION);
            }
        });
    }

    private Binder<MetaData> createBinder() {
        final Binder<MetaData> binder = new Binder<>(MetaData.class);
        binder.forMemberField(tags).withConverter(new Converter<String, List<String>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Result<List<String>> convertToModel(String value, ValueContext context) {
                return Result.ok(Arrays.asList(value.split(" ")));
            }

            @Override
            public String convertToPresentation(List<String> value, ValueContext context) {
                return String.join(" ", value);
            }
        });
        binder.bindInstanceFields(this);
        return binder;
    }

}
