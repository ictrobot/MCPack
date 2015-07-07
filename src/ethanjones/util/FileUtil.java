package ethanjones.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class FileUtil {
    public static void delete(File file) {
        if (!file.exists()) return;

        if (file.isDirectory()) {
            String[] children = file.list();
            for (String child : children) {
                delete(new File(file, child));
            }
        }
        if (!file.delete()) {
            throw new RuntimeException("Failed to delete " + file.getAbsolutePath());
        }
    }

    public static void copyFile(File in, File out) {
        try {
            FileChannel src = new FileInputStream(in).getChannel();
            FileChannel dest = new FileOutputStream(out).getChannel();
            dest.transferFrom(src, 0, src.size());
            src.close();
            dest.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy " + in.getAbsolutePath() + " to " + out.getAbsolutePath(), e);
        }
    }
}
