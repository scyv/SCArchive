package de.scyv.scarchive.server.extraction;

import java.nio.file.Path;

/**
 * Interface for defining an extractor (i.e. some thing that can extract
 * information out of a given path/file)
 */
public interface Extractor {

    /**
     * Get an unique identifier for the extractor.
     */
    String getIdentifier();

    /**
     * Run extraction with the given path.
     *
     * @param path
     *            the path to run the extraction for (must not be <code>null</code>)
     */
    void extract(Path path);

    /**
     * See whether the extractor accepts the path for extraction.
     *
     * @param path
     *            the path to check. Must not be <code>null</code>.
     * @return true, if the path/file can be extracted by the extractor.
     */
    boolean accepts(Path path);

}
