package com.movieorganizer.organizer;

import com.movieorganizer.model.MediaFile;
import com.movieorganizer.model.MovieFile;
import com.movieorganizer.model.TvShowFile;
import com.movieorganizer.tmdb.TmdbClient;
import com.movieorganizer.tmdb.TmdbLink;
import com.movieorganizer.tmdb.TmdbMovieResult;
import com.movieorganizer.tmdb.TmdbTvResult;
import com.movieorganizer.util.MediaNaming;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FileOrganizer {

    private static final Logger logger = Logger.getLogger(FileOrganizer.class.getName());
    private static final String NOK_FOLDER = "#### NOK ####";
    private static final String MULTIPLE_FOLDER = "#### MULTIPLE ####";

    private final Path sourceDir;
    private final TmdbClient tmdbClient;
    private final Map<String, List<TmdbTvResult>> tvCache = new HashMap<>();

    public FileOrganizer(Path sourceDir, TmdbClient tmdbClient) {
        this.sourceDir = sourceDir;
        this.tmdbClient = tmdbClient;
    }

    public void organize(List<MediaFile> files) {
        List<MovieFile> movies = files.stream()
            .filter(f -> !f.isTvShow())
            .map(f -> (MovieFile) f)
            .collect(Collectors.toList());

        Map<String, List<TvShowFile>> tvGroups = files.stream()
            .filter(MediaFile::isTvShow)
            .map(f -> (TvShowFile) f)
            .collect(Collectors.groupingBy(TvShowFile::getCleanName));

        logger.info(movies.size() + " film, " + tvGroups.values().stream().mapToInt(List::size).sum() + " dizi bolumu bulundu.");

        movies.forEach(m -> {
            try { organizeMovie(m); }
            catch (Exception e) { logger.warning("[FILM HATA] " + m.getOriginalFilename() + ": " + e.getMessage()); }
        });

        tvGroups.forEach((showName, episodes) -> {
            try { organizeTvShow(showName, episodes); }
            catch (Exception e) { logger.warning("[DIZI HATA] " + showName + ": " + e.getMessage()); }
        });
    }

    // TMDB vote_average is 0-10; multiply by 10 → 0-100 integer score
    private String formatScore(Double voteAverage) {
        if (voteAverage == null || voteAverage < 0) {
            return "(SC$000)";
        }
        int score = Math.min((int) (voteAverage * 10), 100);
        return String.format("(SC#%03d)", score);
    }

    private void moveFile(Path source, Path target) throws Exception {
        try {
            Files.move(source, target);
        } catch (FileAlreadyExistsException e) {
            logger.warning("Dosya zaten mevcut, atlandi: " + target.getFileName());
        }
    }

    /**
     * poster.jpg ile ayni dizine TMDB sayfa adresini iceren "id.tmdb" dosyasi olusturur.
     * type: "movie" veya "tv". Ornek dosya: movie_3021-1408.tmdb -> icerik: https://www.themoviedb.org/movie/3021-1408
     */
    private void writeTmdbLinkFile(Path mediaFolder, String type, int id, String title) {
        try {
            Path linkFile = mediaFolder.resolve(TmdbLink.fileName(type, id, title));
            if (Files.exists(linkFile)) {
                return;
            }
            String url = TmdbLink.webUrl(type, id, title);
            Files.writeString(linkFile, url + System.lineSeparator(), StandardCharsets.UTF_8);
            logger.info("TMDB link dosyasi olusturuldu: " + linkFile.getFileName());
        } catch (Exception e) {
            logger.warning("TMDB link dosyasi olusturulamadi (" + mediaFolder.getFileName() + "): " + e.getMessage());
        }
    }

    private void downloadPosterIfNeeded(Path mediaFolder, String posterPath, String label) {
        if (posterPath == null || posterPath.isEmpty()) {
            return;
        }
        Path poster = mediaFolder.resolve("poster.jpg");
        if (Files.exists(poster)) {
            return;
        }
        try {
            tmdbClient.downloadPoster(posterPath, poster);
            logger.info("Afis indirildi: " + label);
        } catch (Exception e) {
            logger.warning("Afis indirilemedi (" + label + "): " + e.getMessage());
        }
    }

    /** .tmdb dosyasinin slug'i icin orijinal basligi (varsa) tercih et. */
    private static String linkTitle(String originalTitle, String localizedTitle) {
        // Latin olmayan orijinal baslik bos slug uretir; Latin karsiligi tercih edilir.
        String latin = MediaNaming.latinTitle(originalTitle, localizedTitle);
        if (!latin.isEmpty()) {
            return latin;
        }
        return (originalTitle != null && !originalTitle.isBlank()) ? originalTitle : localizedTitle;
    }

    // ------------------------------------------------------------------ FILM

    private void organizeMovie(MovieFile movie) throws Exception {
        logger.info("Film isleniyor: " + movie.getOriginalFilename());

        List<TmdbMovieResult> candidates;
        try {
            candidates = tmdbClient.searchMovies(movie.getCleanName(), movie.getYear());
        } catch (Exception e) {
            logger.warning("TMDB aramasi basarisiz (" + movie.getCleanName() + "): " + e.getMessage());
            candidates = List.of();
        }

        if (candidates.isEmpty()) {
            logger.warning("Bilgi bulunamadi, " + NOK_FOLDER + " dizinine tasiniyor: " + movie.getOriginalFilename());
            Path nokFolder = sourceDir.resolve(NOK_FOLDER);
            Files.createDirectories(nokFolder);
            moveFile(sourceDir.resolve(movie.getOriginalFilename()), nokFolder.resolve(movie.getOriginalFilename()));
            return;
        }

        if (candidates.size() == 1) {
            Path movieFolder = buildMovieFolder(sourceDir, movie, candidates.get(0));
            moveFile(sourceDir.resolve(movie.getOriginalFilename()), movieFolder.resolve(movie.getOriginalFilename()));
            logger.info("Tasindi: " + movie.getOriginalFilename() + " -> " + movieFolder.getFileName() + "/");
            return;
        }

        // Coklu sonuc: MULTIPLE/<sorgu>/ altina her aday icin dizin yapisi kurulur, medya dosyasi da buraya tasinir.
        Path multiDir = sourceDir.resolve(MULTIPLE_FOLDER)
            .resolve(MediaNaming.sanitizePathSegment(movie.getCleanName()));
        Files.createDirectories(multiDir);
        logger.warning("Coklu TMDB sonucu (" + candidates.size() + ") - " + movie.getCleanName()
            + " -> " + MULTIPLE_FOLDER + "/" + multiDir.getFileName() + "/");

        for (TmdbMovieResult c : candidates) {
            buildMovieFolder(multiDir, movie, c);
        }
        moveFile(sourceDir.resolve(movie.getOriginalFilename()), multiDir.resolve(movie.getOriginalFilename()));
        logger.info("Tasindi: " + movie.getOriginalFilename() + " -> " + MULTIPLE_FOLDER + "/" + multiDir.getFileName() + "/");
    }

    /**
     * Verilen ust dizin altinda film dizinini, poster.jpg ve .tmdb dosyasini olusturur.
     * Medya dosyasini tasimaz. Olusturulan dizin yolunu doner.
     */
    private Path buildMovieFolder(Path parentDir, MovieFile movie, TmdbMovieResult tmdb) throws Exception {
        String base = MediaNaming.chooseFolderBaseName(tmdb.getTitle(), tmdb.getOriginalTitle(), movie.getCleanName());
        // Format: "Movie Name.2022.1080p.WebRip.x264.DUAL.Filmbol(SC#075)"
        String folderName = base + "." + tmdb.getYear() + "." + movie.getQualityInfo() + formatScore(tmdb.getVoteAverage());
        Path movieFolder = parentDir.resolve(folderName);
        Files.createDirectories(movieFolder);

        writeTmdbLinkFile(movieFolder, "movie", tmdb.getId(), linkTitle(tmdb.getOriginalTitle(), tmdb.getTitle()));
        downloadPosterIfNeeded(movieFolder, tmdb.getPosterPath(), folderName);
        return movieFolder;
    }

    // ------------------------------------------------------------------ DIZI

    private void organizeTvShow(String showName, List<TvShowFile> episodes) throws Exception {
        logger.info("Dizi isleniyor: " + showName + " (" + episodes.size() + " bolum)");

        List<TmdbTvResult> candidates = tvCache.get(showName);
        if (candidates == null) {
            try {
                candidates = tmdbClient.searchTvShows(showName);
            } catch (Exception e) {
                logger.warning("TMDB aramasi basarisiz (" + showName + "): " + e.getMessage());
                candidates = List.of();
            }
            tvCache.put(showName, candidates);
        }

        if (candidates.isEmpty()) {
            logger.warning("Bilgi bulunamadi, " + NOK_FOLDER + " dizinine tasiniyor: " + showName);
            Path nokFolder = sourceDir.resolve(NOK_FOLDER);
            Files.createDirectories(nokFolder);
            for (TvShowFile ep : episodes) {
                moveFile(sourceDir.resolve(ep.getOriginalFilename()), nokFolder.resolve(ep.getOriginalFilename()));
                logger.info("NOK'a tasindi: " + ep.getOriginalFilename());
            }
            return;
        }

        if (candidates.size() == 1) {
            organizeSingleTvShow(showName, episodes, candidates.get(0));
            return;
        }

        // Coklu sonuc: MULTIPLE/<sorgu>/ altina her aday icin dizin yapisi kurulur, bolumler de buraya tasinir.
        Path multiDir = sourceDir.resolve(MULTIPLE_FOLDER).resolve(MediaNaming.sanitizePathSegment(showName));
        Files.createDirectories(multiDir);
        logger.warning("Coklu TMDB sonucu (" + candidates.size() + ") - " + showName
            + " -> " + MULTIPLE_FOLDER + "/" + multiDir.getFileName() + "/");

        for (TmdbTvResult tmdb : candidates) {
            buildTvShowFolder(multiDir, showName, tmdb);
        }
        for (TvShowFile ep : episodes) {
            moveFile(sourceDir.resolve(ep.getOriginalFilename()), multiDir.resolve(ep.getOriginalFilename()));
            logger.info("Tasindi: " + ep.getOriginalFilename() + " -> " + MULTIPLE_FOLDER + "/" + multiDir.getFileName() + "/");
        }
    }

    private void organizeSingleTvShow(String showName, List<TvShowFile> episodes, TmdbTvResult tmdb) throws Exception {
        String base = MediaNaming.chooseFolderBaseName(tmdb.getName(), tmdb.getOriginalName(), showName);
        int year = tmdb.getFirstAirYear();
        int totalSeasons = tmdb.getNumberOfSeasons();

        // TMDB bazen yeni sezonu geç listeler; dosyalardaki max sezonu da dikkate al
        int maxEpSeason = episodes.stream().mapToInt(TvShowFile::getSeason).max().orElse(totalSeasons);
        int effectiveTotalSeasons = Math.max(totalSeasons, maxEpSeason);

        String yearStr = year > 0 ? String.valueOf(year) : "9999";
        // Klasör adında TMDB'nin doğruladığı sezon sayısı kullanılır; alt dizinler için effectiveTotalSeasons.
        // Format: "##Show Name(2026)(3)(SC#082)"
        String folderName = "##" + base + "(" + yearStr + ")(" + totalSeasons + ")" + formatScore(tmdb.getVoteAverage());

        // Diskte eski sezon/skor bilgisiyle oluşturulmuş klasörü rename et
        Path showFolder = findAndUpdateExistingShowFolder(base, year, folderName);
        Files.createDirectories(showFolder);

        writeTmdbLinkFile(showFolder, "tv", tmdb.getId(), linkTitle(tmdb.getOriginalName(), tmdb.getName()));

        for (int i = 1; i <= effectiveTotalSeasons; i++) {
            Files.createDirectories(showFolder.resolve(String.format("s%02d", i)));
        }

        downloadPosterIfNeeded(showFolder, tmdb.getPosterPath(), folderName);

        for (TvShowFile ep : episodes) {
            Path seasonFolder = showFolder.resolve(String.format("s%02d", ep.getSeason()));
            Files.createDirectories(seasonFolder);
            moveFile(sourceDir.resolve(ep.getOriginalFilename()), seasonFolder.resolve(ep.getOriginalFilename()));
            logger.info("Tasindi: " + ep.getOriginalFilename() + " -> " + folderName + "/s" + String.format("%02d", ep.getSeason()) + "/");
        }
    }

    /**
     * MULTIPLE dizini altinda tek bir aday icin dizi dizinini, sezon alt dizinlerini,
     * poster.jpg ve .tmdb dosyasini olusturur. Bolum dosyalarini tasimaz.
     */
    private Path buildTvShowFolder(Path parentDir, String queryName, TmdbTvResult tmdb) throws Exception {
        String base = MediaNaming.chooseFolderBaseName(tmdb.getName(), tmdb.getOriginalName(), queryName);
        int year = tmdb.getFirstAirYear();
        int totalSeasons = tmdb.getNumberOfSeasons();
        String yearStr = year > 0 ? String.valueOf(year) : "9999";
        String folderName = "##" + base + "(" + yearStr + ")(" + totalSeasons + ")" + formatScore(tmdb.getVoteAverage());

        Path showFolder = parentDir.resolve(folderName);
        Files.createDirectories(showFolder);

        writeTmdbLinkFile(showFolder, "tv", tmdb.getId(), linkTitle(tmdb.getOriginalName(), tmdb.getName()));

        for (int i = 1; i <= Math.max(totalSeasons, 1); i++) {
            Files.createDirectories(showFolder.resolve(String.format("s%02d", i)));
        }

        downloadPosterIfNeeded(showFolder, tmdb.getPosterPath(), folderName);
        return showFolder;
    }

    /**
     * Diskte aynı dizi için eski sezon sayısıyla oluşturulmuş bir klasör varsa
     * yeni adla yeniden adlandırır ve hedef yolu döner.
     */
    private Path findAndUpdateExistingShowFolder(String folderBaseName, int year, String targetFolderName) throws Exception {
        String yearStr = year > 0 ? String.valueOf(year) : "9999";
        String searchPrefix = "##" + folderBaseName + "(" + yearStr + ")(";

        Path targetPath = sourceDir.resolve(targetFolderName);
        if (Files.exists(targetPath)) {
            return targetPath;
        }

        try (var dirStream = Files.list(sourceDir)) {
            Optional<Path> match = dirStream
                .filter(Files::isDirectory)
                .filter(p -> p.getFileName().toString().startsWith(searchPrefix))
                .findFirst();

            if (match.isPresent()) {
                Path existing = match.get();
                logger.info("Mevcut dizi dizini guncelleniyor: " + existing.getFileName() + " -> " + targetFolderName);
                Files.move(existing, targetPath);
                logger.info("Yeniden adlandirildi: " + existing.getFileName() + " -> " + targetFolderName);
            }
        }

        return targetPath;
    }
}
