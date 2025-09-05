package com.patres.alina.common.settings;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.patres.alina.common.event.Event;
import com.patres.alina.common.event.bus.DefaultEventBus;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.Supplier;

public class FileManager<T> {

    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);
    private static final ObjectMapper mapper = createMapper();

    private final String path;
    private final String localPath;
    private final String name;
    private final Class<T> classToMap;
    private final Supplier<T> defaultValueSupplier;
    private final Supplier<Event> saveEventSupplier;

    private Optional<T> currentSettings = Optional.empty();

    public FileManager(String name, Class<T> classToMap, Supplier<T> defaultValueSupplier) {
        this.path = "data/config/" + name + ".json";
        this.localPath = "data/config/" + name + ".local.json";
        this.name = name;
        this.classToMap = classToMap;
        this.defaultValueSupplier = defaultValueSupplier;
        this.saveEventSupplier = null;
    }

    public FileManager(String name, Class<T> classToMap, Supplier<T> defaultValueSupplier, Supplier<Event> saveEventSupplier) {
        this.path = "data/config/" + name + ".json";
        this.localPath = "data/config/" + name + ".local.json";
        this.name = name;
        this.classToMap = classToMap;
        this.defaultValueSupplier = defaultValueSupplier;
        this.saveEventSupplier = saveEventSupplier;
    }

    public synchronized T getSettings() {
        if (currentSettings.isEmpty()) {
            currentSettings = Optional.of(loadSettings());
        }
        return currentSettings.get();
    }

    public T loadSettings() {
        logger.info("{} file are loading...", name);
        try {
            File file = getFile();
            if (file.exists()) {
                String serializedGlobalSettings = FileUtils.readFileToString(file, Charset.defaultCharset());
                T settings = mapper.readValue(serializedGlobalSettings, classToMap);
                logger.info("{} settings are loaded", name);
                return settings;
            }
            logger.info("{} settings not found in {} - creating new", name, file.getAbsoluteFile());
            return defaultValueSupplier.get();
        } catch (Exception e) {
            logger.error("Exception during load {} settings - creating new", name, e);
            return defaultValueSupplier.get();
        }
    }

    private File getFile() {
        File localFile = new File(localPath);
        if (localFile.exists()) {
            return localFile;
        }
        return new File(path);
    }

    public void saveDocument(T settings) {
        try {
            logger.info("{} settings are saving...", name);

            String serializedRootGroup = mapper.writeValueAsString(settings);
            File file = getFile();
            FileUtils.createParentDirectories(file);
            FileUtils.write(file, serializedRootGroup, Charset.defaultCharset());
            logger.info("{} settings are saved to {}", name, file.getAbsoluteFile());
            currentSettings = Optional.of(settings);
            if (saveEventSupplier != null) {
                DefaultEventBus.getInstance().publish(saveEventSupplier.get());
            }
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
