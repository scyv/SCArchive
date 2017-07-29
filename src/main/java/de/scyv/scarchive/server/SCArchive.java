package de.scyv.scarchive.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Backend service that scans PDF files and extract them.
 */
@Service
public class SCArchive {

	private static final Logger LOGGER = LoggerFactory.getLogger(SCArchive.class);

	@Autowired
	private PDFTextExtractor pdfTextExtractor;

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
		for (String documentPath : documentPaths.split(";")) {
			scheduler.addRunner(new Runnable() {

				@Override
				public void run() {
					LOGGER.info("Scanning " + documentPath + "...");
					try {
						Files.walk(Paths.get(documentPath)).filter(Files::isRegularFile).forEach(path -> {
							String pathStr = path.toString();
							if (pathStr.endsWith(".pdf")) {
								pdfTextExtractor.extract(path);
							}
						});
					} catch (IOException ex) {
						LOGGER.error("Walking through " + documentPath + " failed.", ex);
					}
				}
			});
		}
	}

}
