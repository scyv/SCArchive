package de.scyv.scarchive.server;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Representation of metadata of a document.
 */
public class MetaData {

    private String filePath;
    private List<String> thumbnailPaths = new ArrayList<>();

    private String text = "";

    private String title;

    private Date lastUpdateMetaData;

    private Date lastUpdateFile;

    private List<String> tags = new ArrayList<>();

    /**
     * Factory Method that creates a metaData instance from a given json file.
     *
     * @param metaDataFile
     *            the file to load.
     * @return Instance of the metaData
     * @throws IOException
     *             when the file cannot be read.
     */
    public static MetaData createFromFile(Path metaDataFile) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(metaDataFile.toFile(), MetaData.class);
    }

    /**
     * Save metaData to a json file.
     *
     * @param metaDataFile
     *            path where the data shall be stored.
     * @throws IOException
     *             when writing to the json file fails.
     */
    public void saveToFile(Path metaDataFile) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        this.setLastUpdateMetaData(new Date());
        mapper.writeValue(metaDataFile.toFile(), this);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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

    public Date getLastUpdateFile() {
        return lastUpdateFile;
    }

    public void setLastUpdateFile(Date lastUpdateFile) {
        this.lastUpdateFile = lastUpdateFile;
    }

}
