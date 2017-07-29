package de.scyv.scarchive.server;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MetaData {

	private String filePath;
	private List<String> thumbnailPaths = new ArrayList<>();

	private String text = "";

	private String title;

	private List<String> tags = new ArrayList<>();

	public static MetaData createFromFile(Path metaDataFile) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(metaDataFile.toFile(), MetaData.class);
	}

	public void saveToFile(Path metaDataFile) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
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

}
