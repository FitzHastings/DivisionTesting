package division.swing.table;

import java.awt.Font;
import javax.swing.JTable;

public interface CellFontController {
  public Font getCellFont(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus);
}