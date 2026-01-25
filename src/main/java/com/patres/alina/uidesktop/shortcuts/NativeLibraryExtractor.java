package com.patres.alina.uidesktop.shortcuts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Extracts JNativeHook native libraries from JAR to a temp directory.
 * This is needed because Spring Boot's nested JAR classloader doesn't support
 * the standard URI extraction that JNativeHook uses.
 */
public final class NativeLibraryExtractor {

    private static final Logger logger = LoggerFactory.getLogger(NativeLibraryExtractor.class);
    private static boolean initialized = false;
    private static boolean libraryLoaded = false;

    private NativeLibraryExtractor() {
    }

    /**
     * Must be called before any JNativeHook classes are loaded.
     * Extracts and loads the native library directly via System.load().
     */
    public static synchronized void extractNativeLibraries() {
        if (initialized) {
            return;
        }
        initialized = true;

        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();

        String platform;
        String libExtension;
        String libPrefix = "";

        if (osName.contains("mac") || osName.contains("darwin")) {
            platform = "darwin";
            libExtension = ".dylib";
            libPrefix = "lib";
        } else if (osName.contains("win")) {
            platform = "windows";
            libExtension = ".dll";
        } else {
            platform = "linux";
            libExtension = ".so";
            libPrefix = "lib";
        }

        String arch;
        if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            arch = "arm64";  // JNativeHook uses "arm64" not "aarch64"
        } else if (osArch.contains("arm")) {
            arch = "arm";
        } else if (osArch.contains("64")) {
            arch = "x86_64";
        } else {
            arch = "x86";
        }

        String libName = libPrefix + "JNativeHook" + libExtension;
        String resourcePath = "/com/github/kwhat/jnativehook/lib/" + platform + "/" + arch + "/" + libName;

        try {
            Path tempDir = Files.createTempDirectory("jnativehook");
            tempDir.toFile().deleteOnExit();

            Path libFile = tempDir.resolve(libName);
            libFile.toFile().deleteOnExit();

            try (InputStream is = NativeLibraryExtractor.class.getResourceAsStream(resourcePath)) {
                if (is != null) {
                    Files.copy(is, libFile, StandardCopyOption.REPLACE_EXISTING);

                    // Set the lib path property for JNativeHook
                    System.setProperty("jnativehook.lib.path", tempDir.toAbsolutePath().toString());

                    // Load the library directly - this prevents JNativeHook from trying to extract it
                    System.load(libFile.toAbsolutePath().toString());
                    libraryLoaded = true;

                    logger.info("Extracted and loaded JNativeHook native library from: {}", libFile);
                } else {
                    logger.warn("Native library not found in classpath: {}", resourcePath);
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to extract JNativeHook native library: {}", e.getMessage());
        } catch (UnsatisfiedLinkError e) {
            logger.warn("Failed to load JNativeHook native library: {}", e.getMessage());
        }
    }

    public static boolean isLibraryLoaded() {
        return libraryLoaded;
    }
}
