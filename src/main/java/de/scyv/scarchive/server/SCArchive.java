package de.scyv.scarchive.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SCArchive {

	private static final Logger LOGGER = LoggerFactory.getLogger(SCArchive.class);

	@Autowired
	private PDFTextExtractor pdfTextExtractor;

	private Path documentPath = Paths.get("/Users/Y/sync/scans");

	public SCArchive(Scheduler scheduler) {
		scheduler.addRunner(new Runnable() {

			@Override
			public void run() {
				LOGGER.info("Scanning " + documentPath + "...");
				try {
					Files.walk(documentPath).filter(Files::isRegularFile).forEach(path -> {
						String pathStr = path.toString();
						if (pathStr.endsWith(".pdf")) {
							pdfTextExtractor.extract(path);
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

}
