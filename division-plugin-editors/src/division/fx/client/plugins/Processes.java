package division.fx.client.plugins;

import bum.interfaces.Service;
import division.fx.controller.process.FXProcess;
import division.fx.editor.FXTreeEditor;

public class Processes extends SimplePlugin {
  public Processes() {
    super(new FXTreeEditor(Service.class, FXProcess.class));
  }
}