package de.scyv.scarchive.search;

import de.scyv.scarchive.server.MetaData;

/**
 * Represents a finding of a document/note.
 */
public class Finding {

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

}
