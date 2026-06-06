package de.gnm.voxeldash.api.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;

public class PropertyHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final File PROPERTIES_FILE = new File("server.properties");

    /**
     * Gets a specific property from the properties file
     *
     * @return the properties as a json node
     */
    public static JsonNode getProperties() {
        try {
            Properties properties = new Properties();
            properties.load(new BufferedReader(new FileReader(PROPERTIES_FILE)));

            return MAPPER.valueToTree(properties);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets a specific property from the properties file
     *
     * @param key the key of the property
     * @return the value of the property
     */
    public static String getProperty(String key) {
        try {
            Properties properties = new Properties();
            properties.load(new BufferedReader(new FileReader(PROPERTIES_FILE)));
            return properties.getProperty(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Sets a specific property in the properties file. The file is created if it does not exist yet.
     *
     * @param key   the key of the property
     * @param value the value of the property
     */
    public static void setProperty(String key, String value) {
        try {
            Properties properties = load();
            properties.setProperty(key, value);
            store(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets multiple properties in a single read/write cycle. The file is created if it does not
     * exist yet.
     *
     * @param values the properties to set
     */
    public static void setProperties(Map<String, String> values) {
        try {
            Properties properties = load();
            for (Map.Entry<String, String> entry : values.entrySet()) {
                properties.setProperty(entry.getKey(), entry.getValue());
            }
            store(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Properties load() throws Exception {
        Properties properties = new Properties();
        if (PROPERTIES_FILE.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(PROPERTIES_FILE))) {
                properties.load(reader);
            }
        }
        return properties;
    }

    private static void store(Properties properties) throws Exception {
        try (OutputStream output = Files.newOutputStream(PROPERTIES_FILE.toPath())) {
            properties.store(output, null);
        }
    }

}
