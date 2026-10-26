# Movie Organizer

A Java command-line tool for Windows that organizes a flat directory of `.mkv` movie and TV episode files into a tidy folder structure. 
It uses [The Movie Database (TMDB)](https://www.themoviedb.org/) to look up the correct title, year, season count, rating and poster for each item.

## What it does

Given a directory full of release-style file names such as:

```
Emancipation.2022.1080p.WebRip.x264.DUAL.Filmbol.mkv
DTF.St.Louis.S01E07.WEB-DL.1080p.DUAL.H.265-HDM.mkv
```

the tool:

1. Parses every `.mkv` file in the directory (non-recursive) and classifies it as a **movie** or a **TV episode**.
2. Searches TMDB for each movie and each TV show (episodes of the same show are grouped and looked up once).
3. Creates a folder per movie / show, downloads `poster.jpg` into it, and writes a `.tmdb` link file pointing to the TMDB page.
4. **Moves** the media files into their new folders (TV episodes go into `sNN` season subfolders).

Resulting layout:

```
<source dir>/
├── Emancipation.2022.1080p.WebRip.x264.DUAL.Filmbol(SC#071)/
│   ├── Emancipation.2022.1080p.WebRip.x264.DUAL.Filmbol.mkv
│   ├── poster.jpg
│   └── movie_<id>-emancipation.tmdb
├── ##DTF St Louis(2026)(1)(SC#068)/
│   ├── poster.jpg
│   ├── tv_<id>-dtf-st-louis.tmdb
│   └── s01/
│       └── DTF.St.Louis.S01E07.WEB-DL.1080p.DUAL.H.265-HDM.mkv
├── #### MULTIPLE ####/      (ambiguous TMDB matches, see below)
└── #### NOK ####/           (files with no TMDB match)
```

## Requirements

- Java 11 or newer (uses `java.net.http.HttpClient` and `Files.writeString`)
- Maven 3.x (to build)
- A TMDB API key (v3) — see <https://developer.themoviedb.org/docs/getting-started>
- Internet access to `api.themoviedb.org` and `image.tmdb.org`

## Build

```bash
mvn clean package
```

The Maven Shade plugin produces a self-contained executable jar (Gson included):

```
target/movie-organizer.jar
```

Run the unit tests with:

```bash
mvn test
```

## Usage

```bash
java -jar target/movie-organizer.jar <directory> <tmdb-api-key>
```

| Argument         | Description                                       |
|------------------|---------------------------------------------------|
| `<directory>`    | Directory containing the `.mkv` files to organize |
| `<tmdb-api-key>` | Your TMDB v3 API key                              |

On Windows the included batch file can be used:

```bat
run.bat "R:\Downloads" YOUR_TMDB_API_KEY
```

> **Note:** Files are **moved**, not copied. Try it on a test directory first.

## Supported file name patterns

Only files ending in `.mkv` are processed. Names are matched case-insensitively, in this order:

| Pattern                         | Type    | Example                                                  |
|---------------------------------|---------|----------------------------------------------------------|
| `Name.yyyy.Sxx.Eyy.release.mkv` | TV      | `Show.Name.2024.S01.E03.1080p.WEB-DL.mkv`                |
| `Name.yyyy.SxxEyy.release.mkv`  | TV      | `Show.Name.2024.S01E03.1080p.WEB-DL.mkv`                 |
| `Name.SxxEyy.release.mkv`       | TV      | `DMV.S01E13.WEB-DL.1080p.DUAL.H.264-HDM.mkv`             |
| `Name.yyyy.SxxByy.release.mkv`  | TV      | `Show.Name.2024.S02B05.1080p.mkv` (`B` = *Bölüm*)        |
| `Name.SxxByy.release.mkv`       | TV      | `Show.Name.S02B05.1080p.mkv`                             |
| `Name.yyyy.quality.mkv`         | Movie   | `Eagles.of.the.Republic.2026.1080p.WebRip.x264.DUAL.Filmbol.mkv` |

Dots in the name part are converted to spaces (`DTF.St.Louis` → `DTF St Louis`). Files that match none of the patterns are logged as warnings and left untouched.

## Folder naming rules

### Movies

```
<Title>.<TMDB year>.<quality info>(SC#<score>)
```

- `quality info` is everything after the year in the original file name.
- The year comes from the TMDB release date (the file's year is only used as a search filter).

### TV shows

```
##<Title>(<first air year>)(<season count>)(SC#<score>)
```

- Year and season count come from TMDB. If TMDB has no air date, `9999` is used.
- Season subfolders `s01`, `s02`, … are created. If the files contain a season TMDB doesn't list yet, the subfolder is still created (the folder name keeps TMDB's season count).
- If a folder for the same show/year already exists with an older season count or score, it is **renamed** to the new name rather than duplicated.

### Score

`SC#nnn` is TMDB's `vote_average` (0–10) multiplied by 10 and zero-padded to 3 digits, e.g. `7.5` → `SC#075`.

### Title selection

- If the name in the file differs meaningfully from TMDB's **original title** (`original_title` / `original_name`), the original title is used. This handles translated/local file names:
  `Uzun.Otların.Arasında.2019.1080p...mkv` → `In the Tall Grass.2019.1080p...(SC#055)`
- If the names are essentially the same (only punctuation/spacing differs, compared via slug), the clean name from the file is kept (`DTF St Louis`, not `DTF: St. Louis`).
- If the original title is not in Latin script (e.g. `기생충`, `Брат`), TMDB's English (Latin) title is used instead (`Parasite`, `Brother`). If none is available, the clean name from the file is kept.
- Characters that are illegal in Windows paths (`\ / : * ? " < > |`) are replaced and trailing dots/spaces are removed.

## `.tmdb` link files

Every created media folder gets a small text file next to `poster.jpg`:

- **File name:** `<type>_<tmdbId>-<slug>.tmdb` (`type` = `movie` or `tv`; if the slug is empty, only the id is used)
- **Content:** the TMDB web page URL

| Media           | File name                         | Content                                              |
|-----------------|-----------------------------------|------------------------------------------------------|
| 1408 (movie)    | `movie_3021-1408.tmdb`            | `https://www.themoviedb.org/movie/3021-1408`         |
| Game of Thrones | `tv_1399-game-of-thrones.tmdb`    | `https://www.themoviedb.org/tv/1399-game-of-thrones` |

The slug is generated locally (lower case, accents stripped, non-alphanumerics collapsed to `-`) and may differ slightly from TMDB's own slug. TMDB resolves pages by numeric id, so the link always opens the correct page.

## Special folders

### `#### NOK ####` — no match

If TMDB returns no results (or the request fails), the movie file / all episodes of the show are moved to `#### NOK ####` for manual handling.

### `#### MULTIPLE ####` — ambiguous match

If TMDB returns **more than one** result whose title exactly matches the searched name (slug comparison against both localized and original title), the tool does not guess. For example, `Matlock` matches both `tv/2093-matlock` and `tv/226174-matlock`:

```
#### MULTIPLE ####/
└── Matlock/
    ├── ##Matlock(1986)(9)(SC#071)/   poster.jpg, .tmdb, s01..s09
    ├── ##Matlock(2024)(2)(SC#070)/   poster.jpg, .tmdb, s01..s02
    ├── Matlock.S01E01....mkv
    └── Matlock.S01E02....mkv
```

- A full folder structure (poster, `.tmdb` file, season folders for TV) is built for each candidate (max. 8).
- The media files are moved to `#### MULTIPLE ####/<query>/`, **not** into a candidate folder — pick the correct one manually.
- If there is only one exact match, or no exact match at all, processing continues normally with that match / the first search result.

## Behaviour notes

- Existing files are never overwritten: if a target file already exists, the move is skipped with a warning. An existing `poster.jpg` or `.tmdb` file is kept.
- A failure for one movie or show is logged and does not stop the rest of the run.
- Poster download failures are logged as warnings; the folder is still created and the file still moved.
- Console output is colored: warnings in yellow, errors in red. Log messages are in Turkish.

## Project structure

```
src/main/java/com/movieorganizer/
├── Main.java                     Entry point: argument checks, scans for .mkv files, starts organizing
├── model/
│   ├── MediaFile.java            Base class (original file name, clean name)
│   ├── MovieFile.java            Movie: year, quality info
│   └── TvShowFile.java           Episode: season, episode, release info
├── parser/
│   └── FileNameParser.java       Regex-based file name → MediaFile parsing
├── organizer/
│   └── FileOrganizer.java        Folder creation, poster/link files, moving, NOK/MULTIPLE handling
├── tmdb/
│   ├── TmdbClient.java           TMDB API v3 search/details calls and poster download
│   ├── TmdbLink.java             Slug, .tmdb file name and TMDB web URL generation
│   ├── TmdbMovieResult.java      Movie search result
│   └── TmdbTvResult.java         TV show details result
└── util/
    ├── MediaNaming.java          Folder name selection, Latin-title logic, path sanitizing
    └── ColorConsoleFormatter.java  ANSI-colored console log formatter

src/test/java/com/movieorganizer/
├── parser/FileNameParserTest.java
├── tmdb/TmdbLinkTest.java
└── util/MediaNamingTest.java
```

## TMDB endpoints used

| Endpoint                              | Purpose                                         |
|---------------------------------------|-------------------------------------------------|
| `GET /3/search/movie?query=&year=`    | Movie search (filtered by the year in the file) |
| `GET /3/search/tv?query=`             | TV show search                                  |
| `GET /3/tv/{id}`                      | Season count, first air date, rating, poster    |
| `https://image.tmdb.org/t/p/original` | Poster download                                 |

## Dependencies

- [Gson](https://github.com/google/gson) 2.10.1 — JSON parsing
- JUnit 4.13.2 — tests
