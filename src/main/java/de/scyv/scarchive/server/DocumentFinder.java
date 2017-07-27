package de.scyv.scarchive.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
		LOGGER.info("Searching " + documentPath + "...");
		final Map<String, MetaData> findings = new HashMap<>();
		try {
			Files.walk(documentPath).filter(this::isMetaDataDir).forEach(path -> {
				try {
					Files.walk(path).filter(Files::isRegularFile).forEach(metaDataPath -> {
						if (metaDataPath.toString().endsWith(".txt")) {
							try {
								for (String line : Files.readAllLines(metaDataPath)) {
									if (line.toLowerCase().contains(searchString.toLowerCase())) {
										findings.put(metaDataPath.toString(), createMetaData(metaDataPath));
									}
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new ArrayList<>(findings.values());

	}

	private MetaData createMetaData(Path path) {
		MetaData metaData = new MetaData();
		metaData.setFilePath(
				Paths.get(path.toString().replace("/.scarchive/", "/").replaceAll("_\\d+\\.png\\.txt$", "")));
		metaData.setThumbnailPath(
				Paths.get(path.getParent().toString(), ("thumb_" + path.getFileName()).replaceAll("\\.txt$", "")));
		return metaData;
	}

	private boolean isMetaDataDir(Path path) {
		return Files.isDirectory(path) && path.endsWith(".scarchive");
	}
}
