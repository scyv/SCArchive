package de.scyv.scarchive.server;

import java.nio.file.Path;

public class TesseractRunner extends ProcessRunner {

	public TesseractRunner(Path filePath) {
		setCommand(new String[] { "/usr/local/bin/tesseract", "-psm", "3", filePath.toString(), filePath.toString() });
	}

}
