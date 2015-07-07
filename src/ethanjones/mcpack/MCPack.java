package ethanjones.mcpack;


import java.io.PrintWriter;
import java.io.StringWriter;

public class MCPack {

    public static final Config config = new Config();
    public static Window window;

    public static void main(String[] args) {
        config.read();
        window = new Window();
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
            window.setStatus("Update failed");
        }
    }

    private static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
