package bum.editors.util;

import java.awt.Color;
import java.util.TreeMap;
import javax.swing.JTable;

public class RowIndexColorController extends IDColorController {
  public TreeMap<Integer, Color> getColors() {
    return colors;
  }

  @Override
  public Color getCellColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasfocus) {
    if(colors.containsKey(modelRow))
      return colors.get(modelRow);
    return null;
  }
}