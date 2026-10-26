package com.movieorganizer.tmdb;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TmdbClient {

    private static final String BASE_URL = "https://api.themoviedb.org/3";
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/original";

    /** Ayni ada sahip birden fazla sonuc bulundugunda islenecek azami aday sayisi. */
    private static final int MAX_AMBIGUOUS_CANDIDATES = 8;

    private final String apiKey;
    private final HttpClient httpClient;

    public TmdbClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Film arar. Sorgu adiyla (yerellestirilmis veya orijinal baslik) tam eslesen
     * birden fazla sonuc varsa hepsi dondurulur (belirsiz durum); aksi halde tek
     * elemanli liste (ilk/eslesen sonuc) veya bos liste doner.
     */
    public List<TmdbMovieResult> searchMovies(String name, int year) throws Exception {
        String query = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String url = BASE_URL + "/search/movie?api_key=" + apiKey + "&query=" + query + "&year=" + year;

        JsonObject json = JsonParser.parseString(get(url)).getAsJsonObject();
        JsonArray results = json.getAsJsonArray("results");

        List<TmdbMovieResult> all = new ArrayList<>();
        if (results == null || results.isEmpty()) {
            return all;
        }
        for (JsonElement el : results) {
            JsonObject o = el.getAsJsonObject();
            int id = o.get("id").getAsInt();
            String title = o.has("title") ? o.get("title").getAsString() : name;
            String originalTitle = getString(o, "original_title");
            String releaseDate = getString(o, "release_date");
            String posterPath = getString(o, "poster_path");
            int releaseYear = releaseDate.length() >= 4 ? Integer.parseInt(releaseDate.substring(0, 4)) : year;
            Double voteAverage = getDouble(o, "vote_average");
            all.add(new TmdbMovieResult(id, title, originalTitle, releaseYear, posterPath, voteAverage));
        }

        List<TmdbMovieResult> matches = new ArrayList<>();
        for (TmdbMovieResult r : all) {
            if (titleMatches(name, r.getTitle(), r.getOriginalTitle())) {
                matches.add(r);
            }
        }

        if (matches.size() >= 2) {
            return matches.size() > MAX_AMBIGUOUS_CANDIDATES
                ? new ArrayList<>(matches.subList(0, MAX_AMBIGUOUS_CANDIDATES))
                : matches;
        }
        return List.of(matches.isEmpty() ? all.get(0) : matches.get(0));
    }

    /**
     * Dizi arar. Sorgu adiyla tam eslesen birden fazla sonuc varsa her biri icin
     * detay cekilip hepsi dondurulur (belirsiz durum); aksi halde tek elemanli
     * liste veya bos liste doner.
     */
    public List<TmdbTvResult> searchTvShows(String name) throws Exception {
        String query = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String url = BASE_URL + "/search/tv?api_key=" + apiKey + "&query=" + query;

        JsonObject json = JsonParser.parseString(get(url)).getAsJsonObject();
        JsonArray results = json.getAsJsonArray("results");

        List<TmdbTvResult> out = new ArrayList<>();
        if (results == null || results.isEmpty()) {
            return out;
        }

        List<JsonObject> matches = new ArrayList<>();
        for (JsonElement el : results) {
            JsonObject o = el.getAsJsonObject();
            if (titleMatches(name, getString(o, "name"), getString(o, "original_name"))) {
                matches.add(o);
            }
        }

        List<JsonObject> chosen;
        if (matches.size() >= 2) {
            chosen = matches.size() > MAX_AMBIGUOUS_CANDIDATES
                ? matches.subList(0, MAX_AMBIGUOUS_CANDIDATES)
                : matches;
        } else if (matches.size() == 1) {
            chosen = matches;
        } else {
            chosen = List.of(results.get(0).getAsJsonObject());
        }

        for (JsonObject o : chosen) {
            out.add(getTvDetails(o.get("id").getAsInt()));
        }
        return out;
    }

    private TmdbTvResult getTvDetails(int id) throws Exception {
        String url = BASE_URL + "/tv/" + id + "?api_key=" + apiKey;
        JsonObject json = JsonParser.parseString(get(url)).getAsJsonObject();

        String name = getString(json, "name");
        String originalName = getString(json, "original_name");
        String firstAirDate = getString(json, "first_air_date");
        int numberOfSeasons = json.has("number_of_seasons") ? json.get("number_of_seasons").getAsInt() : 1;
        String posterPath = getString(json, "poster_path");
        int firstAirYear = firstAirDate.length() >= 4 ? Integer.parseInt(firstAirDate.substring(0, 4)) : 0;
        Double voteAverage = getDouble(json, "vote_average");

        return new TmdbTvResult(id, name, originalName, firstAirYear, numberOfSeasons, posterPath, voteAverage);
    }

    /** Sorgu adi, verilen basliklardan biriyle (slug bazinda) tam esitse true. */
    private static boolean titleMatches(String query, String... titles) {
        String q = TmdbLink.slugify(query);
        if (q.isEmpty()) {
            return false;
        }
        for (String t : titles) {
            if (t != null && TmdbLink.slugify(t).equals(q)) {
                return true;
            }
        }
        return false;
    }

    public void downloadPoster(String posterPath, Path destination) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(IMAGE_BASE_URL + posterPath))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Poster indirilemedi: HTTP " + response.statusCode());
        }

        try (InputStream in = response.body()) {
            Files.copy(in, destination);
        }
    }

    private String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " - " + url);
        }
        return response.body();
    }

    private String getString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }

    private Double getDouble(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsDouble();
        }
        return null;
    }
}
