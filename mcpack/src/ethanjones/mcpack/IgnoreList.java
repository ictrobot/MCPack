package ethanjones.mcpack;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class IgnoreList {

  private final ArrayList<String> list = new ArrayList<String>();

  public IgnoreList(File local) {
    try {
      Scanner scan = new Scanner(new File(local, "mcpackignore"));
      while (scan.hasNextLine()) {
        String s = scan.nextLine().trim();
        if (!s.isEmpty() && !s.startsWith("#")) list.add(s.toLowerCase());
      }
    } catch (Exception e) {
      // ignored
    }
    if (!list.isEmpty()) {
      MCPack.log("Ignore List:");
      for (String s : list) {
        MCPack.log(s);
      }
      MCPack.log("");
    }
  }

  public boolean ignore(String rel) {
    rel = rel.toLowerCase();

    for (String s : list) {
      if (rel.startsWith(s)) {
        return true;
      }
    }
    return false;
  }
}
