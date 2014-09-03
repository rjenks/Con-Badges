package com.universalbits.conorganizer.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by rjenks on 9/1/2014.
 */
public class Settings implements ISettings {
    public static final String PROPERTY_APP_NAME = "appName";
    private static final Logger LOGGER = Logger.getLogger(Settings.class.getSimpleName());
    private static Settings instance;

    private final String name;
    private final File propFile;
    private final Properties prop;

    public Settings(final String appName) {
        final File homeDir = new File(System.getProperty("user.home"));
        this.name = appName;
        this.prop = new Properties();
        propFile = new File(homeDir, name + ".properties");
        try {
            if (propFile.exists()) {
                prop.load(new FileInputStream(propFile));
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        setProperty(PROPERTY_APP_NAME, appName);
    }

    public static void init(String appName) {
        if (instance != null) {
            throw new IllegalStateException("Settings already initialized");
        }
        instance = new Settings(appName);
    }

    public static ISettings getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Settings not initialized");
        }
        return instance;
    }

    @Override
    public void setProperty(final String name, final String value) {
        prop.setProperty(name, value);
        save();
    }

    @Override
    public String getProperty(String name) {
        return prop.getProperty(name);
    }

    @Override
    public String getProperty(final String name, final String defaultValue) {
        return prop.getProperty(name, defaultValue);
    }

    @Override
    public double getPropertyDouble(final String name, final double defaultValue) {
        double value = defaultValue;
        final String valueString = prop.getProperty(name);
        if (valueString != null) {
            try {
                value = Double.parseDouble(valueString);
            } catch (NumberFormatException nfe) {
                LOGGER.log(Level.WARNING, "Property " + name + " could not be converted to a floating point number value='" + valueString + "'");
            }
        }
        return value;
    }

    private void save() {
        try {
            prop.store(new FileOutputStream(propFile), "settings for " + name);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
