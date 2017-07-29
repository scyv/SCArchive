package de.scyv.scarchive.server.processing;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs tesseract on commandline.
 */
public class TesseractRunner extends ProcessRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(TesseractRunner.class);

	/**
	 * Create instance.
	 *
	 * @param filePath
	 *            the path of the file to be processed by OCR
	 * @param tesseractBin
	 *            the binary of tesseract
	 */
	public TesseractRunner(Path filePath, String tesseractBin) {
		setCommand(new String[] { tesseractBin, "-psm", "3", filePath.toString(), filePath.toString() });
	}

	@Override
	public int run() throws IOException, InterruptedException {
		LOGGER.debug("Running tesseract with command: " + String.join(" ", command));
		return super.run();
	}

}
