package ethanjones.mcpacker;

import ethanjones.mcpack.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class MCPackerFetch {

    public static boolean fetch(String from, String to) {
        if (from.toLowerCase().startsWith("http:") || from.toLowerCase().startsWith("https:")) {
            return remoteFetch(from, to);
        } else {
            return localFetch(from, to);
        }
    }

    private static boolean remoteFetch(String remote, String local) {
        try {
            MCPacker.log("Downloading " + remote + " to " + local);

            FileUtil.delete(new File(local));
            new File(local).getParentFile().mkdirs();
            new File(local).createNewFile();

            URL url = new URL(remote);
            Files.copy(url.openStream(), new File(local).toPath(), StandardCopyOption.REPLACE_EXISTING);

            MCPacker.log("Downloaded " + new File(remote).getName());
        } catch (Exception e) {
            MCPacker.log("Failed to download");
            MCPacker.log(e);
            return false;
        }
        return true;
    }

    private static boolean localFetch(String from, String to) {
        try {
            MCPacker.log("Copying " + from + " to " + to);

            FileUtil.delete(new File(to));
            new File(to).getParentFile().mkdirs();
            new File(to).createNewFile();

            FileInputStream inputStream = new FileInputStream(new File(from));
            Files.copy(inputStream, new File(to).toPath(), StandardCopyOption.REPLACE_EXISTING);

            MCPacker.log("Copied " + new File(to).getName());
        } catch (Exception e) {
            MCPacker.log("Failed to copy");
            MCPacker.log(e);
            return false;
        }
        return true;
    }
}
