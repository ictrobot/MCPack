package ethanjones.mcpacker;


import ethanjones.mcpack.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Properties;

public class MCPackerConfig {
    private static String filename = System.getProperty("user.home") + "/.mcpacker";
    private static File file = new File(filename);

    public String input;
    public String output;
    public String diff;
    public String[] managedFolders = new String[]{};

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

                    input = properties.getProperty("input");
                    output = properties.getProperty("output");
                    diff = properties.getProperty("diff");
                    managedFolders = new String[Integer.parseInt(properties.getProperty("managedFoldersLength"))];
                    for (int i = 0; i < managedFolders.length; i++) {
                        managedFolders[i] = properties.getProperty("managedFolders" + i);
                    }
                } catch (Exception e) {
                    MCPacker.log("Failed to read config");
                    MCPacker.log(e);
                    FileUtil.delete(file);
                }
            }
        }
    }

    public void write() {
        Properties properties = new Properties();
        properties.setProperty("input", input);
        properties.setProperty("output", output);
        properties.setProperty("diff", diff);
        properties.setProperty("managedFoldersLength", Integer.toString(managedFolders.length));
        for (int i = 0; i < managedFolders.length; i++) {
            properties.setProperty("managedFolders" + i, managedFolders[i]);
        }

        if (file.exists() && file.isDirectory()) {
            FileUtil.delete(file);
        }

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            properties.store(fileOutputStream, "Ethan Jones - MCPack");
            fileOutputStream.close();
        } catch (Exception e) {
            MCPacker.log("Failed to write config");
            MCPacker.log(e);
            FileUtil.delete(file);
        }

    }

    public MCPackerConfig copy() {
        MCPackerConfig config = new MCPackerConfig();
        config.input = this.input;
        config.output = this.output;
        config.diff = this.diff;
        config.managedFolders = Arrays.copyOf(managedFolders, managedFolders.length);
        return config;
    }

}
