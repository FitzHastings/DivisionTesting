package division.swing.table;

import javax.swing.JTable;

public interface CellEditableController {
  public boolean isCellEditable(JTable table, int modelRow, int modelColumn);
}