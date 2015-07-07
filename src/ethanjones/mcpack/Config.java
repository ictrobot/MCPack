package ethanjones.mcpack;


import ethanjones.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class Config {
    private static String filename = System.getProperty("user.home") + "/.mcpack";
    private static File file = new File(filename);

    public String local;
    public String remote;

    public void read() {
        if (file.exists()) {
            if (file.isDirectory()) {
                FileUtil.delete(file);
            } else {
                try {
                    FileInputStream fileInputStream = new FileInputStream(file);
                    Properties properties = new Properties();
                    properties.load(fileInputStream);
                    fileInputStream.close();

                    System.out.println("Properties values");
                    properties.list(System.out);

                    local = properties.getProperty("local");
                    remote = properties.getProperty("remote");
                } catch (Exception e) {
                    MCPack.log("Failed to read config");
                    MCPack.log(e);
                    FileUtil.delete(file);
                }
            }
        }
    }

    public void write() {
        Properties properties = new Properties();
        properties.setProperty("local", local);
        properties.setProperty("remote", remote);

        if (file.exists() && file.isDirectory()) {
            FileUtil.delete(file);
        }

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            properties.store(fileOutputStream, "Ethan Jones - MCPack");
            fileOutputStream.close();
        } catch (Exception e) {
            MCPack.log("Failed to write config");
            MCPack.log(e);
            FileUtil.delete(file);
        }

    }

    public Config copy() {
        Config config = new Config();
        config.local = this.local;
        config.remote = this.remote;
        return config;
    }

}
