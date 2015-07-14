package ethanjones.mcpacker;

import ethanjones.data.Data;
import ethanjones.data.DataFormatter;
import ethanjones.mcpack.util.FileUtil;
import ethanjones.data.DataGroup;
import ethanjones.mcpack.util.Version;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MCPackerUpdater extends Thread {

    private static final String temp = System.getProperty("java.io.tmpdir") + "/ethanjones/mcpacker/";

    private DataGroup main;
    private ArrayList index;
    private File inputFolder;
    private File outputFolder;
    private File outputFilesFolder;
    private String diff;
    private boolean diffMode;
    private String[] managedFolders;

    public MCPackerUpdater(MCPackerConfig config) {
        this.main = new DataGroup();
        this.inputFolder = new File(config.input);
        this.outputFolder = new File(config.output);
        this.outputFilesFolder = new File(outputFolder, "files");
        this.diff = config.diff;
        this.diffMode = diff != null && !diff.isEmpty();
        this.managedFolders = config.managedFolders;
    }

    private HashMap<File, ArrayList<File>> managed = new HashMap<File, ArrayList<File>>();

    public void run() {
        //Empty output folder
        for (File file : outputFolder.listFiles()) {
            FileUtil.delete(file);
        }

        //Diff
        if (diffMode) {
            if (!MCPackerFetch.fetch(diff + "/index", temp + "/index")) return;
            MCPacker.log("Opening diff index");
            try {
                index = (ArrayList) Data.input(new File(temp + "/index"));
            } catch (Exception e) {
                MCPacker.log(e);
                return;
            }
            MCPacker.log("Opened diff index\n");
            System.out.println(DataFormatter.str(index) + "\n");
        } else {
            index = new ArrayList();
        }

        //Add version to datagroup
        main.put("version", Version.versionCode);

        //Add managed folders
        for (String str : managedFolders) {
            managed.put(new File(inputFolder, str.trim()), new ArrayList<File>());
        }

        //Loop
        if (managed.containsKey(inputFolder)) {
            loop(inputFolder, inputFolder);
        } else {
            loop(inputFolder, null);
        }

        //Make output folders
        outputFilesFolder.mkdirs();


        //Log managed
        MCPacker.log("Managed:\n" + managed.toString() + "\n");


        DataGroup managedGroup = main.getGroup("managed");
        for (Map.Entry<File, ArrayList<File>> entry : managed.entrySet()) {
            File file = entry.getKey();
            String rel = getRelative(file);

            if (entry.getValue() != null) {
                MCPacker.log("Managed folder " + rel);
                DataGroup dataGroup = managedGroup.getGroup(rel);
                ArrayList folders = dataGroup.getList("folders");
                DataGroup files = dataGroup.getGroup("files");
                for (File f: entry.getValue()) {
                    String fRel = getRelative(f);
                    if (f.isDirectory()) {
                        MCPacker.log("Folder " + fRel);
                        folders.add(fRel);
                    } else {
                        String hash = FileUtil.hashFile(f);
                        MCPacker.log("File " + fRel + " has a hash of " + hash);
                        files.put(fRel, hash);

                        copyFile(f, hash);
                    }
                }
                MCPacker.log("");
            } else {
                if (file.isDirectory()) {
                    MCPacker.log("Folder " + rel + "\n");
                    managedGroup.put(rel, 0);
                } else {
                    String hash = FileUtil.hashFile(file);
                    MCPacker.log("Managed file " + rel + " has a hash of " + hash);
                    managedGroup.put(rel, hash);

                    copyFile(file, hash);
                    MCPacker.log("");
                }
            }
        }

        MCPacker.log("Data:\n" + DataFormatter.str(main) + "\n");
        try {
            Data.output(main, new File(outputFolder, "data"));
            MCPacker.log("Successfully wrote data to file");
        } catch (IOException e) {
            MCPacker.log("Failed to write data to file");
            MCPacker.log(e);
        }

        MCPacker.log("\nIndex:\n" + DataFormatter.str(index) + "\n");
        try {
            Data.output(index, new File(outputFolder, "index"));
            MCPacker.log("Successfully wrote index to file");
        } catch (IOException e) {
            MCPacker.log("Failed to write index to file");
            MCPacker.log(e);
        }

        MCPacker.window.addExit();
        MCPacker.window.setStatus("Successful");
    }

    private void copyFile(File file,String hash) {
        if (index.contains(hash)) {
            MCPacker.log("Index already contains " + hash);
        } else {
            FileUtil.copyFile(file, new File(outputFilesFolder, hash));
            index.add(hash);
            MCPacker.log("Successfully copied file " + getRelative(file));
        }
    }

    private void loop(File file, File managedFolder) {
        if (file.isDirectory() && managed.containsKey(file)) {
            managedFolder = file;
        } else if (file != inputFolder) {
            if (managedFolder != null) {
                managed.get(managedFolder).add(file);
            } else {
                managed.put(file, null);
            }
        }

        if (file.isDirectory()) {
            String[] children = file.list();
            for (String child : children) {
                loop(new File(file, child), managedFolder);
            }
        }
    }

    public String getRelative(File file) {
        Path pathAbsolute = file.toPath();
        Path pathBase = inputFolder.toPath();
        Path path = pathBase.relativize(pathAbsolute);
        return path.toString().replace("\\", "/");
    }
}
