package division.swing;

import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

public class TableEditorAdapter implements CellEditorListener {
  public void start(int row, int column, Object editValue) {
  }

  @Override
  public void editingStopped(ChangeEvent e) {
  }

  @Override
  public void editingCanceled(ChangeEvent e) {
  }
}