package de.scyv.scarchive.server.extraction;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.scyv.scarchive.server.MetaData;
import de.scyv.scarchive.server.MetaDataService;
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

    private final MetaDataService metaDataService;

    public PDFTextExtractor(MetaDataService metaDataService) {
        this.metaDataService = metaDataService;
    }

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
        LOGGER.info("Extracting " + path);
        try {
            final MetaData metaData = new MetaData();
            metaData.setTitle(path.getFileName().toString());
            metaData.setFilePath(path);

            final Path metaDataPath = metaDataService.getMetaDataPathPrefix(path);
            metaDataPath.getParent().toFile().mkdirs();

            try (PDDocument doc = PDDocument.load(new FileInputStream(path.toFile()))) {
                final PDFTextStripper stripper = new PDFTextStripper();
                final String text = stripper.getText(doc);
                if (text.trim().isEmpty()) {
                    // if we cannot find any text, do ocr over every page
                    iteratePages(doc, metaDataPath, metaData);
                } else {
                    Files.write(Paths.get(metaDataPath + "_1.png.txt"), text.getBytes("UTF-8"));
                    final PDFRenderer renderer = new PDFRenderer(doc);
                    final BufferedImage image = renderer.renderImageWithDPI(0, 300);
                    processImage(metaDataPath, metaData, 1, image, false);
                }
            }
            final Path metaDataJsonPath = metaDataService.getMetaDataPath(path);
            LOGGER.info("Writing meta data: " + metaDataJsonPath);
            metaData.saveToFile(metaDataJsonPath);
        } catch (final Exception ex) {
            LOGGER.error("Could not extract file " + path, ex);
        }
    }

    private void iteratePages(PDDocument doc, Path metaDataPath, MetaData metaData) {
        final PDFRenderer renderer = new PDFRenderer(doc);
        for (int pageNr = 0; pageNr < doc.getNumberOfPages(); pageNr++) {
            try {
                final BufferedImage image = renderer.renderImageWithDPI(pageNr, 300);
                processImage(metaDataPath, metaData, pageNr + 1, image, true);
            } catch (InterruptedException | IOException ex) {
                LOGGER.error("Cannot render page " + pageNr + " of document.", ex);
            }
        }

    }

    private void processImage(Path metaDataPath, MetaData metaData, int imgCount, BufferedImage image, boolean withOCR)
            throws IOException, InterruptedException {
        final File pageImageFile = new File(metaDataPath + "_" + imgCount + ".png");
        LOGGER.info("Writing image " + pageImageFile.getAbsolutePath());
        ImageIO.write(image, "png", pageImageFile);
        if (imgCount == 1) {
            createThumbnail(pageImageFile.toPath(), metaData);
        }
        if (withOCR) {
            doOCR(pageImageFile.toPath());
        }
        pageImageFile.delete();
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
