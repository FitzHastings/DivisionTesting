package division.swing.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;

public class RowHeaderCellRenderer extends JLabel implements ListCellRenderer {
  private Font simpleFont,selectFont;
  private Color simpleBackground, selectBackground, foregroundColor;
  private JTable table;

  public RowHeaderCellRenderer(final JList list, JTable table) {
    this.table = table;
    setOpaque(true);
    setHorizontalAlignment(LEFT);

    if(table.getTableHeader() != null) {
      simpleBackground = table.getTableHeader().getBackground();
      simpleFont = table.getTableHeader().getFont();
      foregroundColor = table.getTableHeader().getForeground();
    }else {
      simpleBackground = list.getBackground();
      simpleFont = list.getFont();
      foregroundColor = list.getForeground();
    }
    selectBackground = new Color(150, 150, 255);
    selectFont = simpleFont;//;new Font(simpleFont.getFontName(), Font.BOLD, simpleFont.getSize());

    MouseAdapter adapter = new MouseAdapter() {
      private int startindex,endIndex;
      @Override
      public void mousePressed(MouseEvent e) {
        startindex = endIndex = list.locationToIndex(e.getPoint());
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        endIndex = list.locationToIndex(e.getPoint());
        list.setSelectionInterval(startindex, endIndex);
      }
    };

    list.addMouseListener(adapter);
    list.addMouseMotionListener(adapter);
    list.setSelectionModel(table.getSelectionModel());
  }

  @Override
  public Component getListCellRendererComponent(JList list, Object value,
      int index, boolean isSelected, boolean cellHasFocus) {

    setText((value == null) ? "" : value.toString());

    setBackground(simpleBackground);
    
    if(isSelected) {
      setFont(selectFont);
      setBackground(selectBackground);
      setBorder(BorderFactory.createMatteBorder(index==0?1:0, 1, 1, 1, Color.GRAY));
    }else {
      setBorder(BorderFactory.createMatteBorder(index==0?1:0, 1, 1, 1, Color.BLACK));
      setForeground(foregroundColor);
      setFont(simpleFont);
    }
    return this;
  }
}