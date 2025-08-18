package com.patres.alina.uidesktop.messagecontext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageWithContextGenerator {

    private static final Logger LOGGER = Logger.getLogger(MessageWithContextGenerator.class.getName());
    private final String message;

    public MessageWithContextGenerator(String message) {
        this.message = message;
    }

    public String replacePathsWithContents() {
        try {
            String pathPattern = "\\$<(.+?)>";
            Pattern pattern = Pattern.compile(pathPattern);
            Matcher matcher = pattern.matcher(message);

            StringBuilder result = new StringBuilder();

            while (matcher.find()) {
                String pathList = matcher.group(1);
                String[] paths = pathList.split(",");
                StringBuilder replacement = new StringBuilder();

                for (String pathString : paths) {
                    pathString = pathString.trim().replace("\"", "");
                    if (!isValidPath(pathString)) {
                        LOGGER.warning("Invalid path provided: " + pathString);
                        continue;
                    }

                    Path path = Paths.get(pathString);
                    if (Files.isDirectory(path)) {
                        Files.walk(path).filter(Files::isRegularFile)
                                .forEach(file -> replacement.append(getFileContentString(file)));
                    } else if (Files.isRegularFile(path)) {
                        replacement.append(getFileContentString(path));
                    }
                }
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.toString()));
            }
            matcher.appendTail(result);
            return result.toString();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException occurred", e);
            throw new MessageContextException(e);
        }
    }

    private static String getFileContentString(Path file) {
        try {
            StringBuilder fileContent = new StringBuilder();
            fileContent.append("\nFile: ").append(file.toString()).append("\nContent:\n```\n");

            List<String> lines = Files.readAllLines(file);
            for (String line : lines) {
                fileContent.append(line).append("\n");
            }

            fileContent.append("```\n");
            return fileContent.toString();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException occurred while reading the file: " + file, e);
            throw new MessageContextException(file, e);
        }
    }

    private static boolean isValidPath(String pathString) {
        try {
            Paths.get(pathString);
            return true;
        } catch (Exception e) {
            LOGGER.warning("Invalid path detected: " + pathString);
            return false;
        }
    }
}