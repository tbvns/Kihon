package xyz.tbvns.kihon.logic.Object;

import androidx.documentfile.provider.DocumentFile;

public class RenderedFile {
    public String name;
    public String extension; // "pdf" or "epub"
    public long sizeBytes;
    public long lastModified; // milliseconds epoch
    public DocumentFile file;

    public RenderedFile(String name, String extension, long sizeBytes, long lastModified, DocumentFile file) {
        this.name = name;
        this.extension = extension;
        this.sizeBytes = sizeBytes;
        this.lastModified = lastModified;
        this.file = file;
    }

    /**
     * Returns human-readable size: e.g. "3.4 MB" or "1.2 GB"
     */
    public String getFormattedSize() {
        double mb = sizeBytes / (1024.0 * 1024.0);
        if (mb >= 1024.0) {
            return String.format("%.1f GB", mb / 1024.0);
        }
        return String.format("%.1f MB", mb);
    }
}