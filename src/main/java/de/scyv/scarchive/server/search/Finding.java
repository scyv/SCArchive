package de.scyv.scarchive.server.search;

import java.util.Comparator;

import de.scyv.scarchive.server.MetaData;

/**
 * Represents a finding of a document/note.
 */
public class Finding {

    public static class LatestUpdateComparator implements Comparator<Finding> {

        @Override
        public int compare(Finding o1, Finding o2) {
            if (o1 == null && o2 != null) {
                return 1;
            } else if (o2 == null && o1 != null) {
                return -1;
            } else if (o2 == null && o1 == null) {
                return 0;
            }

            int compared = o2.getMetaData().getLastUpdateMetaData().compareTo(o1.getMetaData().getLastUpdateMetaData());

            if (compared == 0) {
                compared = o1.getMetaData().getFilePath().toString()
                        .compareToIgnoreCase(o2.getMetaData().getFilePath().toString());
            }
            return compared;
        }

    }

    private MetaData metaData;

    private String context;

    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    @Override
    public int hashCode() {
        return this.getMetaData().getFilePath().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.getMetaData().getFilePath().equals(((Finding) obj).getMetaData().getFilePath());
    }

}
