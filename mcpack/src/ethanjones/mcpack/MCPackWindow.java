package ethanjones.mcpack;

import ethanjones.mcpack.util.Version;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MCPackWindow extends JFrame {

  private JLabel labelStatus;
  private JTextField textFieldLocal;
  private JTextField textFieldRemote;
  private JButton buttonUpdate;
  private JButton buttonConsole;
  private volatile boolean failed = false;
  private volatile boolean canExit = true;

  private String consoleText = "";
  private JTextArea console;

  private boolean showingTable = true;
  private JScrollPane tableView;
  private JScrollPane logView;

  public MCPackWindow() {
    setLayout(new BorderLayout());
    setResizable(true);
    setSize(800,  400);
    setTitle("MCPack - version " + Version.versionString);

    add(makeConfigPanel(), BorderLayout.NORTH);
    add(makeActionPanel(), BorderLayout.SOUTH);

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        if (canExit) exit();
      }
    });

    JTable table = new JTable(UpdateData.INSTANCE);
    table.getColumnModel().getColumn(1).setMaxWidth(200);
    tableView = new JScrollPane(table);
    add(tableView);

    console = new JTextArea(consoleText);
    logView = new JScrollPane(console);

    setVisible(true);

    if (MCPack.script) {
      startUpdate();
    }
  }

  private JPanel makeConfigPanel() {
    JPanel panel = new JPanel(new GridLayout(2, 2));

    JLabel labelLocal = new JLabel("Local:");
    panel.add(labelLocal);

    JLabel labelRemote = new JLabel("Remote:");
    panel.add(labelRemote);

    textFieldLocal = new JTextField();
    textFieldLocal.setText(MCPack.config.local);
    panel.add(textFieldLocal);

    textFieldRemote = new JTextField();
    textFieldRemote.setText(MCPack.config.remote);
    panel.add(textFieldRemote);

    return panel;
  }

  private JPanel makeActionPanel() {
    JPanel panel = new JPanel(new GridLayout(1, 3));

    buttonConsole = new JButton("Show log");
    buttonConsole.addActionListener(e -> {
      showingTable = !showingTable;
      buttonConsole.setText(showingTable ? "Show log" :  "Show table");
      remove(showingTable ? logView : tableView);
      add(showingTable ? tableView : logView);
      revalidate();
    });
    panel.add(buttonConsole);

    labelStatus = new JLabel("Ready", JLabel.CENTER);
    panel.add(labelStatus);

    buttonUpdate = new JButton("Update");
    buttonUpdate.addActionListener(e -> startUpdate());
    panel.add(buttonUpdate);

    return panel;
  }

  private void startUpdate() {
    if (labelStatus.getText().equals("Updating")) return;

    failed = false;
    consoleText = "";
    UpdateData.INSTANCE.resetChanges();
    MCPack.log("Updating");
    setStatus("Updating");

    canExit = false;

    updateConfig();

    new MCPackUpdater(MCPack.config.copy()).start();
  }

  private void exit() {
    MCPack.log("Exiting");

    updateConfig();

    if (failed) {
      System.exit(10);
    } else {
      System.exit(0);
    }
  }

  public void log(String str) {
    consoleText = consoleText + str + "\n";
    if (console != null) {
      console.setText(consoleText);
      console.setCaretPosition(consoleText.length());
    }
  }

  public void setStatus(String str) {
    MCPack.log(str);
    if (labelStatus != null) labelStatus.setText(str);
  }

  public void enableExit() {
    canExit = true;

    if (MCPack.script && labelStatus.getText().toLowerCase().contains("success")) {
      SwingUtilities.invokeLater(() -> {
        buttonConsole.setEnabled(false);
        buttonUpdate.setEnabled(false);

        new Thread(() -> {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ignored) {}
          SwingUtilities.invokeLater(this::exit);
        }).start();
      });
    }
  }

  private void updateConfig() {
    MCPack.config.local = textFieldLocal.getText();
    MCPack.config.remote = textFieldRemote.getText();
    MCPack.config.write();
  }

  public void setFailed() {
    this.failed = true;
  }
}
