package com.patres.alina.server.logs;

import com.patres.alina.common.logs.LogsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/logs")
public class LogsController {

    private final static String LOG_FILE_PATH = "logs/application.log";

    @GetMapping
    public LogsResponse getLogs() {
        try {
            final File logFile = new File(LOG_FILE_PATH);
            final List<String> allLines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
            return new LogsResponse(allLines);
        } catch (Exception e) {
            throw new CannotReceiveLogsException(e);
        }
    }
}