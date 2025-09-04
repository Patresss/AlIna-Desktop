/* SPDX-License-Identifier: MIT */

package com.patres.alina.uidesktop.ui.theme;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Dracula;
import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.NordLight;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Theme;
import com.patres.alina.uidesktop.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class ThemeRepository {

    private static final Comparator<SamplerTheme> THEME_COMPARATOR = Comparator.comparing(SamplerTheme::getName);
    private static final Logger logger = LoggerFactory.getLogger(ThemeRepository.class);

    private static final String DATA_DIR = "data";
    private static final String CSS_DIR_NAME = "css";
    private static final String CSS_EXTENSION = ".css";
    private static final String DARK_KEYWORD = "dark";
    private static final String THEME_SUFFIX_REGEX = "(?i)-theme$";


    private final List<SamplerTheme> internalThemes = Arrays.asList(
        new SamplerTheme(new PrimerLight()),
        new SamplerTheme(new PrimerDark()),
        new SamplerTheme(new NordLight()),
        new SamplerTheme(new NordDark()),
        new SamplerTheme(new CupertinoLight()),
        new SamplerTheme(new CupertinoDark()),
        new SamplerTheme(new Dracula())
    );

    private final List<SamplerTheme> externalThemes = new ArrayList<>();
    private final Preferences themePreferences = Resources.getPreferences().node("theme");

    public ThemeRepository() {
        try {
            loadPreferences();
        } catch (BackingStoreException e) {
            logger.warn("Unable to load themes from the preferences.", e);
        }

        try {
            loadExternalFromDataCss();
        } catch (IOException e) {
            logger.warn("Unable to scan data/css for themes.", e);
        }
    }

    public List<SamplerTheme> getAll() {
        final var list = new ArrayList<>(internalThemes);
        list.addAll(externalThemes);
        return list;
    }

    public boolean isFileValid(Path path) {
        Objects.requireNonNull(path);
        final String fileName = path.getFileName() != null ? path.getFileName().toString() : "";
        return !Files.isDirectory(path, NOFOLLOW_LINKS)
            && Files.isRegularFile(path, NOFOLLOW_LINKS)
            && Files.isReadable(path)
            && fileName.toLowerCase().endsWith(CSS_EXTENSION);
    }

    private void loadPreferences() throws BackingStoreException {
        final var existingNames = collectExistingThemeNames();
        final List<String> keys = List.of(themePreferences.keys());
        for (final String themeName : keys) {
            addThemeFromPreferencesEntry(themeName, existingNames);
        }
        sortExternalThemes();
    }

    private void loadExternalFromDataCss() throws IOException {
        final Path cssDir = getCssDirectoryPath();
        ensureDirectoryExists(cssDir);
        if (!isDirectoryReadable(cssDir)) {
            return;
        }

        final var existingNames = collectExistingThemeNames();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cssDir, "*" + CSS_EXTENSION)) {
            for (final Path cssFile : stream) {
                processCssFile(cssFile, existingNames);
            }
        }

        sortExternalThemes();
    }

    private java.util.Set<String> collectExistingThemeNames() {
        final var names = new java.util.HashSet<String>();
        for (final var t : getAll()) {
            names.add(t.getName().toLowerCase());
        }
        return names;
    }

    private void addThemeFromPreferencesEntry(final String themeName, final java.util.Set<String> existingNames) {
        final String uaStylesheet = themePreferences.get(themeName, "");
        final Path uaStylesheetPath = Paths.get(uaStylesheet);

        if (!isFileValid(uaStylesheetPath)) {
            logger.warn("CSS file invalid or missing: '{}'. Removing from preferences.", uaStylesheetPath);
            themePreferences.remove(themeName);
            return;
        }

        if (existingNames.contains(themeName.toLowerCase())) {
            return;
        }

        final String fileName = uaStylesheetPath.getFileName() != null ? uaStylesheetPath.getFileName().toString() : "";
        final boolean isDark = containsIgnoreCase(fileName, DARK_KEYWORD);
        externalThemes.add(new SamplerTheme(Theme.of(themeName, uaStylesheet, isDark)));
        existingNames.add(themeName.toLowerCase());
    }

    private Path getCssDirectoryPath() {
        return Paths.get(DATA_DIR, CSS_DIR_NAME).toAbsolutePath();
    }

    private void ensureDirectoryExists(final Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            logger.debug("Unable to create directory: {}", dir, e);
        }
    }

    private boolean isDirectoryReadable(final Path dir) {
        return Files.isDirectory(dir, NOFOLLOW_LINKS) && Files.isReadable(dir);
    }

    private void processCssFile(final Path cssFile, final java.util.Set<String> existingNames) {
        if (!isFileValid(cssFile)) {
            return;
        }

        final String filename = cssFile.getFileName().toString();
        final String base = filename.substring(0, Math.max(0, filename.length() - CSS_EXTENSION.length()));
        final boolean isDark = containsIgnoreCase(base, DARK_KEYWORD);

        final String name = humanizeThemeName(base);
        if (name.isBlank() || existingNames.contains(name.toLowerCase())) {
            return;
        }

        externalThemes.add(new SamplerTheme(Theme.of(name, cssFile.toString(), isDark)));
        existingNames.add(name.toLowerCase());
    }

    private String humanizeThemeName(final String base) {
        final String humanized = base.replaceAll(THEME_SUFFIX_REGEX, "").replace('-', ' ').trim();
        final String[] parts = humanized.split("\\s+");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            final String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            if (i < parts.length - 1) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private boolean containsIgnoreCase(final String text, final String needle) {
        return text != null && needle != null && text.toLowerCase().contains(needle.toLowerCase());
    }

    private void sortExternalThemes() {
        externalThemes.sort(THEME_COMPARATOR);
    }
}
