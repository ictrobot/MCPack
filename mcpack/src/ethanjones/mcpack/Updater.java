package ethanjones.mcpack;

import ethanjones.data.Data;
import ethanjones.data.DataGroup;
import ethanjones.data.DataFormatter;
import ethanjones.util.FileUtil;
import ethanjones.util.Version;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Updater extends Thread {

    private final Config config;
    private final File workingFolder;
    private final String temp = System.getProperty("java.io.tmpdir") + "/ethanjones/mcpack/";

    public Updater(Config config) {
        config.local  = config.local + "/";
        this.config = config;
        this.workingFolder = new File(config.local);
    }

    @Override
    public void run() {
        new File(temp).mkdirs();
        MCPack.log("Temp folder " + temp);
        MCPack.log("Local folder " + workingFolder.getAbsolutePath());
        MCPack.log("Remote " + config.remote);

        if (!Fetch.fetch(config.remote + "/data", temp + "/data")) return;

        MCPack.log("Opening data");
        DataGroup dataGroup;
        try {
            dataGroup = (DataGroup) Data.input(new File(temp + "/data"));
        } catch (Exception e) {
            MCPack.log(e);
            return;
        }
        MCPack.log("Opened data\n");
        MCPack.log(DataFormatter.str(dataGroup));
        MCPack.log("\n");

        int versionCode = dataGroup.getInteger("version");
        if (versionCode != Version.versionCode) {
            MCPack.log("Please update MCPack to version " + versionCode);
            MCPack.window.addExit();
            MCPack.window.setStatus("Please update MCPack to version " + versionCode);
            return;
        } else {
            MCPack.log("MCPack version matches");
        }

        MCPack.log("Creating folders");
        String[] folders = dataGroup.getArray("folders", String.class);
        for (String folder : folders) {
            MCPack.log("Creating " + folder);
            System.out.println(new File(workingFolder, folder).mkdirs());
        }
        MCPack.log("Created folders");

        List<String> folderList = Arrays.asList(folders);

        MCPack.log("Checking managed folders");
        DataGroup managedFolders = dataGroup.getGroup("managedFolders");
        for (Map.Entry<String, Object> entry : managedFolders.entrySet()) {
            MCPack.log("Checking " + entry.getKey());
            DataGroup d = (DataGroup) entry.getValue();
            File f = new File(workingFolder, entry.getKey());
            if (!check(f, d, folderList)) return;

            for (Map.Entry<String, Object> e : d.entrySet()) {
                MCPack.log("File " + e.getKey() + " does not exist locally");
                if (!fetch(e.getKey())) return;
            }
            MCPack.log("Finished checking " + entry.getKey());
        }
        MCPack.log("Finished checking managed folders");

        MCPack.log("Checking other files");
        DataGroup otherFiles = dataGroup.getGroup("otherFiles");
        for (Map.Entry<String, Object> entry : otherFiles.entrySet()) {
            String rel = entry.getKey();

            File local = new File(workingFolder, rel);
            if (local.exists()) {
                String hash = FileUtil.hashFile(local);
                String remoteHash = (String) entry.getValue();
                if (!hash.equals(remoteHash)) {
                    MCPack.log("Hash of file " + rel + " has changed");
                    if (!fetch(rel)) return;
                } else {
                    MCPack.log("Hash of file " + rel + " matches remote");
                }
            } else {
                MCPack.log("File " + rel + " does not exist locally");
                if (!fetch(rel)) return;
            }
        }
        MCPack.log("Finished checking other files");

        MCPack.window.addExit();
        MCPack.window.setStatus("Successfully updated");
    }

    public boolean check(File file, DataGroup dataGroup, List<String> folderList) {
        String rel = getRelative(file);

        if (file.isDirectory()) {
            if (folderList.contains(rel)) {
                MCPack.log("Folder " + rel + " exists on remote");
                String[] children = file.list();
                for (String child : children) {
                    if (!check(new File(file, child), dataGroup, folderList)) return false;
                }
            } else {
                MCPack.log("Folder " + rel + " does not exist on remote, deleting");
                FileUtil.delete(file);
            }
        } else {
            if (dataGroup.containsKey(rel)) {
                String hash = FileUtil.hashFile(file);
                String remoteHash = dataGroup.getString(rel);
                if (!hash.equals(remoteHash)) {
                    MCPack.log("Hash of file " + rel + " has changed");
                    if (!fetch(rel)) return false;
                } else {
                    MCPack.log("Hash of file " + rel + " matches remote");
                }
                dataGroup.remove(rel);
            } else {
                MCPack.log("File " + rel + " does not exist on remote, deleting");
                FileUtil.delete(file);
            }
        }
        return true;
    }

    public boolean fetch(String rel) {
        return Fetch.fetch(config.remote + "/files/" + rel, config.local + rel);
    }

    public String getRelative(File file) {
        Path pathAbsolute = file.toPath();
        Path pathBase = workingFolder.toPath();
        Path path = pathBase.relativize(pathAbsolute);
        return path.toString().replace("\\", "/");
    }
}
