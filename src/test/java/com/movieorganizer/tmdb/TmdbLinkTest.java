package com.movieorganizer.tmdb;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TmdbLinkTest {

    @Test
    public void testMovieExampleFromSpec() {
        assertEquals("3021-1408", TmdbLink.idPart(3021, "1408"));
        assertEquals("movie_3021-1408.tmdb", TmdbLink.fileName("movie", 3021, "1408"));
        assertEquals("https://www.themoviedb.org/movie/3021-1408", TmdbLink.webUrl("movie", 3021, "1408"));
    }

    @Test
    public void testSlugifyMultiWordAndCase() {
        assertEquals("the-dark-knight", TmdbLink.slugify("The Dark Knight"));
        assertEquals("dtf-st-louis", TmdbLink.slugify("DTF St Louis"));
    }

    @Test
    public void testSlugifyAccentsAndPunctuation() {
        // Aksanlar temizlenir; ASCII karsiligi olmayan harfler (ı) dusurulur.
        assertEquals("turkce-altyaz", TmdbLink.slugify("Türkçe Altyazı"));
        assertEquals("wall-e", TmdbLink.slugify("WALL·E"));
    }

    @Test
    public void testSlugifyIsLocaleIndependent() {
        // Turkce locale'de "I".toLowerCase() -> "ı" olur; Locale.ROOT ile ASCII korunur.
        assertEquals("titanic", TmdbLink.slugify("TITANIC"));
    }

    @Test
    public void testEmptyTitleFallsBackToIdOnly() {
        assertEquals("3021", TmdbLink.idPart(3021, ""));
        assertEquals("tv_1399.tmdb", TmdbLink.fileName("tv", 1399, null));
        assertEquals("https://www.themoviedb.org/tv/1399", TmdbLink.webUrl("tv", 1399, "   "));
    }

    @Test
    public void testTvExample() {
        assertEquals("https://www.themoviedb.org/tv/1399-game-of-thrones",
            TmdbLink.webUrl("tv", 1399, "Game of Thrones"));
    }
}
