package cn.pig.compat.common.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

   
                                                     
                                                     
   
public class Configuration {

    private final File file;
    private final Properties props = new Properties();
    private boolean loaded;

    public Configuration(File file) {
        this.file = file;
    }

    public void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (file == null || !file.isFile()) {
            return;
        }
        try {
            FileInputStream in = new FileInputStream(file);
            try {
                props.load(in);
            } finally {
                in.close();
            }
        } catch (Throwable ignored) {
        }
    }

    public void save() {
        if (file == null) {
            return;
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            FileOutputStream out = new FileOutputStream(file);
            try {
                props.store(out, "VexView injection config");
            } finally {
                out.close();
            }
        } catch (Throwable ignored) {
        }
    }

    public Property get(String category, String key, boolean defaultValue) {
        return property(category, key, String.valueOf(defaultValue));
    }

    public Property get(String category, String key, String defaultValue) {
        return property(category, key, defaultValue == null ? "" : defaultValue);
    }

    public Property get(String category, String key, double defaultValue) {
        return property(category, key, String.valueOf(defaultValue));
    }

    public Property get(String category, String key, int defaultValue) {
        return property(category, key, String.valueOf(defaultValue));
    }

    private Property property(String category, String key, String defaultValue) {
        String full = (category == null || category.isEmpty()) ? key : category + "." + key;
        String value = props.getProperty(full);
        if (value == null) {
            value = defaultValue;
            props.setProperty(full, value);
        }
        return new Property(value, defaultValue);
    }
}
