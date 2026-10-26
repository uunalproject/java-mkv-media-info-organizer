package com.movieorganizer.tmdb;

public class TmdbMovieResult {
    private final int id;
    private final String title;
    private final String originalTitle;
    private final int year;
    private final String posterPath;
    private final Double voteAverage;

    public TmdbMovieResult(int id, String title, String originalTitle, int year, String posterPath, Double voteAverage) {
        this.id = id;
        this.title = title;
        this.originalTitle = originalTitle;
        this.year = year;
        this.posterPath = posterPath;
        this.voteAverage = voteAverage;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    /** TMDB'nin orijinal dildeki basligi (yerellestirilmemis). Bos olabilir. */
    public String getOriginalTitle() { return originalTitle; }
    public int getYear() { return year; }
    public String getPosterPath() { return posterPath; }
    public Double getVoteAverage() { return voteAverage; }
}
