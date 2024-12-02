package org.example.gbhujanfx1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    public static Properties loadConfig() throws IOException {
        String homeDir = System.getProperty("user.home");
        File configFile = new File(homeDir + "/config/db.conf");

        if (!configFile.exists()) {
            throw new FileNotFoundException("Configuration file not found: " + configFile.getAbsolutePath());
        }

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
        }

        return properties;
    }
}
