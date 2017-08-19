package de.scyv.scarchive.server;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.scyv.scarchive.server.extraction.Extractor;

/**
 * Container class for files that are to be extracted.
 */
public class ExtractionCollection {

    private final Map<String, Set<Path>> collection = new HashMap<>();

    /**
     * Add path to the collection.
     *
     * @param extractor
     *            must not be <code>null</code>
     * @param path
     *            must not be <code>null</code>
     */
    public void add(Extractor extractor, Path path) {
        Set<Path> list = collection.get(extractor.getIdentifier());
        if (list == null) {
            list = new TreeSet<>();
            collection.put(extractor.getIdentifier(), list);
        }
        list.add(path);
    }

    /**
     * Number of paths added to the collection.
     *
     * @return the size.
     */
    public int size() {
        return collection.values().stream().map(set -> set.size()).reduce(0, (a, b) -> (a + b));
    }

    /**
     * The size of collection for a given extractor.
     *
     * @param extractor
     *            the extractor to check. Must not be <code>null</code>.
     * @return the size. 0 if no elemets are added to an extractor, or the extractor
     *         is not added in the list at all.
     */
    public int size(Extractor extractor) {
        final Set<Path> set = collection.get(extractor.getIdentifier());
        if (set == null) {
            return 0;
        }
        return set.size();
    }

    /**
     * Retrieve the set of paths for a given extractor. Must not be
     * <code>null</code>.
     *
     * @param extractor
     *            the extractor to get the paths from.
     * @return an unmodifiable set of paths.
     */
    public Set<Path> get(Extractor extractor) {
        return Collections.unmodifiableSet(collection.get(extractor.getIdentifier()));
    }
}
