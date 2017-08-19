package de.scyv.scarchive.search;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.scyv.scarchive.server.MetaData;
import de.scyv.scarchive.server.MetaDataService;

/**
 * Service for finding documents by query.
 */
@Service
public class DocumentFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentFinder.class);

    @Value("${scarchive.maxSearchResults}")
    private Integer maxSearchResults;

    @Value("${scarchive.documentPaths}")
    private String documentPaths;

    private final MetaDataService metaDataService;

    public DocumentFinder(MetaDataService metaDataService) {
        this.metaDataService = metaDataService;
    }

    /**
     * Find documents by a search string.
     *
     * When containing blank, the string is splitted and an OR search is done.
     *
     * @param searchString
     *            the search string.
     * @return list of findins. Empty list, if nothing could be found.
     */
    public Set<Finding> find(String searchString) {
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
        final TreeSet<Finding> sortedFindings = new TreeSet<>(new Finding.LatestUpdateComparator());
        sortedFindings.addAll(findings.values());
        return Collections.unmodifiableSortedSet(sortedFindings);
    }

    /**
     * Find the newest entries.
     *
     * @return list of findins. Empty list, if nothing could be found.
     */
    public Set<Finding> findNewest() {

        final Map<String, Finding> findings = new HashMap<>();
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -2);
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
        final TreeSet<Finding> sortedFindings = new TreeSet<>(new Finding.LatestUpdateComparator());
        sortedFindings.addAll(findings.values());
        return Collections.unmodifiableSortedSet(sortedFindings);
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
                if (findings.size() >= maxSearchResults) {
                    return;
                }
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
                    if (findings.size() >= maxSearchResults) {
                        return;
                    }
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
        if (findings.size() >= maxSearchResults) {
            return;
        }

        try {
            final MetaData metaData = createMetaData(metaDataPath);
            final Path originalFilePath = metaDataService.getOriginalFilePath(metaData);
            if (!Files.exists(originalFilePath)) {
                Files.delete(metaDataPath);
                return;
            }

            String context = "";
            if (metaData.getText().toLowerCase().contains(searchString)
                    || String.join(",", metaData.getTags()).toLowerCase().contains(searchString)
                    || metaData.getTitle().toLowerCase().contains(searchString)
                    || metaDataPath.toString().contains(searchString)) {
                context = metaData.getText();
            }

            if (!context.isEmpty() && findings.get(originalFilePath.toString()) == null) {
                final Finding finding = new Finding();
                finding.setMetaData(metaData);
                finding.setContext(context);
                findings.put(originalFilePath.toString(), finding);
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
                    Finding finding = findings.get(textDataPath.toString());
                    if (finding == null) {
                        finding = new Finding();
                        finding.setMetaData(createMetaData(textDataPath));
                        finding.setContext(line);
                    } else {
                        finding.setContext(finding.getContext() + "<br><br>...<br><br>" + line);
                    }
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
            if (metaData.getLastUpdateMetaData() == null) {
                metaData.setLastUpdateMetaData(new Date(Files.getLastModifiedTime(metaDataFile).toMillis()));
            }
        } catch (final IOException e) {
            LOGGER.warn("Cannot open metaDataFile " + metaDataFile + ". Creating one for you...");
            final String originFileName = Paths.get(path.toString().replaceAll("_\\d+\\.png\\.txt$", "")).getFileName()
                    .toString();
            metaData = new MetaData();
            metaData.setTitle(originFileName);
            metaData.getThumbnailPaths()
                    .add(Paths.get(("thumb_" + path.getFileName()).replaceAll("\\.txt$", "")).toString());
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
