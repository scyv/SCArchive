package de.scyv.scarchive.search;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
					findInMetaData(path, searchStrings, findings);
				});
			} catch (final IOException ex) {
				LOGGER.error("Cannot walk the path: " + documentPath, ex);
			}
		});
		return new ArrayList<>(findings.values());
	}

	private void findInMetaData(Path path, List<String> searchStrings, Map<String, Finding> findings) {
		try {
			Files.walk(path).filter(Files::isRegularFile).forEach(metaDataPath -> {
				if (metaDataPath.toString().endsWith(".txt")) {
					findInMetaDataFile(metaDataPath, searchStrings, findings);
				}
			});
		} catch (final IOException ex) {
			LOGGER.error("Cannot walk the path: " + path, ex);
		}
	}

	private void findInMetaDataFile(Path metaDataPath, List<String> searchStrings, Map<String, Finding> findings) {
		searchStrings.parallelStream().forEach(search -> {
			try {
				Files.readAllLines(metaDataPath).parallelStream().forEach(line -> {
					if (line.replaceAll("\\s", "").toLowerCase().contains(search)) {
						LOGGER.debug("Found something in " + metaDataPath + ": " + line);
						final Finding finding = new Finding();
						finding.setMetaData(createMetaData(metaDataPath));
						finding.setContext(line);
						findings.put(metaDataPath.toString(), finding);
					}
				});
			} catch (final IOException ex) {
				LOGGER.error("Cannot walk the path: " + metaDataPath, ex);
			}
		});

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
