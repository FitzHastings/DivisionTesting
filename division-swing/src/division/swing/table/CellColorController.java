package division.swing.table;

import java.awt.Color;
import javax.swing.JTable;

public interface CellColorController {
  public default Color getCellColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
    return null;
  }
  
  public default Color getCellTextColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
    return null;
  }
}
