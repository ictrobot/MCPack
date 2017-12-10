package ethanjones.mcpack;

import ethanjones.data.Data;
import ethanjones.data.DataFormatter;
import ethanjones.data.DataGroup;
import ethanjones.mcpack.util.FileUtil;
import ethanjones.mcpack.util.Version;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

public class MCPackUpdater extends Thread {

  private static final String temp = System.getProperty("java.io.tmpdir") + "/ethanjones/mcpack/";

  private final MCPackConfig config;
  private final File workingFolder;
  private final IgnoreList ignoreList;

  public MCPackUpdater(MCPackConfig config) {
    config.local = config.local + "/";
    this.config = config;
    this.workingFolder = new File(config.local);
    this.ignoreList = new IgnoreList(workingFolder);
  }

  @Override
  public void run() {
    new File(temp).mkdirs();
    MCPack.log("Temp folder " + temp);
    MCPack.log("Local folder " + workingFolder.getAbsolutePath());
    MCPack.log("Remote " + config.remote);

    if (!MCPackFetch.fetch(config.remote + "/data", temp + "/data")) return;

    MCPack.log("Opening data");
    DataGroup dataGroup;
    try {
      dataGroup = (DataGroup) Data.input(new File(temp + "/data"));
    } catch (Exception e) {
      MCPack.log(e);
      return;
    }
    MCPack.log("Opened data\n");
    System.out.println(DataFormatter.str(dataGroup) + "\n");

    int versionCode = dataGroup.getInteger("version");
    if (versionCode != Version.versionCode) {
      MCPack.log("Please update MCPack to version " + versionCode);
      MCPack.window.setStatus("Please update MCPack to version " + versionCode);
      MCPack.window.addExit();
      return;
    } else {
      MCPack.log("MCPack version matches");
    }
    MCPack.log("");

    // use minecraft/ not .minecraft/
    File dotMinecraft = new File(workingFolder, ".minecraft");
    FileUtil.delete(dotMinecraft);

    File mmcpack = new File(workingFolder, "mmc-pack.json");
    String mmcpackhash = mmcpack.exists() ? FileUtil.hashFile(mmcpack) : "";
    if (mmcpack.exists()) setMMCPackWritable(false);

    DataGroup managedGroup = dataGroup.getGroup("managed");
    for (Map.Entry<String, Object> entry : managedGroup.entrySet()) {
      String rel = entry.getKey();
      File local = new File(workingFolder, rel);

      if (ignoreList.ignore(rel)) {
        MCPack.log("IGNORING " + rel);
        continue;
      }

      Object o = entry.getValue();
      if (o == (Integer) 0) {
        if (local.exists() && local.isDirectory()) {
          MCPack.log("Folder " + rel + " already exists");
        } else {
          local.delete();
          if (local.mkdirs()) {
            MCPack.log("Folder " + rel + " successfully made");
          } else {
            MCPack.log("Failed to make folder " + rel);
            MCPack.log(new RuntimeException());
            return;
          }
        }
      } else if (o instanceof String) {
        String remoteHash = (String) o;
        if (local.exists()) {
          String hash = FileUtil.hashFile(local);
          if (!hash.equals(remoteHash)) {
            MCPack.log("Hash of managed file " + rel + " has changed");
            if (!fetch(remoteHash, rel)) return;
          } else {
            MCPack.log("Hash of managed file " + rel + " matches remote");
          }
        } else {
          MCPack.log("Managed file " + rel + " does not exist locally");
          if (!fetch(remoteHash, rel)) return;
        }
      } else {
        MCPack.log("Managed folder " + rel);
        DataGroup d = (DataGroup) entry.getValue();
        DataGroup files = d.getGroup("files");
        if (!check(local, d.getList("folders"), files, local)) return;

        if (files.size() > 0) {
          for (Map.Entry<String, Object> e : files.entrySet()) {
            MCPack.log("File " + e.getKey() + " does not exist locally");
            if (!fetch(((String) e.getValue()), e.getKey())) return;
          }
        } else {
          MCPack.log("No extra files exist on remote");
        }
      }
      MCPack.log("");
    }

    if (mmcpack.exists()) {
      String newMmcpackHash = FileUtil.hashFile(mmcpack);
      MCPack.log("mmc-pack.json old hash " + mmcpackhash);
      MCPack.log("mmc-pack.json new hash " + newMmcpackHash);
      if (!newMmcpackHash.equals(mmcpackhash)) {
        MCPack.log("Restart needed");

        MCPack.window.setStatus("Restart MultiMC Instance");
        JOptionPane.showMessageDialog(null, "Please restart your MultiMC instance");
        System.exit(11);
        return;
      }
    }

    MCPack.window.setStatus("Successfully updated");
    MCPack.window.addExit();
  }

  public boolean check(File file, ArrayList folders, DataGroup files, File managedFolder) {
    String rel = getRelative(file);
    if (ignoreList.ignore(rel)) {
      MCPack.log("IGNORING " + rel);
      return true;
    }

    if (file.isDirectory()) {
      if (!folders.contains(rel) && file != managedFolder) {
        MCPack.log("Folder " + rel + " does not exist on remote, deleting");
        FileUtil.delete(file);
      } else {
        if (file != managedFolder) MCPack.log("Folder " + rel + " exists on remote");
        String[] children = file.list();
        for (String child : children) {
          if (!check(new File(file, child), folders, files, managedFolder)) return false;
        }
      }
    } else {
      if (files.containsKey(rel)) {
        String hash = FileUtil.hashFile(file);
        String remoteHash = files.getString(rel);
        if (!hash.equals(remoteHash)) {
          MCPack.log("Hash of file " + rel + " has changed");
          if (!fetch(remoteHash, rel)) return false;
        } else {
          MCPack.log("Hash of file " + rel + " matches remote");
        }
        files.remove(rel);
      } else {
        MCPack.log("File " + rel + " does not exist on remote, deleting");
        FileUtil.delete(file);
      }
    }
    return true;
  }

  public boolean fetch(String hash, String rel) {
    if (ignoreList.ignore(rel)) {
      MCPack.log("IGNORING " + rel);
      return true;
    }

    if (rel.equals("mmc-pack.json")) setMMCPackWritable(true);
    boolean f = MCPackFetch.fetch(config.remote + "/files/" + hash, config.local + rel);
    if (rel.equals("mmc-pack.json")) setMMCPackWritable(false);
    return f;
  }

  private void setMMCPackWritable(boolean writable) {
    try {
      MCPack.log("Making mmc-pack.json read only");
      if (!new File(workingFolder, "mmc-pack.json").setWritable(writable)) throw new RuntimeException("Failed");
    } catch (Exception e) {
      MCPack.log("Failed making mmc-pack.json read only");
      e.printStackTrace();
    }
  }

  public String getRelative(File file) {
    Path pathAbsolute = file.toPath();
    Path pathBase = workingFolder.toPath();
    Path path = pathBase.relativize(pathAbsolute);
    return path.toString().replace("\\", "/");
  }
}
