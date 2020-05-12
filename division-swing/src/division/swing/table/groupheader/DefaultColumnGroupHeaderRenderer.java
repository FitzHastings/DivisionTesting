package division.swing.table.groupheader;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

public class DefaultColumnGroupHeaderRenderer extends DefaultTableCellRenderer {

  public DefaultColumnGroupHeaderRenderer() {
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,  int row, int column) {
    JTableHeader tableHeader = table.getTableHeader();
    if(tableHeader != null) {
      setForeground(tableHeader.getForeground());
      setBackground(tableHeader.getBackground());
      setFont(tableHeader.getFont());
    }
    setHorizontalAlignment(JLabel.CENTER);
    setText(value != null ? value.toString() : "");
    setBorder(UIManager.getBorder("TableHeader.cellBorder"));
    return this;
  }
}