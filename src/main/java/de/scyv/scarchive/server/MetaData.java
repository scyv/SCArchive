package de.scyv.scarchive.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Representation of metadata of a document.
 */
public class MetaData {

    private String text = "";

    private String title;

    private List<String> thumbnailPaths = new ArrayList<>();

    private List<String> tags = new ArrayList<>();

    // set at runtime
    @JsonIgnore
    private Path filePath;

    // set at runtime
    @JsonIgnore
    private Date lastUpdateMetaData;

    /**
     * Factory Method that creates a metaData instance from a given json file.
     *
     * @param metaDataFile
     *            the file to load. Must not be <code>null</code>.
     * @return Instance of the metaData
     * @throws IOException
     *             when the file cannot be read.
     */
    public static MetaData createFromFile(Path metaDataFile) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final MetaData metaData = mapper.readValue(metaDataFile.toFile(), MetaData.class);
        metaData.setFilePath(metaDataFile);
        metaData.setLastUpdateMetaData(new Date(Files.getLastModifiedTime(metaDataFile).toMillis()));
        if (metaData.getThumbnailPaths() != null && metaData.getThumbnailPaths().size() > 0) {
            final List<String> newPaths = new ArrayList<>();
            for (final String path : metaData.getThumbnailPaths()) {
                newPaths.add(Paths.get(path).getFileName().toString());
            }
            metaData.setThumbnailPaths(newPaths);
        }
        return metaData;
    }

    /**
     * Save metaData to a json file.
     *
     * @param metaDataFile
     *            path where the data shall be stored. Must not be
     *            <code>null</code>.
     * @throws IOException
     *             when writing to the json file fails.
     */
    public void saveToFile(Path metaDataFile) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        this.setLastUpdateMetaData(new Date());
        this.setFilePath(metaDataFile);
        mapper.writeValue(metaDataFile.toFile(), this);
    }

    public List<String> getThumbnailPaths() {
        return thumbnailPaths;
    }

    public void setThumbnailPaths(List<String> thumbnailPaths) {
        this.thumbnailPaths = thumbnailPaths;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Date getLastUpdateMetaData() {
        return lastUpdateMetaData;
    }

    public void setLastUpdateMetaData(Date lastUpdateMetaData) {
        this.lastUpdateMetaData = lastUpdateMetaData;
    }

    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

}
