package ethanjones.mcpacker;

import ethanjones.data.Data;
import ethanjones.data.DataFormatter;
import ethanjones.util.FileUtil;
import ethanjones.data.DataGroup;
import ethanjones.util.Version;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MCPacker {

    private static DataGroup main = new DataGroup();
    private static File workingFolder = new File(System.getProperty("user.dir"));
    private static File outputFolder = new File(workingFolder, "output");
    private static File outputFilesFolder = new File(outputFolder, "files");

    private static HashMap<File, ArrayList<File>> managedFolders = new HashMap<File, ArrayList<File>>();

    private static ArrayList<File> folders = new ArrayList<File>();
    private static ArrayList<File> otherFiles = new ArrayList<File>();

    public static void main(String[] args) {
        FileUtil.delete(outputFolder);

        main.put("version", Version.versionCode);

        for (String arg : args) {
            managedFolders.put(new File(workingFolder, arg), new ArrayList<File>());
        }

        if (managedFolders.containsKey(workingFolder)) {
            loop(workingFolder, workingFolder);
        } else {
            loop(workingFolder, null);
        }

        outputFilesFolder.mkdirs(); //will also make outputFolder

        System.out.println(managedFolders.toString());
        System.out.println(folders.toString());
        System.out.println(otherFiles.toString());

        ArrayList<String> folderRelativeStrings = new ArrayList<String>();
        for (File folder : folders) {
            folderRelativeStrings.add(getRelative(folder));
        }
        System.out.println(folderRelativeStrings);
        main.put("folders", folderRelativeStrings.toArray(new String[folderRelativeStrings.size()]));

        DataGroup otherFilesGroup = main.getGroup("otherFiles");
        for (File otherFile : otherFiles) {
            String rel = getRelative(otherFile);
            otherFilesGroup.put(rel, FileUtil.hashFile(otherFile));

            File out = new File(outputFilesFolder, rel);
            new File(out.getParent()).mkdirs();
            FileUtil.copyFile(otherFile, out);
        }

        DataGroup managedFoldersGroup = main.getGroup("managedFolders");
        for (Map.Entry<File, ArrayList<File>> entry : managedFolders.entrySet()) {
            File managedFolder = entry.getKey();
            String rel = getRelative(managedFolder);
            DataGroup dataGroup = managedFoldersGroup.getGroup(rel);

            for (File file : entry.getValue()) {
                String fileRel = getRelative(file);
                dataGroup.put(fileRel, FileUtil.hashFile(file));

                File out = new File(outputFilesFolder, fileRel);
                new File(out.getParent()).mkdirs();
                FileUtil.copyFile(file, out);
            }
        }

        System.out.println();
        System.out.println(DataFormatter.str(main));
        try {
            Data.output(main, new File(outputFolder, "data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loop(File file, File managedFolder) {
        if (file.isDirectory()) {
            if (managedFolders.containsKey(file)) {
                managedFolder = file;
            }
            folders.add(file);

            String[] children = file.list();
            for (String child : children) {
                loop(new File(file, child), managedFolder);
            }
        } else {
            if (managedFolder != null) {
                managedFolders.get(managedFolder).add(file);
            } else {
                otherFiles.add(file);
            }
        }
    }

    public static String getRelative(File file) {
        Path pathAbsolute = file.toPath();
        Path pathBase = workingFolder.toPath();
        Path path = pathBase.relativize(pathAbsolute);
        return path.toString().replace("\\", "/");
    }
}
