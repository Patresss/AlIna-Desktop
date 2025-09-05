package com.patres.alina.uidesktop.common.settings;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GlobalSettingsLoader {

    private static final String path = "data/config/Settings.json";
    private static final Logger logger = LoggerFactory.getLogger(GlobalSettingsLoader.class);
    private static final ObjectMapper mapper = createMapper();

    private static GlobalSettings currentGlobalSettings = null;

    public static GlobalSettings getSettings() {
        if (currentGlobalSettings == null) {
            currentGlobalSettings = load();
        }
        return currentGlobalSettings;
    }


    public static GlobalSettings load() {
        logger.info("Global settings are loading...");
        try {
            File file = new File(path);
            if (file.exists()) {
                String serializedGlobalSettings = FileUtils.readFileToString(file, Charset.defaultCharset());
                GlobalSettings settings = mapper.readValue(serializedGlobalSettings, GlobalSettings.class);
                logger.info("Global settings are loaded");
                return settings;
            }
            logger.info("Global settings not found - creating new");
            return new GlobalSettings();
        } catch (Exception e) {
            logger.error("Exception during load Global settings - creating new", e);
            return new GlobalSettings();
        }
    }

    public static void save(GlobalSettings globalSettings) {
        try {
            logger.info("Global settings are saving...");

            String serializedRootGroup = mapper.writeValueAsString(globalSettings);
            File file = new File(path);
            FileUtils.createParentDirectories(file);
            FileUtils.write(file, serializedRootGroup, Charset.defaultCharset());
            currentGlobalSettings = globalSettings;
            logger.info("Global settings are saved");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

}