package bum.editors;

import java.awt.LayoutManager;
import javax.swing.JPanel;

public class EditorGuiPanel extends JPanel {
  private EditorGui editor;

  public EditorGuiPanel(EditorGui editor, LayoutManager layout) {
    super(layout);
    this.editor = editor;
  }
  
  public EditorGui getEditor() {
    return editor;
  }
}