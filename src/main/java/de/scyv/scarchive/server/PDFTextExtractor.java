package de.scyv.scarchive.server;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
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
import org.springframework.stereotype.Service;

@Service
public class PDFTextExtractor {
	private static Logger LOGGER = LoggerFactory.getLogger(PDFTextExtractor.class);

	public void extract(Path path) {

		if (isAlreadyExtracted(path)) {
			return;
		}

		LOGGER.info("Extracting " + path);

		try {
			MetaData metaData = new MetaData();
			metaData.setFilePath(path.toString());
			metaData.setTitle(path.getFileName().toString());

			Path metaDataPath = getMetaDataPath(path);
			PDDocument doc = PDDocument.load(new FileInputStream(path.toFile()));
			PDPageTree pages = doc.getPages();
			AtomicInteger imgCount = new AtomicInteger(0);
			AtomicInteger pageCount = new AtomicInteger(0);
			pages.forEach(page -> {
				LOGGER.info("Extracting " + path + " page: " + pageCount.incrementAndGet());
				PDResources resources = page.getResources();
				resources.getXObjectNames().forEach(name -> {
					PDXObject obj;
					try {
						obj = resources.getXObject(name);
						if (obj instanceof PDImageXObject) {
							String fileName = metaDataPath + "_" + imgCount.incrementAndGet() + ".png";
							LOGGER.info("Writing image " + fileName);
							RenderedImage image = (((PDImageXObject) obj).getImage());
							metaDataPath.getParent().toFile().mkdirs();
							File pageImageFile = new File(fileName);
							ImageIO.write(image, "png", pageImageFile);
							doOCR(pageImageFile.toPath());
							createThumbnail(pageImageFile.toPath());
							pageImageFile.delete();
							metaData.getThumbnailPaths().add(Paths
									.get(pageImageFile.getParent(), "thumb_" + pageImageFile.getName()).toString());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			});
			doc.close();
			LOGGER.info("Writing meta data");
			metaData.saveToFile(Paths.get(metaDataPath + ".json"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private boolean isAlreadyExtracted(Path path) {
		return getMetaDataFile(path).toFile().exists();
	}

	/**
	 * converts ./bla/blubb.pdf to ./bla/.scarchive/blubb.pdf
	 */
	private Path getMetaDataPath(Path path) {
		return Paths.get(path.getParent().toString(), ".scarchive", path.getFileName().toString());
	}

	private Path getMetaDataFile(Path path) {
		return Paths.get(path.getParent().toString(), ".scarchive", path.getFileName().toString() + "_1.png.txt");
	}

	private void doOCR(Path filePath) throws Exception {
		new GraphicsMagickRunner(filePath).prepareForOCR().run();
		new TesseractRunner(filePath).run();
	}

	private void createThumbnail(Path filePath) throws Exception {
		new GraphicsMagickRunner(filePath).prepareForThumbnail().run();
	}

}
