package division.swing.table;

import javax.swing.JTable;

public interface CellAligmentController {
  public Integer getCellAligment(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus);
}