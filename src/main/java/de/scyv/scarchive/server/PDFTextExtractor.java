package de.scyv.scarchive.server;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.scyv.scarchive.server.processing.GraphicsMagickRunner;
import de.scyv.scarchive.server.processing.ProcessRunner;
import de.scyv.scarchive.server.processing.TesseractRunner;

/**
 * Extracts text from a PDF by doing OCR on each page.
 *
 * <ul>
 * <li>For extracting images from the PDF we use PDFBox
 * (https://pdfbox.apache.org/)</li>
 * <li>For preparing the images for OCR we use GraphicsMagick
 * (http://www.graphicsmagick.org/)</li>
 * <li>For OCR we use tesseract
 * (https://github.com/tesseract-ocr/tesseract)</li>
 * </ul>
 *
 * GraphicssMagick and tesseract are called via command line using
 * {@link ProcessRunner}.
 */
@Service
public class PDFTextExtractor {
	private static Logger LOGGER = LoggerFactory.getLogger(PDFTextExtractor.class);

	@Value("${scarchive.tesseract.bin}")
	private String tesseractBin;

	@Value("${scarchive.graphicsmagick.bin}")
	private String graphicsmagickBin;

	/**
	 * Extract text from the given pdf document.
	 *
	 * @param path
	 *            the path to the pdf document.
	 */
	public void extract(Path path) {

		if (isAlreadyExtracted(path)) {
			return;
		}

		LOGGER.info("Extracting " + path);

		try {
			final MetaData metaData = new MetaData();
			metaData.setFilePath(path.toString());
			metaData.setTitle(path.getFileName().toString());

			final Path metaDataPath = getMetaDataPath(path);
			metaDataPath.getParent().toFile().mkdirs();

			final PDDocument doc = PDDocument.load(new FileInputStream(path.toFile()));
			iteratePages(doc.getPages(), metaDataPath, metaData);

			doc.close();
			LOGGER.info("Writing meta data...");
			metaData.saveToFile(Paths.get(metaDataPath + ".json"));
		} catch (final Exception ex) {
			ex.printStackTrace();
		}

	}

	private void iteratePages(PDPageTree pages, Path metaDataPath, MetaData metaData) {
		final AtomicInteger imgCount = new AtomicInteger(0);
		final AtomicInteger pageCount = new AtomicInteger(0);
		pages.forEach(page -> {
			LOGGER.info("Extracting " + metaData.getFilePath() + " page: " + pageCount.incrementAndGet());
			iterateImages(page.getResources(), metaDataPath, metaData, imgCount);
		});

	}

	private void iterateImages(PDResources resources, Path metaDataPath, MetaData metaData, AtomicInteger imgCount) {
		resources.getXObjectNames().forEach(name -> {
			PDXObject obj;
			try {
				obj = resources.getXObject(name);
				if (obj instanceof PDImageXObject) {
					processImage(metaDataPath, metaData, imgCount, (((PDImageXObject) obj).getImage()));
				}
			} catch (InterruptedException | IOException ex) {
				LOGGER.error("Cannot process image of " + metaData.getFilePath(), ex);
			}
		});
	}

	private void processImage(Path metaDataPath, MetaData metaData, AtomicInteger imgCount, BufferedImage image)
			throws IOException, InterruptedException {
		final File pageImageFile = new File(metaDataPath + "_" + imgCount.incrementAndGet() + ".png");
		LOGGER.info("Writing image " + pageImageFile.getAbsolutePath());
		ImageIO.write(image, "png", pageImageFile);
		doOCR(pageImageFile.toPath());
		createThumbnail(pageImageFile.toPath());
		pageImageFile.delete();
		metaData.getThumbnailPaths()
				.add(Paths.get(pageImageFile.getParent(), "thumb_" + pageImageFile.getName()).toString());

	}

	private boolean isAlreadyExtracted(Path path) {
		return getTextDataFile(path).toFile().exists();
	}

	/**
	 * converts ./bla/blubb.pdf to ./bla/.scarchive/blubb.pdf
	 */
	private Path getMetaDataPath(Path path) {
		return Paths.get(path.getParent().toString(), ".scarchive", path.getFileName().toString());
	}

	private Path getTextDataFile(Path path) {
		return Paths.get(path.getParent().toString(), ".scarchive", path.getFileName().toString() + "_1.png.txt");
	}

	private void doOCR(Path filePath) throws IOException, InterruptedException {
		new GraphicsMagickRunner(filePath, graphicsmagickBin).prepareForOCR().run();
		new TesseractRunner(filePath, tesseractBin).run();
	}

	private void createThumbnail(Path filePath) throws IOException, InterruptedException {
		new GraphicsMagickRunner(filePath, graphicsmagickBin).prepareForThumbnail().run();
	}

}
