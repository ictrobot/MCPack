package ethanjones.mcpacker;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MCPacker {
  public static final MCPackerConfig config = new MCPackerConfig();
  public static MCPackerWindow window;

  public static void main(String[] args) {
    config.read();
    if (args.length == 1 && args[0].toLowerCase().equals("run")) {
      new MCPackerUpdater(config.copy()).start();
    } else {
      window = new MCPackerWindow();
    }
  }

  public static void log(String str) {
    System.out.println(str);
    if (window != null) window.log(str);
  }

  public static void log(Exception e) {
    e.printStackTrace();
    if (window != null) {
      window.log(getStackTrace(e));
      window.addExit();
      window.setStatus("Failed");
    }
  }

  private static String getStackTrace(final Throwable throwable) {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw, true);
    throwable.printStackTrace(pw);
    return sw.getBuffer().toString();
  }

  public static void success() {
    if (window != null) {
      window.addExit();
      window.setStatus("Successful");
    } else {
      System.exit(0);
    }
  }
}
