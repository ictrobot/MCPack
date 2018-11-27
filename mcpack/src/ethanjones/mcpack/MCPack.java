package ethanjones.mcpack;


import java.io.*;
import java.util.Properties;

public class MCPack {

  public static final MCPackConfig config = new MCPackConfig();
  public static MCPackWindow window;

  public static boolean script = false;

  public static void main(String[] args) {
    if (args.length == 0) {
      config.read();
      script = false;
    } else if (args.length == 1) {
      try {
        config.local = args[0];
        config.remote = getRemote(new File(args[0]));
        script = true;
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      config.local = args[0];
      config.remote = args[1];
      script = true;
    }
    window = new MCPackWindow();
  }

  private static String getRemote(File local) {
    try {
      File file = new File(local, "mcpack.properties");
      FileInputStream fileInput = new FileInputStream(file);
      Properties properties = new Properties();
      properties.load(fileInput);
      fileInput.close();

      String remote = properties.getProperty("remote", null);
      if (remote == null || remote.isEmpty()) throw new RuntimeException("No remote defined");
      return remote;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void log(String str) {
    System.out.println(str);
    if (window != null) window.log(str);
  }

  public static void log(Exception e) {
    e.printStackTrace();
    if (window != null) {
      window.setFailed();
      window.log(getStackTrace(e));
      window.setStatus("Update failed");
      MCPack.window.enableExit();
    }
  }

  private static String getStackTrace(final Throwable throwable) {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw, true);
    throwable.printStackTrace(pw);
    return sw.getBuffer().toString();
  }
}
