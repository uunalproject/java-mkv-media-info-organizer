package com.movieorganizer.tmdb;

import java.text.Normalizer;
import java.util.Locale;

/**
 * TMDB web sayfasi adresi ve ".tmdb" link dosyasi adi uretir.
 *
 * Ornek: 1408 filmi -> id=3021, baslik="1408"
 *   idPart  : "movie_3021-1408"  (tur onekli)
 *   fileName : "movie_3021-1408.tmdb"
 *   webUrl  : "https://www.themoviedb.org/movie/3021-1408"
 *
 * Not: TMDB URL'sinde slug kismi yok sayilir; sayfa sayisal id ile cozumlenir,
 * dolayisiyla yaklasik uretilen slug ile de dogru sayfaya yonlenir.
 */
public final class TmdbLink {

    private static final String WEB_BASE_URL = "https://www.themoviedb.org";

    private TmdbLink() {
    }

    /** "movie" veya "tv". */
    public static String fileName(String type, int id, String title) {
        return type + "_" + idPart(id, title) + ".tmdb";
    }

    public static String webUrl(String type, int id, String title) {
        return WEB_BASE_URL + "/" + type + "/" + idPart(id, title);
    }

    /** Ornek: 3021 + "1408" -> "3021-1408"; slug bossa sadece "3021". */
    public static String idPart(int id, String title) {
        String slug = slugify(title);
        return slug.isEmpty() ? String.valueOf(id) : id + "-" + slug;
    }

    /** Kucuk harf, aksan temizligi, alfanumerik disi gruplar tek '-', bas/son '-' atilir. */
    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String noAccents = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return noAccents.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+|-+$)", "");
    }
}
