package org.example.OOD.Configurations;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    private static Properties properties;
    private static final String CONFIG_FILE = ".env"; // Path to your config file

    // Private constructor to prevent instantiation
    private Config() {}

    // Static method to load and retrieve properties
    public static Properties getProperties() {
        if (properties == null) {
            loadProperties();
        }
        return properties;
    }

    // Load properties from the config file
    private static void loadProperties() {
        properties = new Properties();
        try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

