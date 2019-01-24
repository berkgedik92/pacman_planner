package Main;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

public class Config {
    private static Config instance;
    private Map cfg = null;

    private Config() {}

    private Config(String fname) {
        Yaml yaml = new Yaml();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(fname));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.cfg = (Map) yaml.load(inputStream);
    }

    public Object get(String key) {
        return this.cfg.getOrDefault(key, null);
    }

    public boolean getBoolean(String key) {
        return (boolean) get(key);
    }

    public int getInt(String key) {
        return (int) get(key);
    }

    public String getString(String key) {
        return (String) get(key);
    }

    public static Config getInstance(){
        if(instance == null) {
            instance = new Config();
        }
        return instance;
    }

    public static void load(String fname) {
        instance = new Config(fname);
    }


}
