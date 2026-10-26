package com.movieorganizer.parser;

import com.movieorganizer.model.MediaFile;
import com.movieorganizer.model.MovieFile;
import com.movieorganizer.model.TvShowFile;
import org.junit.Test;

import static org.junit.Assert.*;

public class FileNameParserTest {

    @Test
    public void testMovieSingleWord() {
        MovieFile m = (MovieFile) FileNameParser.parse("Emancipation.2022.1080p.WebRip.x264.DUAL.Filmbol.mkv");
        assertNotNull(m);
        assertFalse(m.isTvShow());
        assertEquals("Emancipation", m.getCleanName());
        assertEquals(2022, m.getYear());
        assertEquals("1080p.WebRip.x264.DUAL.Filmbol", m.getQualityInfo());
    }

    @Test
    public void testMovieMultiWord() {
        MovieFile m = (MovieFile) FileNameParser.parse("Eagles.of.the.Republic.2026.1080p.WebRip.x264.DUAL.Filmbol.mkv");
        assertNotNull(m);
        assertEquals("Eagles of the Republic", m.getCleanName());
        assertEquals(2026, m.getYear());
        assertEquals("1080p.WebRip.x264.DUAL.Filmbol", m.getQualityInfo());
    }

    @Test
    public void testMovieWithSpecialCharsInQuality() {
        MovieFile m = (MovieFile) FileNameParser.parse("Dracula.2025.1080p.WebRip.x264.ENG.Türkçe.Altyazı.Filmbol.mkv");
        assertNotNull(m);
        assertEquals("Dracula", m.getCleanName());
        assertEquals(2025, m.getYear());
        assertEquals("1080p.WebRip.x264.ENG.Türkçe.Altyazı.Filmbol", m.getQualityInfo());
    }

    @Test
    public void testTvShowSimpleName() {
        TvShowFile tv = (TvShowFile) FileNameParser.parse("DMV.S01E13.WEB-DL.1080p.DUAL.H.264-HDM.mkv");
        assertNotNull(tv);
        assertTrue(tv.isTvShow());
        assertEquals("DMV", tv.getCleanName());
        assertEquals(1, tv.getSeason());
        assertEquals(13, tv.getEpisode());
    }

    @Test
    public void testTvShowNameWithDots() {
        TvShowFile tv = (TvShowFile) FileNameParser.parse("DTF.St.Louis.S01E07.WEB-DL.1080p.DUAL.H.265-HDM.mkv");
        assertNotNull(tv);
        assertEquals("DTF St Louis", tv.getCleanName());
        assertEquals(1, tv.getSeason());
        assertEquals(7, tv.getEpisode());
    }

    @Test
    public void testTvShowHighSeason() {
        TvShowFile tv = (TvShowFile) FileNameParser.parse("Euphoria.S03E01.WEB-DL.1080p.DUAL.H.265-HDM.mkv");
        assertNotNull(tv);
        assertEquals("Euphoria", tv.getCleanName());
        assertEquals(3, tv.getSeason());
        assertEquals(1, tv.getEpisode());
    }

    @Test
    public void testUnknownPatternReturnsNull() {
        assertNull(FileNameParser.parse("random_file.mkv"));
        assertNull(FileNameParser.parse("no.extension"));
    }
}
