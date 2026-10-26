package com.movieorganizer.model;

public class MovieFile extends MediaFile {
    private final int year;
    private final String qualityInfo;

    public MovieFile(String originalFilename, String cleanName, int year, String qualityInfo) {
        super(originalFilename, cleanName);
        this.year = year;
        this.qualityInfo = qualityInfo;
    }

    public int getYear() { return year; }
    public String getQualityInfo() { return qualityInfo; }

    @Override
    public boolean isTvShow() { return false; }
}
