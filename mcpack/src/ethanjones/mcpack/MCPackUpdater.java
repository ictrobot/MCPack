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
  private static final UpdateData ud = UpdateData.INSTANCE;

  private final MCPackConfig config;
  private final File workingFolder;
  private final IgnoreList ignoreList;
  private final MCPackWorker.WorkerTask workerTask;

  public MCPackUpdater(MCPackConfig config) {
    config.local = config.local + "/";
    this.config = config;
    this.workingFolder = new File(config.local);
    this.ignoreList = new IgnoreList(workingFolder);
    this.workerTask = new MCPackWorker.WorkerTask();
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
      MCPack.window.enableExit();
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
        ud.addChange(rel, UpdateData.ChangeType.IGNORED);
        continue;
      }

      Object o = entry.getValue();
      if (o == (Integer) 0) {
        if (local.exists() && local.isDirectory()) {
          MCPack.log("Folder " + rel + " already exists");
          ud.addChange(rel, UpdateData.ChangeType.MATCHES);
        } else {
          local.delete();
          ud.addChange(rel, UpdateData.ChangeType.ONLY_SERVER);
          if (local.mkdirs()) {
            MCPack.log("Folder " + rel + " successfully made");
          } else {
            MCPack.log("Failed to make folder " + rel);
            MCPack.log(new RuntimeException());
            return;
          }
        }
      } else if (o instanceof String) {
        workerTask.addFileTask(() -> managedFile(rel, (String) o, local));
      } else {
        MCPack.log("Managed folder " + rel);
        DataGroup d = (DataGroup) entry.getValue();
        DataGroup files = d.getGroup("files");
        if (!managedFolder(local, d.getList("folders"), files, local)) return;
        workerTask.addFolderTask(() -> {
          if (files.size() > 0) {
            MCPack.log("Extra files exist on remote in " + rel);
            for (Map.Entry<String, Object> e : files.entrySet()) {
              workerTask.addFileTask(() -> {
                MCPack.log("File " + e.getKey() + " does not exist locally");
                ud.addChange(e.getKey(), UpdateData.ChangeType.ONLY_SERVER);
                return fetch(((String) e.getValue()), e.getKey());
              });
            }
          } else {
            MCPack.log("No extra files exist on remote in " + rel);
          }
          return true;
        });
      }
      MCPack.log("");
    }

    MCPack.log("\nDoing work");
    if (!workerTask.run()) {
      MCPack.log("Work failed");
      MCPack.log(new RuntimeException());
      return;
    } else {
      MCPack.log("Done work\n");
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
    MCPack.window.enableExit();
  }

  private boolean managedFile(String rel, String remoteHash, File local) {
    if (local.exists()) {
      String hash = FileUtil.hashFile(local);
      if (!hash.equals(remoteHash)) {
        MCPack.log("Hash of managed file " + rel + " has changed");
        ud.addChange(rel, UpdateData.ChangeType.CHANGED);
        return fetch(remoteHash, rel);
      } else {
        MCPack.log("Hash of managed file " + rel + " matches remote");
        ud.addChange(rel, UpdateData.ChangeType.MATCHES);
      }
    } else {
      MCPack.log("Managed file " + rel + " does not exist locally");
      ud.addChange(rel, UpdateData.ChangeType.ONLY_SERVER);
      return fetch(remoteHash, rel);
    }
    return true;
  }

  private boolean file(String rel, File file, DataGroup files) {
    if (files.containsKey(rel)) {
      String hash = FileUtil.hashFile(file);
      String remoteHash = files.getString(rel);
      if (!hash.equals(remoteHash)) {
        MCPack.log("Hash of file " + rel + " has changed");
        UpdateData.INSTANCE.addChange(rel, UpdateData.ChangeType.CHANGED);
        if (!fetch(remoteHash, rel)) return false;
      } else {
        MCPack.log("Hash of file " + rel + " matches remote");
        UpdateData.INSTANCE.addChange(rel, UpdateData.ChangeType.MATCHES);
      }
      files.remove(rel);
    } else {
      MCPack.log("File " + rel + " does not exist on remote, deleting");
      UpdateData.INSTANCE.addChange(rel, UpdateData.ChangeType.ONLY_LOCAL);
      FileUtil.delete(file);
    }
    return true;
  }

  public boolean managedFolder(File file, ArrayList folders, DataGroup files, File managedFolder) {
    String rel = getRelative(file);
    if (ignoreList.ignore(rel)) {
      MCPack.log("IGNORING " + rel);
      return true;
    }

    if (file.isDirectory()) {
      if (!folders.contains(rel) && file != managedFolder) {
        MCPack.log("Folder " + rel + " does not exist on remote, deleting");
        UpdateData.INSTANCE.addChange(rel, UpdateData.ChangeType.ONLY_LOCAL);
        FileUtil.delete(file);
      } else {
        if (file != managedFolder) {
          MCPack.log("Folder " + rel + " exists on remote");
          UpdateData.INSTANCE.addChange(rel, UpdateData.ChangeType.MATCHES);
        }
        String[] children = file.list();
        for (String child : children) {
          if (!managedFolder(new File(file, child), folders, files, managedFolder)) return false;
        }
      }
    } else {
      workerTask.addFileTask(() -> file(rel, file, files));
    }
    return true;
  }

  public boolean fetch(String hash, String rel) {
    if (ignoreList.ignore(rel)) {
      MCPack.log("IGNORING " + rel);
      UpdateData.INSTANCE.addChange(rel, UpdateData.ChangeType.IGNORED);
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
