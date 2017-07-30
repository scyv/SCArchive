package de.scyv.scarchive.server.extraction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.scyv.scarchive.server.MetaData;

@Service
public class HTMLExtractor implements Extractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HTMLExtractor.class);

    @Override
    public String getIdentifier() {
        return "HTML";
    }

    @Override
    public void extract(Path path) {

        LOGGER.info("Extracting " + path);

        final MetaData metaData = new MetaData();
        metaData.setFilePath(path.toString());
        metaData.setTitle(path.getFileName().toString());
        final Path metaDataPath = getMetaDataPath(path);
        if (Files.exists(metaDataPath)) {
            return;
        }
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

    private Path getMetaDataPath(Path path) {
        return Paths.get(path.getParent().toString(), ".scarchive", path.getFileName().toString() + ".json");
    }

}
