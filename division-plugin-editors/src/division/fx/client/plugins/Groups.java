package division.fx.client.plugins;

import bum.interfaces.Group;
import division.fx.controller.group.FXGroup;
import division.fx.editor.FXTreeEditor;

public class Groups extends SimplePlugin {
  public Groups() {
    super(new FXTreeEditor(Group.class, FXGroup.class));
  }
}