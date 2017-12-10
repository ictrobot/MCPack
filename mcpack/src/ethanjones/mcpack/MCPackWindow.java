package ethanjones.mcpack;

import ethanjones.mcpack.util.Version;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MCPackWindow extends Frame {
  private Label labelLocal;
  private Label labelRemote;
  private Label labelStatus;
  private TextField textFieldLocal;
  private TextField textFieldRemote;
  private Button buttonUpdate;
  private Button buttonExit;

  private TextArea console;
  private String consoleText = "";

  private MCPackWindow window;

  public MCPackWindow() {
    window = this;

    setLayout(null);
    setResizable(false);
    setSize(700, 400);
    setTitle("MCPack - version " + Version.versionCode);

    labelLocal = new Label("Local:");
    labelLocal.setBounds(5, 25, 50, 25);
    add(labelLocal);

    labelRemote = new Label("Remote:");
    labelRemote.setBounds(5, 55, 50, 25);
    add(labelRemote);

    labelStatus = new Label("", Label.CENTER);
    labelStatus.setBounds(60, 85, this.getWidth() - 120, 25);
    add(labelStatus);

    textFieldLocal = new TextField();
    textFieldLocal.setBounds(60, 25, this.getWidth() - 65, 25);
    textFieldLocal.setText(MCPack.config.local);
    add(textFieldLocal);

    textFieldRemote = new TextField();
    textFieldRemote.setBounds(60, 55, this.getWidth() - 65, 25);
    textFieldRemote.setText(MCPack.config.remote);
    add(textFieldRemote);

    buttonUpdate = new Button("Update");
    buttonUpdate.setBounds(this.getWidth() - 55, 85, 50, 25);
    buttonUpdate.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        startUpdate();
      }
    });
    add(buttonUpdate);

    buttonExit = new Button("Exit");
    buttonExit.setBounds(5, 85, 50, 25);
    buttonExit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        exit();
      }
    });
    add(buttonExit);

    console = new TextArea(consoleText, 0, 0);
    console.setBounds(5, 115, this.getWidth() - 10, this.getHeight() - 120);
    add(console);

    setVisible(true);

    if (MCPack.script) {
      window.remove(buttonUpdate);
      startUpdate();
    }
  }

  private void startUpdate() {
    MCPack.log("Updating");
    setStatus("Updating");

    window.remove(buttonExit);

    updateConfig();

    new MCPackUpdater(MCPack.config.copy()).start();
  }

  private void exit() {
    MCPack.log("Exiting");

    updateConfig();

    System.exit(0);
  }

  public void log(String str) {
    consoleText = consoleText + str + "\n";
    if (console != null) console.setText(consoleText);
  }

  public void setStatus(String str) {
    MCPack.log(str);
    if (labelStatus != null) labelStatus.setText(str);
  }

  public void addExit() {
    add(buttonExit);

    if (MCPack.script && labelStatus.getText().toLowerCase().contains("success")) {
      exit();
    }
  }

  private void updateConfig() {
    MCPack.config.local = textFieldLocal.getText();
    MCPack.config.remote = textFieldRemote.getText();
    MCPack.config.write();
  }
}
