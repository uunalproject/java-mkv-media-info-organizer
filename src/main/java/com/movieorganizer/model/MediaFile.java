package com.movieorganizer.model;

public abstract class MediaFile {
    protected final String originalFilename;
    protected final String cleanName;

    protected MediaFile(String originalFilename, String cleanName) {
        this.originalFilename = originalFilename;
        this.cleanName = cleanName;
    }

    public String getOriginalFilename() { return originalFilename; }
    public String getCleanName() { return cleanName; }
    public abstract boolean isTvShow();
}
