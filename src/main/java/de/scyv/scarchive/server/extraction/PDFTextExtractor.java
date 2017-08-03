package de.scyv.scarchive.server.extraction;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.scyv.scarchive.server.MetaData;
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
public class PDFTextExtractor implements Extractor {
    private static Logger LOGGER = LoggerFactory.getLogger(PDFTextExtractor.class);

    @Value("${scarchive.tesseract.bin}")
    private String tesseractBin;

    @Value("${scarchive.graphicsmagick.bin}")
    private String graphicsmagickBin;

    @Override
    public String getIdentifier() {
        return "PDF";
    }

    @Override
    public boolean accepts(Path path) {
        return path.getFileName().toString().toLowerCase().endsWith(".pdf");
    }

    /**
     * Extract text from the given pdf document.
     *
     * @param path
     *            the path to the pdf document.
     */
    @Override
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

            try (PDDocument doc = PDDocument.load(new FileInputStream(path.toFile()))) {
                final PDFTextStripper stripper = new PDFTextStripper();
                final String text = stripper.getText(doc);
                if (text.trim().isEmpty()) {
                    // if we cannot find any text, do ocr over every page
                    iteratePages(doc.getPages(), metaDataPath, metaData);
                } else {
                    Files.write(getTextDataFile(path), text.getBytes("UTF-8"));
                    // TODO create thumbnail from first page
                }
            }
            LOGGER.info("Writing meta data...");
            metaData.setLastUpdateFile(new Date(Files.getLastModifiedTime(path).toMillis()));
            metaData.saveToFile(Paths.get(metaDataPath + ".json"));
        } catch (final Exception ex) {

            LOGGER.error("Could not extract file " + path, ex);
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
            try {
                final PDXObject obj = resources.getXObject(name);
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
        if (imgCount.get() == 1) {
            createThumbnail(pageImageFile.toPath(), metaData);
        }
        doOCR(pageImageFile.toPath());
        pageImageFile.delete();

    }

    private boolean isAlreadyExtracted(Path path) {
        return Files.exists(getMetaDataPath(Paths.get(path.toString() + ".json")));
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

    private void createThumbnail(Path filePath, MetaData metaData) throws IOException, InterruptedException {
        new GraphicsMagickRunner(filePath, graphicsmagickBin).prepareForThumbnail().run();
        metaData.getThumbnailPaths()
                .add(Paths.get(filePath.getParent().toString(), "thumb_" + filePath.getFileName()).toString());

    }

}
