package de.scyv.scarchive.server;

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
import org.springframework.stereotype.Service;

@Service
public class DocumentFinder {

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentFinder.class);

	private Path documentPath = Paths.get("/Users/Y/sync/scans");

	public List<MetaData> find(String searchString) {
		List<String> searchStrings = Arrays.asList(searchString.toLowerCase().split(" "));
		LOGGER.info("Searching " + documentPath + "...");
		final Map<String, MetaData> findings = new HashMap<>();
		try {
			Files.walk(documentPath).filter(this::isMetaDataDir).forEach(path -> {
				findInMetaData(path, searchStrings, findings);
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new ArrayList<>(findings.values());

	}

	private void findInMetaData(Path path, List<String> searchStrings, Map<String, MetaData> findings) {
		try {
			Files.walk(path).filter(Files::isRegularFile).forEach(metaDataPath -> {
				if (metaDataPath.toString().endsWith(".txt")) {
					findInMetaDataFile(metaDataPath, searchStrings, findings);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void findInMetaDataFile(Path metaDataPath, List<String> searchStrings, Map<String, MetaData> findings) {
		searchStrings.forEach(search -> {
			try {
				Files.readAllLines(metaDataPath).parallelStream().forEach(line -> {
					if (line.replaceAll("\\s", "").toLowerCase().contains(search)) {
						findings.put(metaDataPath.toString(), createMetaData(metaDataPath));
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

	}

	private MetaData createMetaData(Path path) {

		MetaData metaData;

		Path metaDataFile = Paths
				.get(path.toString().replace("/.scarchive/", "/").replaceAll("_\\d+\\.png\\.txt$", ".json"));

		try {
			metaData = MetaData.createFromFile(metaDataFile);
		} catch (IOException e) {
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
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		return metaData;
	}

	private boolean isMetaDataDir(Path path) {
		return Files.isDirectory(path) && path.endsWith(".scarchive");
	}
}
