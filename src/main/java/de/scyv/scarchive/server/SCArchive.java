package de.scyv.scarchive.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.scyv.scarchive.server.extraction.Extractor;
import de.scyv.scarchive.server.extraction.HTMLExtractor;
import de.scyv.scarchive.server.extraction.PDFTextExtractor;

/**
 * Backend service that scans PDF files and extract them.
 */
@Service
public class SCArchive {

    private static final Logger LOGGER = LoggerFactory.getLogger(SCArchive.class);

    @Value("${scarchive.enablescan}")
    private Boolean enableScan;

    @Value("${scarchive.documentPaths}")
    private String documentPaths;

    @Autowired
    private PDFTextExtractor pdfTextExtractor;

    @Autowired
    private HTMLExtractor htmlExtractor;

    @Autowired
    private MetaDataService metaDataService;

    /**
     * Create the service.
     *
     * @param scheduler
     *            the scheduler service that executes the scan job periodically
     */
    public SCArchive(Scheduler scheduler) {

        scheduler.addRunner(() -> {
            if (enableScan) {
                final ExtractionCollection collection = new ExtractionCollection();
                collectFiles(documentPaths, collection);
                LOGGER.info("Found " + collection.size() + " files in the archive.");
                runExtraction(collection);
            }
        });

    }

    private void collectFiles(String documentPaths, final ExtractionCollection collection) {
        for (final String documentPath : documentPaths.split(";")) {
            LOGGER.info("Collecting files from " + documentPath + "...");
            try {

                Files.walk(Paths.get(documentPath)).filter(path -> {
                    try {
                        return Files.isRegularFile(path) && !Files.isHidden(path)
                                && !path.getParent().endsWith(".AppleDouble");
                    } catch (final IOException ex) {
                        LOGGER.error("Cannot invode isHidden on file " + path, ex);
                    }
                    return false;
                }).forEach(path -> {
                    if (pdfTextExtractor.accepts(path)) {
                        collection.add(pdfTextExtractor, path);
                    }
                    if (htmlExtractor.accepts(path)) {
                        collection.add(htmlExtractor, path);
                    }
                });

            } catch (final IOException ex) {
                LOGGER.error("Walking through " + documentPath + " failed.", ex);
            }
        }
    }

    private void runExtraction(ExtractionCollection collection) {
        final AtomicInteger currentIdx = new AtomicInteger(0);

        runExtraction(collection, pdfTextExtractor, currentIdx);
        runExtraction(collection, htmlExtractor, currentIdx);
    }

    private void runExtraction(ExtractionCollection collection, Extractor extractor, AtomicInteger currentIdx) {
        final List<Path> toExtract = new ArrayList<>();
        collection.get(extractor).stream().filter(path -> !metaDataService.isAlreadyExtracted(path)).forEach(path -> {
            toExtract.add(path);
        });
        if (toExtract.size() == 0) {
            return;
        }
        LOGGER.info("Running extraction for " + extractor.getIdentifier() + ": " + toExtract.size() + " items...");
        final AtomicInteger currentCount = new AtomicInteger(0);
        toExtract.stream().forEach(path -> {
            LOGGER.info(extractor.getIdentifier() + ": "
                    + String.format("%.0f%% (%d/%d)", (double) currentCount.get() * 100 / toExtract.size(),
                            currentCount.getAndIncrement(), toExtract.size()));
            extractor.extract(path);
        });
        LOGGER.info("Extraction for " + extractor.getIdentifier() + " done.");

    }

}
