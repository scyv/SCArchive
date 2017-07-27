package de.scyv.scarchive.server;

import java.nio.file.Path;

public class MetaData {

	private Path filePath;
	private Path thumbnailPath;

	public Path getFilePath() {
		return filePath;
	}

	public void setFilePath(Path filePath) {
		this.filePath = filePath;
	}

	public Path getThumbnailPath() {
		return thumbnailPath;
	}

	public void setThumbnailPath(Path thumbnailPath) {
		this.thumbnailPath = thumbnailPath;
	}

}
