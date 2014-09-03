package com.universalbits.conorganizer.common;

/**
 * Created by rjenks on 9/1/2014.
 */
public interface ISettings {
    void setProperty(String name, String value);
    String getProperty(String name);
    String getProperty(String name, String defaultValue);
    double getPropertyDouble(String name, double defaultValue);
}
