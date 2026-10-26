package com.movieorganizer.tmdb;

public class TmdbTvResult {
    private final int id;
    private final String name;
    private final String originalName;
    private final int firstAirYear;
    private final int numberOfSeasons;
    private final String posterPath;
    private final Double voteAverage;

    public TmdbTvResult(int id, String name, String originalName, int firstAirYear, int numberOfSeasons,
                        String posterPath, Double voteAverage) {
        this.id = id;
        this.name = name;
        this.originalName = originalName;
        this.firstAirYear = firstAirYear;
        this.numberOfSeasons = numberOfSeasons;
        this.posterPath = posterPath;
        this.voteAverage = voteAverage;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    /** TMDB'nin orijinal dildeki dizi adi (yerellestirilmemis). Bos olabilir. */
    public String getOriginalName() { return originalName; }
    public int getFirstAirYear() { return firstAirYear; }
    public int getNumberOfSeasons() { return numberOfSeasons; }
    public String getPosterPath() { return posterPath; }
    public Double getVoteAverage() { return voteAverage; }
}
