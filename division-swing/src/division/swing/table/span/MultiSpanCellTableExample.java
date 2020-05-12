package division.swing.table.span;
 
import division.swing.DivisionScrollPane;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * @version 1.0 11/26/98
 */
public class MultiSpanCellTableExample extends JFrame {

  MultiSpanCellTableExample() {
    super( "Multi-Span Cell Example" );
    
    AttributiveCellTableModel ml = new AttributiveCellTableModel(10, 10);
    MultiSpanCellTable bottomTable = new MultiSpanCellTable(ml);
    JScrollPane bottomScroll = new JScrollPane(bottomTable);
    
    /*AttributiveCellTableModel ml = new AttributiveCellTableModel(10,6);
    final CellSpan cellAtt = (CellSpan)ml.getCellAttribute();
    final MultiSpanCellTable table = new MultiSpanCellTable(ml);
    JScrollPane scroll = new JScrollPane( table );*/

    JButton b_one   = new JButton("Combine");
    /*b_one.addActionListener((ActionEvent e) -> {
      int[] columns = table.getSelectedColumns();
      int[] rows    = table.getSelectedRows();
      cellAtt.combine(rows,columns);
      table.clearSelection();
      table.revalidate();
      table.repaint();
    });*/
    
    JButton b_split = new JButton("Split");
    /*b_split.addActionListener((ActionEvent e) -> {
      int column = table.getSelectedColumn();
      int row    = table.getSelectedRow();
      cellAtt.split(row,column);
      table.clearSelection();
      table.revalidate();
      table.repaint();
    });*/
    
    JPanel p_buttons = new JPanel();
    p_buttons.setLayout(new GridLayout(2,1));
    p_buttons.add(b_one);
    p_buttons.add(b_split);

    Box box = new Box(BoxLayout.X_AXIS);
    box.add(bottomScroll);
    box.add(new JSeparator(SwingConstants.HORIZONTAL));
    box.add(p_buttons);
    getContentPane().add( box );
    setSize( 400, 200 );
    setVisible(true);
  }

  public static void main(String[] args) {
    MultiSpanCellTableExample frame = new MultiSpanCellTableExample();
    frame.addWindowListener( new WindowAdapter() {
      @Override
      public void windowClosing( WindowEvent e ) {
        System.exit(0);
      }
    });
  }
}
