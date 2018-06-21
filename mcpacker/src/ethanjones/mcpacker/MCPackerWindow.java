package ethanjones.mcpacker;

import ethanjones.mcpack.util.Version;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MCPackerWindow extends Frame {
  private Label labelInput;
  private Label labelOutput;
  private Label labelDiff;
  private Label labelManagedFolders;
  private Label labelStatus;
  private TextField textFieldInput;
  private TextField textFieldOutput;
  private TextField textFieldDiff;
  private TextArea textAreaManagedFolders;
  private Button buttonUpdate;
  private Button buttonExit;

  private TextArea console;
  private String consoleText = "";

  private MCPackerWindow window;

  public MCPackerWindow() {
    window = this;

    setLayout(null);
    setResizable(false);
    setSize(700, 500);
    setTitle("MCPacker - version " + Version.versionCode);

    labelInput = new Label("Input:");
    labelInput.setBounds(5, 25, 50, 25);
    add(labelInput);

    labelOutput = new Label("Output:");
    labelOutput.setBounds(5, 55, 50, 25);
    add(labelOutput);

    labelDiff = new Label("Diff:");
    labelDiff.setBounds(5, 85, 50, 25);
    add(labelDiff);

    labelManagedFolders = new Label("Folders:");
    labelManagedFolders.setBounds(5, 115, 50, 25);
    add(labelManagedFolders);

    textFieldInput = new TextField();
    textFieldInput.setBounds(60, 25, this.getWidth() - 65, 25);
    textFieldInput.setText(MCPacker.config.input);
    add(textFieldInput);

    textFieldOutput = new TextField();
    textFieldOutput.setBounds(60, 55, this.getWidth() - 65, 25);
    textFieldOutput.setText(MCPacker.config.output);
    add(textFieldOutput);

    textFieldDiff = new TextField();
    textFieldDiff.setBounds(60, 85, this.getWidth() - 65, 25);
    textFieldDiff.setText(MCPacker.config.diff);
    add(textFieldDiff);

    StringBuilder stringBuilder = new StringBuilder();
    for (String managedFolder : MCPacker.config.managedFolders) {
      stringBuilder.append(managedFolder);
      stringBuilder.append("\n");
    }
    textAreaManagedFolders = new TextArea(stringBuilder.toString(), 0, 0);
    textAreaManagedFolders.setBounds(60, 115, this.getWidth() - 65, 85);
    add(textAreaManagedFolders);

    labelStatus = new Label("", Label.CENTER);
    labelStatus.setBounds(60, 205, this.getWidth() - 120, 25);
    add(labelStatus);

    buttonUpdate = new Button("Update");
    buttonUpdate.setBounds(this.getWidth() - 55, 205, 50, 25);
    buttonUpdate.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        consoleText = "";
        MCPacker.log("Updating");
        setStatus("Updating");

        window.remove(buttonExit);

        updateConfig();

        new MCPackerUpdater(MCPacker.config.copy()).start();
      }
    });
    add(buttonUpdate);

    buttonExit = new Button("Exit");
    buttonExit.setBounds(5, 205, 50, 25);
    buttonExit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        MCPacker.log("Exiting");

        updateConfig();

        System.exit(0);
      }
    });
    add(buttonExit);

    console = new TextArea(consoleText, 0, 0);
    console.setBounds(5, 235, this.getWidth() - 10, this.getHeight() - 240);
    add(console);

    setVisible(true);
  }

  public void log(String str) {
    consoleText = consoleText + str + "\n";
    if (console != null) {
      console.setText(consoleText);
      console.setCaretPosition(consoleText.length());
    }
  }

  public void setStatus(String str) {
    MCPacker.log(str);
    if (labelStatus != null) labelStatus.setText(str);
  }

  public void addExit() {
    add(buttonExit);
  }

  private void updateConfig() {
    MCPacker.config.input = textFieldInput.getText();
    MCPacker.config.output = textFieldOutput.getText();
    MCPacker.config.diff = textFieldDiff.getText();
    MCPacker.config.managedFolders = textAreaManagedFolders.getText().split("\n");
    MCPacker.config.write();
  }
}
