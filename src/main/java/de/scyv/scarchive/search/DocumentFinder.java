package de.scyv.scarchive.search;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.scyv.scarchive.server.MetaData;

/**
 * Service for finding documents by query.
 */
@Service
public class DocumentFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentFinder.class);

    @Value("${scarchive.documentpaths}")
    private String documentPaths;

    /**
     * Find documents by a search string.
     *
     * When containing blank, the string is splitted and an OR search is done.
     *
     * @param searchString
     *            the search string.
     * @return list of findins. Empty list, if nothing could be found.
     */
    public List<Finding> find(String searchString) {
        final List<String> searchStrings = Arrays.asList(searchString.toLowerCase().split(" "));
        final Map<String, Finding> findings = new HashMap<>();
        Arrays.asList(documentPaths.split(";")).parallelStream().forEach(documentPath -> {
            LOGGER.info("Searching " + documentPath + "...");
            try {
                Files.walk(Paths.get(documentPath)).filter(this::isMetaDataDir).forEach(path -> {
                    findBySearchStrings(path, searchStrings, findings);
                });
            } catch (final IOException ex) {
                LOGGER.error("Cannot walk the path: " + documentPath, ex);
            }
        });
        return Collections.unmodifiableList(new ArrayList<>(findings.values()));
    }

    public List<Finding> findNewest() {

        final Map<String, Finding> findings = new HashMap<>();
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_WEEK_IN_MONTH, -2);
        Arrays.asList(documentPaths.split(";")).parallelStream().forEach(documentPath -> {
            LOGGER.info("Searching " + documentPath + "...");
            try {
                Files.walk(Paths.get(documentPath)).filter(this::isMetaDataDir).forEach(path -> {
                    findByCalendar(path, cal, findings);
                });
            } catch (final IOException ex) {
                LOGGER.error("Cannot walk the path: " + documentPath, ex);
            }
        });
        return Collections.unmodifiableList(new ArrayList<>(findings.values()));

    }

    private void findByCalendar(Path path, Calendar cal, Map<String, Finding> findings) {
        try {
            Files.walk(path).filter(metaDataPath -> {
                try {
                    if (metaDataPath.toString().endsWith(".json")) {
                        return Files.getLastModifiedTime(metaDataPath).toMillis() > cal.getTimeInMillis();
                    }
                } catch (final IOException ex) {
                    LOGGER.error("Cannot get last modified time of: " + metaDataPath, ex);
                }
                return false;
            }).forEach(metaDataPath -> {
                final Finding finding = new Finding();
                final MetaData metaData = createMetaData(metaDataPath);
                finding.setMetaData(metaData);
                finding.setContext(metaData.getText());

                findings.put(metaDataPath.toString(), finding);
            });
        } catch (final IOException ex) {
            LOGGER.error("Cannot walk the path: " + path, ex);
        }
    }

    private void findBySearchStrings(Path path, List<String> searchStrings, Map<String, Finding> findings) {
        searchStrings.parallelStream().forEach(searchString -> {
            try {
                Files.walk(path).filter(Files::isRegularFile).forEach(metaDataPath -> {
                    if (metaDataPath.toString().endsWith(".txt")) {
                        findInTextDataFile(metaDataPath, searchString, findings);
                    } else if (metaDataPath.toString().endsWith(".json")) {
                        findInMetaData(metaDataPath, searchString, findings);
                    }
                });
            } catch (final IOException ex) {
                LOGGER.error("Cannot walk the path: " + path, ex);
            }
        });
    }

    private void findInMetaData(Path metaDataPath, String searchString, Map<String, Finding> findings) {
        try {
            final MetaData metaData = MetaData.createFromFile(metaDataPath);

            if (!Files.exists(Paths.get(metaData.getFilePath()))) {
                Files.delete(metaDataPath);
                return;
            }

            String context = "";
            if (metaData.getText().toLowerCase().contains(searchString)) {
                final String text = metaData.getText();
                context = text.length() > 400 ? text.substring(0, 400) : text;
            } else if (String.join(",", metaData.getTags()).toLowerCase().contains(searchString)) {
                context = String.join(", ", metaData.getTags());
            } else if (metaData.getTitle().toLowerCase().contains(searchString)) {
                context = metaData.getTitle();
            } else if (metaData.getFilePath().toLowerCase().contains(searchString)) {
                context = metaData.getFilePath();
            }

            if (!context.isEmpty()) {
                final Finding finding = new Finding();
                finding.setMetaData(metaData);
                finding.setContext(context);
                findings.put(metaData.getFilePath(), finding);
            }

        } catch (final IOException ex) {
            LOGGER.error("Cannot read meta data file: " + metaDataPath, ex);
        }
    }

    private void findInTextDataFile(Path textDataPath, String searchString, Map<String, Finding> findings) {
        try {
            Files.readAllLines(textDataPath).parallelStream().forEach(line -> {
                if (line.replaceAll("\\s", "").toLowerCase().contains(searchString)) {
                    LOGGER.debug("Found something in " + textDataPath + ": " + line);
                    final Finding finding = new Finding();
                    finding.setMetaData(createMetaData(textDataPath));
                    finding.setContext(line);
                    // TODO fix that: we possibly have two findings for the same file: textDataPath
                    // is wrong at this place
                    findings.put(textDataPath.toString(), finding);
                }
            });
        } catch (final IOException ex) {
            LOGGER.error("Cannot walk the path: " + textDataPath, ex);
        }
    }

    private MetaData createMetaData(Path path) {

        MetaData metaData;

        final Path metaDataFile = Paths.get(path.toString().replaceAll("_\\d+\\.png\\.txt$", ".json"));

        try {
            metaData = MetaData.createFromFile(metaDataFile);
        } catch (final IOException e) {
            LOGGER.warn("Cannot open metaDataFile " + metaDataFile + ". Creating one for you...");
            metaData = new MetaData();
            metaData.setTitle(path.getFileName().toString());
            metaData.setFilePath(path.toString().replace("/.scarchive/", "/").replaceAll("_\\d+\\.png\\.txt$", ""));
            metaData.getThumbnailPaths()
                    .add(Paths
                            .get(path.getParent().toString(), ("thumb_" + path.getFileName()).replaceAll("\\.txt$", ""))
                            .toString());
            try {
                metaData.saveToFile(metaDataFile);
            } catch (final IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return metaData;
    }

    private boolean isMetaDataDir(Path path) {
        return Files.isDirectory(path) && path.endsWith(".scarchive");
    }
}
