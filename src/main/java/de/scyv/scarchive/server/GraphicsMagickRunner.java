package de.scyv.scarchive.server;

import java.nio.file.Path;
import java.nio.file.Paths;

public class GraphicsMagickRunner extends ProcessRunner {

	private Path filePath;

	public GraphicsMagickRunner(Path filePath) {
		this.filePath = filePath;
	}

	public GraphicsMagickRunner prepareForOCR() {
		setCommand(new String[] { //
				"/usr/local/bin/gm", "convert", //
				"-auto-orient", //
				"-density", "300", //
				"-depth", "4", //
				"-colorspace", "gray", //
				"-filter", "triangle", //
				"-resize", "900%", //
				"-contrast", //
				"-sharpen", "5", //
				"-verbose", //
				filePath.toString(), //
				filePath.toString() //
		});
		return this;
	}

	public GraphicsMagickRunner prepareForThumbnail() {
		setCommand(new String[] { //
				"/usr/local/bin/gm", "convert", //
				"-size", "120x120", //
				filePath.toString(), //
				"-resize", "120x120", //
				"+profile", "'*'", //
				Paths.get(filePath.getParent().toString(), "thumb_" + filePath.getFileName()).toString() //
		});
		return this;
	}

}
