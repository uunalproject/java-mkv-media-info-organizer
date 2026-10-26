package com.movieorganizer.parser;

import com.movieorganizer.model.MediaFile;
import com.movieorganizer.model.MovieFile;
import com.movieorganizer.model.TvShowFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileNameParser {

    // Matches: ShowName.SxxEyy.releaseInfo.mkv
    private static final Pattern TV_PATTERN = Pattern.compile(
            "^(.+?)\\.S(\\d{2})E(\\d{2})\\.(.+)\\.mkv$", Pattern.CASE_INSENSITIVE
        );

    private static final Pattern TV_PATTERN3 = Pattern.compile(
            "^(.+?)\\.S(\\d{2})B(\\d{2})\\.(.+)\\.mkv$", Pattern.CASE_INSENSITIVE
        );

    // Matches: ShowName.yyyy.SxxEyy.releaseInfo.mkv
    private static final Pattern TV_PATTERN2 = Pattern.compile(
            "^(.+?)\\.(\\d{4})\\.S(\\d{2})E(\\d{2})\\.(.+)\\.mkv$", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TV_PATTERN6 = Pattern.compile(
            "^(.+?)\\.(\\d{4})\\.S(\\d{2})\\.E(\\d{2})\\.(.+)\\.mkv$", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TV_PATTERN4 = Pattern.compile(
            "^(.+?)\\.(\\d{4})\\.S(\\d{2})B(\\d{2})\\.(.+)\\.mkv$", Pattern.CASE_INSENSITIVE
    );

    // Matches: MovieName.yyyy.qualityInfo.mkv
    private static final Pattern MOVIE_PATTERN = Pattern.compile(
        "^(.+?)\\.(\\d{4})\\.(.+)\\.mkv$", Pattern.CASE_INSENSITIVE
    );

    public static MediaFile parse(String filename) {
        Matcher tvMatcher6 = TV_PATTERN6.matcher(filename);
        if (tvMatcher6.matches()) {
            String cleanName = tvMatcher6.group(1).replace('.', ' ');
            int season = Integer.parseInt(tvMatcher6.group(3));
            int episode = Integer.parseInt(tvMatcher6.group(4));
            String releaseInfo = tvMatcher6.group(5);
            return new TvShowFile(filename, cleanName, season, episode, releaseInfo);
        }

    	Matcher tvMatcher2 = TV_PATTERN2.matcher(filename);
        if (tvMatcher2.matches()) {
            String cleanName = tvMatcher2.group(1).replace('.', ' ');
            int season = Integer.parseInt(tvMatcher2.group(3));
            int episode = Integer.parseInt(tvMatcher2.group(4));
            String releaseInfo = tvMatcher2.group(5);
            return new TvShowFile(filename, cleanName, season, episode, releaseInfo);
        }
    	
        Matcher tvMatcher = TV_PATTERN.matcher(filename);
        if (tvMatcher.matches()) {
            String cleanName = tvMatcher.group(1).replace('.', ' ');
            int season = Integer.parseInt(tvMatcher.group(2));
            int episode = Integer.parseInt(tvMatcher.group(3));
            String releaseInfo = tvMatcher.group(4);
            return new TvShowFile(filename, cleanName, season, episode, releaseInfo);
        }

        Matcher tvMatcher4 = TV_PATTERN4.matcher(filename);
        if (tvMatcher4.matches()) {
            String cleanName = tvMatcher4.group(1).replace('.', ' ');
            int season = Integer.parseInt(tvMatcher4.group(3));
            int episode = Integer.parseInt(tvMatcher4.group(4));
            String releaseInfo = tvMatcher4.group(5);
            return new TvShowFile(filename, cleanName, season, episode, releaseInfo);
        }
    	
        Matcher tvMatcher3 = TV_PATTERN3.matcher(filename);
        if (tvMatcher3.matches()) {
            String cleanName = tvMatcher3.group(1).replace('.', ' ');
            int season = Integer.parseInt(tvMatcher3.group(2));
            int episode = Integer.parseInt(tvMatcher3.group(3));
            String releaseInfo = tvMatcher3.group(4);
            return new TvShowFile(filename, cleanName, season, episode, releaseInfo);
        }

        Matcher movieMatcher = MOVIE_PATTERN.matcher(filename);
        if (movieMatcher.matches()) {
            String cleanName = movieMatcher.group(1).replace('.', ' ');
            int year = Integer.parseInt(movieMatcher.group(2));
            String qualityInfo = movieMatcher.group(3);
            return new MovieFile(filename, cleanName, year, qualityInfo);
        }

        return null;
    }
}
