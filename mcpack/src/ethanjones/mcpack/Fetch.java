package ethanjones.mcpack;

import ethanjones.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Fetch {

    public static boolean fetch(String from, String to) {
        if (from.toLowerCase().startsWith("http:") || from.toLowerCase().startsWith("https:")) {
            return remoteFetch(from, to);
        } else {
            return localFetch(from, to);
        }
    }

    private static boolean remoteFetch(String remote, String local) {
        try {
            MCPack.log("Downloading " + remote + " to " + local);

            FileUtil.delete(new File(local));
            new File(local).createNewFile();

            URL url = new URL(remote);
            Files.copy(url.openStream(), new File(local).toPath(), StandardCopyOption.REPLACE_EXISTING);

            MCPack.log("Downloaded " + remote);
        } catch (Exception e) {
            MCPack.log("Failed to download");
            MCPack.log(e);
            return false;
        }
        return true;
    }

    private static boolean localFetch(String from, String to) {
        try {
            MCPack.log("Copying " + from + " to " + to);

            FileUtil.delete(new File(to));
            new File(to).createNewFile();

            FileInputStream inputStream = new FileInputStream(new File(from));
            Files.copy(inputStream, new File(to).toPath(), StandardCopyOption.REPLACE_EXISTING);

            MCPack.log("Copied " + from);
        } catch (Exception e) {
            MCPack.log("Failed to copy");
            MCPack.log(e);
            return false;
        }
        return true;
    }
}
