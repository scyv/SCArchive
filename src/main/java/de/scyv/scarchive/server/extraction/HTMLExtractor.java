package de.scyv.scarchive.server.extraction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.scyv.scarchive.server.MetaData;
import de.scyv.scarchive.server.MetaDataService;

@Service
public class HTMLExtractor implements Extractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HTMLExtractor.class);

    private final MetaDataService metaDataService;

    public HTMLExtractor(MetaDataService metaDataService) {
        this.metaDataService = metaDataService;
    }

    @Override
    public String getIdentifier() {
        return "HTML";
    }

    @Override
    public void extract(Path path) {

        final Path metaDataPath = metaDataService.getMetaDataPath(path);

        LOGGER.info("Extracting " + path);

        final MetaData metaData = new MetaData();
        metaData.setTitle(path.getFileName().toString());
        metaDataPath.getParent().toFile().mkdirs();
        try {
            metaData.setText(new String(Files.readAllBytes(path), "UTF-8"));
            metaData.saveToFile(metaDataPath);
        } catch (final IOException ex) {
            LOGGER.error("Could not extract " + path, ex);
        }
    }

    @Override
    public boolean accepts(Path path) {
        return path.toString().toLowerCase().endsWith(".html");
    }

}
