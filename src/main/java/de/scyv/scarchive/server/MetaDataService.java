package de.scyv.scarchive.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;

/**
 * Service that provides means to find/handle meta data files
 *
 */
@Service
public class MetaDataService {

    public static final String META_DATA_SUB_DIRECTORY = ".scarchive";

    /**
     * Check if a file has already been processed by extraction service.
     *
     * This is determined by the pure existence of the metadata file
     *
     * @param originalFilePath.
     *            Must not be <code>null</code>.
     * @return true, if the file has been processed.
     */
    public boolean isAlreadyExtracted(Path originalFilePath) {
        return Files.exists(getMetaDataPath(originalFilePath));
    }

    /**
     * Get the full filepath of the metadata json file, by its origin file.
     *
     * @param originalFilePath.
     *            Must not be <code>null</code>.
     * @return never <code>null</code>.
     */
    public Path getMetaDataPath(Path originalFilePath) {
        return Paths.get(originalFilePath.getParent().toString(), META_DATA_SUB_DIRECTORY,
                originalFilePath.getFileName().toString() + ".meta.json");
    }

    /**
     * Get metadata file prefix.
     *
     * This is the originalFilePath with the metadata subdirectory "injected".
     *
     * @param originalFilePath.
     *            Must not be <code>null</code>.
     * @return never <code>null</code>.
     *
     */
    public Path getMetaDataPathPrefix(Path originalFilePath) {
        return Paths.get(originalFilePath.getParent().toString(), META_DATA_SUB_DIRECTORY,
                originalFilePath.getFileName().toString());
    }

    /**
     * Get the original filepath from the metadata file.
     * 
     * @param metaData
     * @return
     */
    public Path getOriginalFilePath(MetaData metaData) {
        if (metaData.getFilePath() == null) {
            throw new IllegalStateException("Filepath of metadata is not set!");
        }
        return Paths.get(metaData.getFilePath().getParent().getParent().toString(),
                metaData.getFilePath().getFileName().toString().replaceAll("\\.meta\\.json$", ""));
    }

}
