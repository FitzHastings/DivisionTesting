package bum.editors.util;

import division.swing.table.CellColorController;
import java.awt.Color;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.swing.JTable;

public class IDColorController implements CellColorController {
  protected TreeMap<Integer, Color> colors = new TreeMap<>();
  protected Entry<Integer, Color> dropRowColor;

  public void setDropRowColor(int row, Color color) {
    dropRowColor = new SimpleEntry<>(row, color);
  }

  public void clearRowColors() {
    colors.clear();
  }

  public void removeDropRowColor() {
    dropRowColor = null;
  }

  public void addRowColor(Integer id, Color color) {
    colors.put(id, color);
  }

  public void removeRowColor(Integer id) {
    colors.remove(id);
  }

  @Override
  public Color getCellColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasfocus) {
    Object object = table.getModel().getValueAt(modelRow, 0);
    if(object instanceof Integer && colors.containsKey((Integer)object))
      return colors.get((Integer)object);
    if(dropRowColor != null && dropRowColor.getKey().intValue() == modelRow)
      return dropRowColor.getValue();
    return null;
  }
}