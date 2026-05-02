package com.patres.alina.server.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

/**
 * Service that fetches recently modified markdown notes from a directory.
 * <p>
 * Strategy:
 * <ol>
 *   <li>Walk the given root directory and find {@code .md} files sorted by last-modified time (newest first).</li>
 *   <li>Return the top N results as root-relative paths.</li>
 * </ol>
 * Optionally integrates with {@code obsidian-cli} for opening notes in the Obsidian app.
 */
public final class ObsidianCliService {

    private static final Logger logger = LoggerFactory.getLogger(ObsidianCliService.class);
    private static final String DEFAULT_CLI_PATH = "obsidian-cli";

    private ObsidianCliService() {
    }

    /**
     * Fetches the most recently modified notes from the given directory.
     *
     * @param notesDirectory   root directory to scan for {@code .md} files
     * @param limit            maximum number of notes to return
     * @param excludePatterns  comma-separated list of glob patterns to exclude (matched against root-relative path)
     * @return result containing notes or error information
     */
    public static ObsidianNotesResult fetchRecentNotes(final Path notesDirectory, final int limit,
                                                       final String excludePatterns) {
        final List<Pattern> compiled = compileExcludePatterns(excludePatterns);
        try {
            if (notesDirectory == null || !Files.isDirectory(notesDirectory)) {
                return ObsidianNotesResult.error("Notes directory does not exist: " + notesDirectory);
            }

            final List<ObsidianNote> notes = findRecentlyModifiedNotes(notesDirectory, limit, compiled);
            return ObsidianNotesResult.success(notes);
        } catch (final Exception e) {
            logger.warn("Notes: failed to fetch recent notes from {}", notesDirectory, e);
            return ObsidianNotesResult.error(e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    /**
     * Opens a note in Obsidian (requires obsidian-cli).
     *
     * @param cliPath   path to the obsidian-cli executable
     * @param notePath  the vault-relative path to the note
     */
    public static void openNote(final String cliPath, final String notePath) {
        final String executable = resolveCliPath(cliPath);
        try {
            final List<String> command = buildOpenCommand(executable, notePath);
            final ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.start();
            logger.info("Obsidian: opened note '{}'", notePath);
        } catch (final Exception e) {
            logger.warn("Obsidian: failed to open note '{}'", notePath, e);
        }
    }

    /**
     * Counts the total number of {@code .md} files in the given directory (excluding hidden and excluded paths).
     *
     * @param notesDirectory   root directory to scan
     * @param excludePatterns  comma-separated list of glob patterns to exclude
     * @return the number of markdown files, or 0 if the directory is unreachable
     */
    public static long countNotes(final Path notesDirectory, final String excludePatterns) {
        final List<Pattern> compiled = compileExcludePatterns(excludePatterns);
        try {
            if (notesDirectory == null || !Files.isDirectory(notesDirectory)) {
                return 0;
            }
            return countMarkdownFiles(notesDirectory, compiled);
        } catch (final Exception e) {
            logger.warn("Notes: failed to count notes in {}", notesDirectory, e);
            return 0;
        }
    }

    private static long countMarkdownFiles(final Path vaultPath, final List<Pattern> excludePatterns) throws IOException {
        try (final Stream<Path> stream = Files.walk(vaultPath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
//                    .filter(p -> !isHiddenPath(vaultPath, p))
                    .filter(p -> !isExcluded(vaultPath, p, excludePatterns))
                    .count();
        }
    }

    // ── Filesystem scan ──────────────────────────────────────────

    private static List<ObsidianNote> findRecentlyModifiedNotes(final Path vaultPath, final int limit,
                                                                   final List<Pattern> excludePatterns) throws IOException {
        try (final Stream<Path> stream = Files.walk(vaultPath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> !isHiddenPath(vaultPath, p))
                    .filter(p -> !isExcluded(vaultPath, p, excludePatterns))
                    .sorted(Comparator.comparing(ObsidianCliService::getLastModified).reversed())
                    .limit(limit)
                    .map(p -> toObsidianNote(vaultPath, p))
                    .toList();
        }
    }

    /**
     * Checks if the file is inside a hidden directory (starting with "."),
     * e.g. ".obsidian", ".trash", ".git".
     */
    private static boolean isHiddenPath(final Path vaultRoot, final Path file) {
        final Path relative = vaultRoot.relativize(file);
        for (final Path segment : relative) {
            if (segment.toString().startsWith(".")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the vault-relative path matches any of the exclude patterns.
     * Patterns are matched against the vault-relative path using forward slashes.
     */
    private static boolean isExcluded(final Path vaultRoot, final Path file, final List<Pattern> excludePatterns) {
        if (excludePatterns.isEmpty()) {
            return false;
        }
        final String relativePath = vaultRoot.relativize(file).toString().replace('\\', '/');
        for (final Pattern pattern : excludePatterns) {
            if (pattern.matcher(relativePath).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses a comma-separated list of exclude patterns into compiled regex patterns.
     * <p>
     * Each entry is trimmed and converted to a regex:
     * <ul>
     *   <li>{@code *} is converted to {@code [^/]*} (any chars except path separator)</li>
     *   <li>{@code **} is converted to {@code .*} (any chars including path separators)</li>
     *   <li>All other characters are regex-quoted for literal matching</li>
     * </ul>
     * Examples:
     * <ul>
     *   <li>{@code Agent.md} — excludes a file named exactly {@code Agent.md} at the vault root</li>
     *   <li>{@code Memory/*} — excludes all files directly inside the {@code Memory} folder</li>
     *   <li>{@code Memory/**} — excludes all files recursively inside {@code Memory}</li>
     *   <li>{@code *.excalidraw.md} — excludes all {@code .excalidraw.md} files at any single level</li>
     *   <li>{@code **&#47;*.excalidraw.md} — excludes all {@code .excalidraw.md} files at any depth</li>
     * </ul>
     */
    static List<Pattern> compileExcludePatterns(final String excludePatterns) {
        if (excludePatterns == null || excludePatterns.isBlank()) {
            return List.of();
        }
        final List<Pattern> compiled = new ArrayList<>();
        for (final String raw : excludePatterns.split(",")) {
            final String entry = raw.trim();
            if (entry.isEmpty()) {
                continue;
            }
            try {
                final String regex = globToRegex(entry);
                compiled.add(Pattern.compile(regex));
            } catch (final PatternSyntaxException e) {
                logger.warn("Obsidian: invalid exclude pattern '{}': {}", entry, e.getMessage());
            }
        }
        return compiled;
    }

    /**
     * Converts a simple glob pattern to a Java regex.
     * Handles {@code **}, {@code *}, and {@code ?} wildcards;
     * all other characters are quoted for literal matching.
     */
    private static String globToRegex(final String glob) {
        final StringBuilder regex = new StringBuilder();
        int i = 0;
        while (i < glob.length()) {
            final char c = glob.charAt(i);
            if (c == '*') {
                if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                    // ** matches any path depth
                    regex.append(".*");
                    i += 2;
                    // skip trailing slash after ** (e.g. **/)
                    if (i < glob.length() && glob.charAt(i) == '/') {
                        i++;
                    }
                } else {
                    // * matches anything except /
                    regex.append("[^/]*");
                    i++;
                }
            } else if (c == '?') {
                regex.append("[^/]");
                i++;
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
                i++;
            }
        }
        return regex.toString();
    }

    private static Instant getLastModified(final Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (final IOException e) {
            return Instant.EPOCH;
        }
    }

    private static ObsidianNote toObsidianNote(final Path vaultRoot, final Path absolutePath) {
        final String relativePath = vaultRoot.relativize(absolutePath).toString();
        final String name = extractNoteName(relativePath);
        final String folder = extractFolder(relativePath);
        final Instant lastModified = getLastModified(absolutePath);
        return new ObsidianNote(relativePath, name, folder, lastModified);
    }

    // ── Command building ─────────────────────────────────────────

    private static List<String> buildOpenCommand(final String executable, final String notePath) {
        final List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("open");
        command.add("path=" + notePath);
        return command;
    }

    // ── Path helpers ─────────────────────────────────────────────

    /**
     * Extracts the display name from a vault-relative path.
     * E.g. "Notes/My Note.md" -> "My Note"
     */
    static String extractNoteName(final String path) {
        final String fileName = Path.of(path).getFileName().toString();
        if (fileName.endsWith(".md")) {
            return fileName.substring(0, fileName.length() - 3);
        }
        return fileName;
    }

    /**
     * Extracts the folder portion from a vault-relative path.
     * E.g. "Notes/Sub/My Note.md" -> "Notes/Sub"
     */
    static String extractFolder(final String path) {
        final int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            // Also handle OS-specific separator
            final int lastBackslash = path.lastIndexOf('\\');
            if (lastBackslash <= 0) {
                return "";
            }
            return path.substring(0, lastBackslash).replace('\\', '/');
        }
        return path.substring(0, lastSlash);
    }

    // ── Process execution ────────────────────────────────────────

    private static String resolveCliPath(final String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return DEFAULT_CLI_PATH;
        }
        return configuredPath.trim();
    }
}
