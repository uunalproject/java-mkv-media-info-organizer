package com.movieorganizer.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MediaNamingTest {

    @Test
    public void testSanitizeRemovesWindowsIllegalChars() {
        assertEquals("DTF St. Louis", MediaNaming.sanitizePathSegment("DTF: St. Louis"));
        assertEquals("AC DC Live", MediaNaming.sanitizePathSegment("AC/DC\\Live"));
        assertEquals("What If", MediaNaming.sanitizePathSegment("What If?"));
    }

    @Test
    public void testSanitizeTrimsTrailingDotsAndSpaces() {
        assertEquals("Movie Name", MediaNaming.sanitizePathSegment("Movie Name...  "));
        assertEquals("", MediaNaming.sanitizePathSegment("   "));
        assertEquals("", MediaNaming.sanitizePathSegment(null));
    }

    @Test
    public void testKeepsCleanNameWhenNotTranslated() {
        // Yerellestirilmis ve orijinal baslik ayni -> dosya adindan gelen temiz ad korunur.
        assertEquals("DTF St Louis",
            MediaNaming.chooseFolderBaseName("DTF: St. Louis", "DTF: St. Louis", "DTF St Louis"));
        assertEquals("Matlock",
            MediaNaming.chooseFolderBaseName("Matlock", "Matlock", "Matlock"));
    }

    @Test
    public void testUsesOriginalTitleWhenTranslated() {
        // Turkce cevrilmis ad ile bulundu -> orijinal ad ile dizin.
        assertEquals("Emancipation",
            MediaNaming.chooseFolderBaseName("Esaret", "Emancipation", "Esaret"));
    }

    @Test
    public void testUsesOriginalTitleEvenWhenApiTitleIsAlreadyOriginal() {
        // TMDB araması en-US döndüğü için title == original_title = "In the Tall Grass";
        // dosya adı Türkçe -> yine orijinal başlıkla dizin oluşturulmalı.
        assertEquals("In the Tall Grass",
            MediaNaming.chooseFolderBaseName("In the Tall Grass", "In the Tall Grass", "Uzun Otların Arasında"));
    }

    @Test
    public void testIsLatin() {
        assertEquals(true, MediaNaming.isLatin("Uzun Otların Arasında 2: Dönüş"));
        assertEquals(true, MediaNaming.isLatin("1408"));
        assertEquals(false, MediaNaming.isLatin("기생충"));
        assertEquals(false, MediaNaming.isLatin("Брат 2"));
    }

    @Test
    public void testNonLatinOriginalTitleReplacedWithLatinTitle() {
        // Orijinal baslik Latin degil -> TMDB'nin Latin (en-US) basligi kullanilir.
        assertEquals("Parasite",
            MediaNaming.chooseFolderBaseName("Parasite", "기생충", "Parazit"));
        assertEquals("Brother",
            MediaNaming.chooseFolderBaseName("Brother", "Брат", null));
        // Latin karsilik dosya adiyla ayniysa temiz ad korunur.
        assertEquals("Squid Game",
            MediaNaming.chooseFolderBaseName("Squid Game", "오징어 게임", "Squid Game"));
    }

    @Test
    public void testNonLatinOriginalWithoutLatinTitleKeepsCleanName() {
        // TMDB'de Latin baslik yok -> dosya adindan gelen ad korunur.
        assertEquals("Brat",
            MediaNaming.chooseFolderBaseName("Брат", "Брат", "Brat"));
    }

    @Test
    public void testFallsBackToLocalizedWhenNoCleanName() {
        assertEquals("Some Movie",
            MediaNaming.chooseFolderBaseName("Some Movie", "", null));
        assertEquals("unknown",
            MediaNaming.chooseFolderBaseName("", "", ""));
    }
}
