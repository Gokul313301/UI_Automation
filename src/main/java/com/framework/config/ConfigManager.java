package com.framework.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads environment-specific configuration from classpath properties files.
 *
 * <p>The active environment is selected with the {@code env} system property
 * (defaults to {@code qa}), e.g. {@code -Denv=dev}. Individual values can be
 * overridden at runtime by passing them as system properties, e.g.
 * {@code -Dbrowser=firefox -Dheadless=false}.</p>
 */
public final class ConfigManager {

    private static final String DEFAULT_ENV = "qa";
    private static final Properties PROPERTIES = new Properties();

    static {
        String env = System.getProperty("env", DEFAULT_ENV).trim().toLowerCase();
        String resource = "config/" + env + ".properties";
        try (InputStream in = ConfigManager.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Configuration file not found on classpath: " + resource);
            }
            PROPERTIES.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load configuration: " + resource, e);
        }
    }

    private ConfigManager() {
    }

    /**
     * Returns the value for {@code key}. A matching system property takes
     * precedence over the value from the properties file, allowing per-run
     * overrides from the command line.
     */
    public static String get(String key) {
        String override = System.getProperty(key);
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        String value = PROPERTIES.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing configuration key: " + key);
        }
        return value.trim();
    }

    public static String get(String key, String defaultValue) {
        String override = System.getProperty(key);
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        return PROPERTIES.getProperty(key, defaultValue).trim();
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    public static int getInt(String key, int defaultValue) {
        return Integer.parseInt(get(key, String.valueOf(defaultValue)));
    }
}
