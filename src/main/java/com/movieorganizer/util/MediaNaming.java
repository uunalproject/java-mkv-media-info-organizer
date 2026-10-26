package com.movieorganizer.util;

import com.movieorganizer.tmdb.TmdbLink;

import java.util.regex.Pattern;

/**
 * Dizin adi uretimiyle ilgili saf (I/O'suz) yardimcilar.
 */
public final class MediaNaming {

    private static final Pattern ILLEGAL_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern TRAILING_DOT_SPACE = Pattern.compile("[ .]+$");

    private MediaNaming() {
    }

    /**
     * Bir yol parcasini Windows dosya sistemi icin guvenli hale getirir:
     * yasakli karakterleri boslukla degistirir, bosluklari sadelestirir, sondaki nokta/bosluklari atar.
     */
    public static String sanitizePathSegment(String s) {
        if (s == null) {
            return "";
        }
        String r = ILLEGAL_CHARS.matcher(s).replaceAll(" ");
        r = MULTI_SPACE.matcher(r).replaceAll(" ").trim();
        r = TRAILING_DOT_SPACE.matcher(r).replaceAll("");
        return r;
    }

    /** Metindeki tum harfler Latin alfabesindense true (rakam/noktalama yok sayilir). */
    public static boolean isLatin(String s) {
        if (s == null) {
            return true;
        }
        return s.codePoints()
            .filter(Character::isLetter)
            .allMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.LATIN);
    }

    /**
     * Orijinal basligin Latin alfabesiyle yazilmis karsiligini doner.
     *
     * Orijinal baslik Latin alfabesindeyse aynen doner. Degilse (ornek: "기생충",
     * "Брат") TMDB'nin en-US basligi (title / name) Latin ise o kullanilir
     * (ornek: "Parasite", "Brother"). Ikisi de Latin degilse bos string doner.
     */
    public static String latinTitle(String originalTitle, String localizedTitle) {
        if (originalTitle != null && !originalTitle.isBlank() && isLatin(originalTitle)) {
            return originalTitle;
        }
        if (localizedTitle != null && !localizedTitle.isBlank() && isLatin(localizedTitle)) {
            return localizedTitle;
        }
        return "";
    }

    /**
     * Dizin adinin temel kismini secer.
     *
     * Dosya adindan gelen ad ile TMDB'nin ORIJINAL basligi (TMDB sayfasindaki
     * "Orijinal Baslik" / original_title / original_name) anlamli sekilde farkliysa
     * -- yani medyaya cevrilmis/yerel bir ad ile ulasilmissa -- orijinal baslik
     * kullanilir (ornek: "Uzun Otlarin Arasinda" -> "In the Tall Grass").
     * Adlar ozde ayniysa dosya adindan gelen temiz ad korunur (mevcut davranis,
     * ornegin "DTF St Louis" -> "DTF: St. Louis" noktalama farki yok sayilir).
     * Temiz ad yoksa orijinal baslige, o da yoksa yerellestirilmis baslige duselir.
     *
     * Orijinal baslik Latin alfabesiyle yazilmamissa onun yerine Latin karsiligi
     * ({@link #latinTitle}) kullanilir (ornek: "기생충" -> "Parasite"); Latin karsilik
     * bulunamazsa dosya adindan gelen temiz ad korunur.
     *
     * @param localizedTitle    TMDB'den gelen yerellestirilmis baslik (title / name) -- son care fallback
     * @param originalTitle     TMDB'den gelen orijinal baslik (original_title / original_name), bos olabilir
     * @param fallbackCleanName dosya adindan ayiklanan temiz ad, bos olabilir
     */
    public static String chooseFolderBaseName(String localizedTitle, String originalTitle, String fallbackCleanName) {
        boolean haveClean = fallbackCleanName != null && !fallbackCleanName.isBlank();
        String latinOriginal = latinTitle(originalTitle, localizedTitle);
        String sanitizedOriginal = sanitizePathSegment(latinOriginal);

        if (!sanitizedOriginal.isEmpty()) {
            boolean differsFromFilename = !haveClean
                || !TmdbLink.slugify(latinOriginal).equals(TmdbLink.slugify(fallbackCleanName));
            if (differsFromFilename) {
                return sanitizedOriginal;
            }
        }
        if (haveClean) {
            return fallbackCleanName;
        }
        String sanitizedLocalized = sanitizePathSegment(localizedTitle == null ? "" : localizedTitle);
        return sanitizedLocalized.isEmpty() ? "unknown" : sanitizedLocalized;
    }
}
