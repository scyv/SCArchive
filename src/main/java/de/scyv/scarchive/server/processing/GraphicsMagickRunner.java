package de.scyv.scarchive.server.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs GraphicMagick on commandline.
 */

public class GraphicsMagickRunner extends ProcessRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphicsMagickRunner.class);

	private final Path filePath;

	private final String graphicsmagickBin;

	/**
	 * Create instance.
	 *
	 * @param filePath
	 *            the file to be processed by gm
	 * @param graphicsmagickBin
	 *            the binary of GraphicsMagick
	 */
	public GraphicsMagickRunner(Path filePath, String graphicsmagickBin) {
		this.filePath = filePath;
		this.graphicsmagickBin = graphicsmagickBin;
	}

	@Override
	public int run() throws IOException, InterruptedException {
		LOGGER.debug("Running GraphicsMagick with command: " + String.join(" ", command));
		return super.run();
	}

	/**
	 * Prepare command for preparing the image for use with OCR.
	 *
	 * @return the runner instance
	 */
	public GraphicsMagickRunner prepareForOCR() {
		setCommand(new String[] { //
				graphicsmagickBin, "convert", //
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

	/**
	 * Prepare command for creating thumbnails.
	 *
	 * @return the runner instance.
	 */
	public GraphicsMagickRunner prepareForThumbnail() {
		setCommand(new String[] { //
				graphicsmagickBin, "convert", //
				"-size", "120x120", //
				filePath.toString(), //
				"-resize", "120x120", //
				"+profile", "'*'", //
				Paths.get(filePath.getParent().toString(), "thumb_" + filePath.getFileName()).toString() //
		});
		return this;
	}

}
