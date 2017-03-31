package com.neeq.crawler.dependence;

/**
 * Created by bj on 16/7/19.
 */

import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class Config {
    private static Properties properties = new Properties();

    public Config() {
    }

    public static File getRootFile() {
        String path;
        if (Config.class.getClassLoader().getResource("") != null) {
            path = Config.class.getClassLoader().getResource("").getPath();
        } else {
            path = Config.class.getProtectionDomain().getCodeSource().getLocation().getFile();
            path = (new File(path)).getParentFile().getPath();
        }

        File file = (new File(path));
        return file;
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        if (defaultValue == null) {
            throw new NullPointerException("defaultValue can\'t be null");
        } else {
            String v = properties.getProperty(key);
            return v == null ? defaultValue : v;
        }
    }

    public static Integer getInt(String key, int defaultValue) {
        String v = properties.getProperty(key);
        return Integer.valueOf(v == null ? defaultValue : Integer.parseInt(v));
    }

    public static boolean getBool(String key, boolean defaultValue) {
        String v = properties.getProperty(key);
        return v == null ? defaultValue : Boolean.parseBoolean(v);
    }

    public static Properties getProps() {
        return properties;
    }

    static {
        try {
            File e = getRootFile();
            File pFile = new File(e.getPath() + File.separator + "/conf/config.properties");
            System.out.println("the config is :" + pFile.getAbsolutePath());
            File logFile = new File(e.getPath() + File.separator + "/conf/log4j.properties");
            if (logFile.exists()) {
                PropertyConfigurator.configure(new FileInputStream(logFile));
            }

            if (pFile.exists()) {
                InputStreamReader inputStream = new InputStreamReader(new FileInputStream(pFile));
                properties.load(inputStream);
            }

            System.getProperties().putAll(properties);
        } catch (IOException var4) {
            var4.printStackTrace();
        }

    }
}
