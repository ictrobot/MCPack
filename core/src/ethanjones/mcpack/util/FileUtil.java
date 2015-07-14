package ethanjones.mcpack.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;

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

    public static String hashFile(File file) {
        String algorithm = "SHA-256";
        try (FileInputStream inputStream = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            byte[] bytesBuffer = new byte[1024];
            int bytesRead = -1;

            while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                digest.update(bytesBuffer, 0, bytesRead);
            }

            byte[] hashedBytes = digest.digest();

            return convertByteArrayToHexString(hashedBytes);
        } catch (Exception ex) {
            throw new RuntimeException("Could not generate hash from file " + file.getAbsolutePath(), ex);
        }
    }

    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuilder stringBuffer = new StringBuilder();
        for (byte arrayByte : arrayBytes) {
            stringBuffer.append(Integer.toString((arrayByte & 0xff) + 0x100, 16).substring(1));
        }
        return stringBuffer.toString();
    }
}
