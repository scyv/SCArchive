package de.scyv.scarchive.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    @Autowired
    private PDFTextExtractor pdfTextExtractor;

    @Autowired
    private HTMLExtractor htmlExtractor;

    /**
     * Create the service.
     *
     * @param scheduler
     *            the scheduler service that executes the scan job periodically
     * @param documentPaths
     *            application property containing a list of paths that should be
     *            scanned
     */
    public SCArchive(Scheduler scheduler, @Value("${scarchive.documentpaths}") String documentPaths) {

        scheduler.addRunner(() -> {
            final ExtractionCollection collection = new ExtractionCollection();
            collectFiles(documentPaths, collection);
            LOGGER.info("Found " + collection.size() + " files in the archive.");
            runExtraction(collection);

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
        LOGGER.info("Running extraction for " + extractor.getIdentifier() + ", " + collection.size(extractor)
                + " items...");
        collection.get(extractor).forEach(path -> {
            extractor.extract(path);
        });
    }

}
