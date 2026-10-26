package com.movieorganizer.model;

public class TvShowFile extends MediaFile {
    private final int season;
    private final int episode;
    private final String releaseInfo;

    public TvShowFile(String originalFilename, String cleanName, int season, int episode, String releaseInfo) {
        super(originalFilename, cleanName);
        this.season = season;
        this.episode = episode;
        this.releaseInfo = releaseInfo;
    }

    public int getSeason() { return season; }
    public int getEpisode() { return episode; }
    public String getReleaseInfo() { return releaseInfo; }

    @Override
    public boolean isTvShow() { return true; }
}
