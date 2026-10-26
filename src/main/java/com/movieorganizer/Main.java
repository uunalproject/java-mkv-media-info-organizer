package com.movieorganizer;

import com.movieorganizer.model.MediaFile;
import com.movieorganizer.organizer.FileOrganizer;
import com.movieorganizer.parser.FileNameParser;
import com.movieorganizer.tmdb.TmdbClient;
import com.movieorganizer.util.ColorConsoleFormatter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final String RED   = "[31m";
    private static final String RESET = "[0m";

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        setupLogging();

        if (args.length < 2) {
            System.err.println(RED + "Kullanim: java -jar movie-organizer.jar <dizin> <tmdb-api-key>" + RESET);
            System.exit(1);
        }

        Path dirPath = Paths.get(args[0]);
        File dir = dirPath.toFile();

        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println(RED + "Hata: Dizin bulunamadi: " + args[0] + RESET);
            System.exit(1);
        }

        File[] mkvFiles = dir.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".mkv"));
        if (mkvFiles == null || mkvFiles.length == 0) {
            System.out.println(".mkv dosyasi bulunamadi: " + args[0]);
            return;
        }

        List<MediaFile> mediaFiles = new ArrayList<>();
        for (File file : mkvFiles) {
            MediaFile parsed = FileNameParser.parse(file.getName());
            if (parsed != null) {
                mediaFiles.add(parsed);
            } else {
                logger.warning("Tanimlanamayan dosya adi: " + file.getName());
            }
        }

        System.out.println(mkvFiles.length + " .mkv dosyasi bulundu, " + mediaFiles.size() + " tanesi islenecek.");

        TmdbClient tmdbClient = new TmdbClient(args[1]);
        new FileOrganizer(dirPath, tmdbClient).organize(mediaFiles);

        System.out.println("Duzenleme tamamlandi.");
    }

    private static void setupLogging() {
        Logger rootLogger = Logger.getLogger("");
        for (Handler h : rootLogger.getHandlers()) {
            rootLogger.removeHandler(h);
        }
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO);
        handler.setFormatter(new ColorConsoleFormatter());
        rootLogger.addHandler(handler);
        rootLogger.setLevel(Level.INFO);
    }
}