package com.patres.alina.server.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

/**
 * Service that fetches recently modified notes from an Obsidian vault.
 * <p>
 * Strategy:
 * <ol>
 *   <li>Resolve the vault root path using {@code obsidian-cli vault info=path}.</li>
 *   <li>Walk the vault directory and find {@code .md} files sorted by last-modified time (newest first).</li>
 *   <li>Return the top N results as vault-relative paths.</li>
 * </ol>
 * This approach returns genuinely <em>recently modified</em> notes rather than just recently opened ones.
 */
public final class ObsidianCliService {

    private static final Logger logger = LoggerFactory.getLogger(ObsidianCliService.class);
    private static final long COMMAND_TIMEOUT_SECONDS = 10;
    private static final String DEFAULT_CLI_PATH = "obsidian-cli";

    private ObsidianCliService() {
    }

    /**
     * Fetches the most recently modified notes from the vault.
     *
     * @param cliPath          path to the obsidian-cli executable (may be empty for default)
     * @param vaultName        optional vault name (empty means default vault)
     * @param limit            maximum number of notes to return
     * @param excludePatterns  comma-separated list of regex patterns to exclude (matched against vault-relative path)
     * @return result containing notes or error information
     */
    public static ObsidianNotesResult fetchRecentNotes(final String cliPath, final String vaultName,
                                                       final int limit, final String excludePatterns) {
        final String executable = resolveCliPath(cliPath);
        final List<Pattern> compiled = compileExcludePatterns(excludePatterns);
        try {
            // Step 1: resolve vault path via CLI
            final Path vaultPath = resolveVaultPath(executable, vaultName);
            if (vaultPath == null) {
                return ObsidianNotesResult.error("Could not determine Obsidian vault path");
            }

            // Step 2: scan filesystem for recently modified .md files
            final List<ObsidianNote> notes = findRecentlyModifiedNotes(vaultPath, limit, compiled);
            return ObsidianNotesResult.success(notes);
        } catch (final IOException e) {
            if (isCommandNotFound(e)) {
                logger.warn("Obsidian: CLI not found at '{}'", executable);
                return ObsidianNotesResult.cliNotFound();
            }
            logger.warn("Obsidian: failed to fetch recent notes", e);
            return ObsidianNotesResult.error(e.getMessage() != null ? e.getMessage() : "Unknown error");
        } catch (final Exception e) {
            logger.warn("Obsidian: failed to fetch recent notes", e);
            return ObsidianNotesResult.error(e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    /**
     * Opens a note in Obsidian.
     *
     * @param cliPath   path to the obsidian-cli executable
     * @param vaultName optional vault name
     * @param notePath  the vault-relative path to the note
     */
    public static void openNote(final String cliPath, final String vaultName, final String notePath) {
        final String executable = resolveCliPath(cliPath);
        try {
            final List<String> command = buildOpenCommand(executable, vaultName, notePath);
            final ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.start();
            logger.info("Obsidian: opened note '{}'", notePath);
        } catch (final Exception e) {
            logger.warn("Obsidian: failed to open note '{}'", notePath, e);
        }
    }

    // ── Vault path resolution ────────────────────────────────────

    private static Path resolveVaultPath(final String executable, final String vaultName) throws Exception {
        final List<String> command = buildVaultPathCommand(executable, vaultName);
        final CommandExecutionResult result = runCommand(command);

        if (!result.finished()) {
            logger.warn("Obsidian: vault info command timed out");
            return null;
        }
        if (result.exitCode() != 0) {
            logger.warn("Obsidian: vault info command failed with exit code {}: {}", result.exitCode(), result.output());
            return null;
        }

        final String output = result.output().trim();
        if (output.isEmpty()) {
            return null;
        }

        final Path path = Path.of(output);
        if (!Files.isDirectory(path)) {
            logger.warn("Obsidian: vault path does not exist: {}", path);
            return null;
        }
        return path;
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

    private static List<String> buildVaultPathCommand(final String executable, final String vaultName) {
        final List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("vault");
        command.add("info=path");
        if (vaultName != null && !vaultName.isBlank()) {
            command.add("vault=" + vaultName);
        }
        return command;
    }

    private static List<String> buildOpenCommand(final String executable, final String vaultName, final String notePath) {
        final List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("open");
        command.add("path=" + notePath);
        if (vaultName != null && !vaultName.isBlank()) {
            command.add("vault=" + vaultName);
        }
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

    private static CommandExecutionResult runCommand(final List<String> command) throws Exception {
        final ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        final Process process = pb.start();
        final String output = readProcessOutput(process);
        final boolean finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new CommandExecutionResult(-1, output, false);
        }
        return new CommandExecutionResult(process.exitValue(), output, true);
    }

    private static String readProcessOutput(final Process process) throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            final StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
            return output.toString().trim();
        }
    }

    private static String resolveCliPath(final String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return DEFAULT_CLI_PATH;
        }
        return configuredPath.trim();
    }

    private static boolean isCommandNotFound(final IOException e) {
        final String message = e.getMessage();
        return message != null && (message.contains("No such file") || message.contains("not found")
                || message.contains("Cannot run program"));
    }

    private record CommandExecutionResult(int exitCode, String output, boolean finished) {
    }
}
