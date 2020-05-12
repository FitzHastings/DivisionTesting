package division.swing.TreeTable;

import division.swing.TableSelectionListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.*;

public class TreeTable extends JTable {
  protected TreeTableCellRenderer tree;
  private boolean treeEditable = true;
  private boolean showsIcons   = true;
  
  private final ArrayList<TableSelectionListener> listeners = new ArrayList<>();
  private int[] lastSelection = new int[]{0};

  private final ArrayList<Integer> editingColumns = new ArrayList<>();
  
  public TreeTable(TreeTableModel treeTableModel) {
    super();
    tree = new TreeTableCellRenderer(treeTableModel);
    super.setModel(new TreeTableModelAdapter(treeTableModel, tree));
    ListToTreeSelectionModelWrapper selectionWrapper = new ListToTreeSelectionModelWrapper();
    tree.setSelectionModel(selectionWrapper);
    setSelectionModel(selectionWrapper.getListSelectionModel());
    setDefaultRenderer(TreeTableModel.class, tree);
    setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());
    setShowGrid(false);
    setIntercellSpacing(new Dimension(0, 0));	
    if(tree.getRowHeight() < 1)
      setRowHeight(20);
  }

  public void setColumnEditable(int column, boolean isEditable) {
    if(isEditable) {
      if(!editingColumns.contains(Integer.valueOf(column)))
        editingColumns.add(Integer.valueOf(column));
    }else
      editingColumns.remove(Integer.valueOf(column));
  }

  @Override
  public boolean isCellEditable(int row, int column) {
    return column == 0 && !((DefaultMutableTreeNode)tree.getPathForRow(row).getLastPathComponent()).isRoot() ? true : (((DefaultMutableTreeNode)tree.getPathForRow(row).getLastPathComponent()).isRoot() ? false : editingColumns.contains(convertColumnIndexToModel(column)));
    /*if(convertColumnIndexToModel(column) != 0 && ((DefaultMutableTreeNode)tree.getPathForRow(row).getLastPathComponent()).isRoot())
      return false;
    return super.isCellEditable(row, column);*/
  }


  
  public void addTableSelectionListener(TableSelectionListener tableSelectionListener) {
    listeners.add(tableSelectionListener);
  }

  public void removeTableSelectionListener(TableSelectionListener tableSelectionListener) {
    listeners.remove(tableSelectionListener);
  }

  private void fireTableSelectionChange() {
    if(getSelectedRow() != -1) {
      for(TableSelectionListener tableSelectionListener:listeners)
        tableSelectionListener.TableSelectionChanged(lastSelection,getSelectedRows());
      lastSelection = getSelectedRows();
    }
  }
  
  @Override
  public void valueChanged(ListSelectionEvent e) {
    super.valueChanged(e);
    if(!e.getValueIsAdjusting())fireTableSelectionChange();
  }

  public boolean getTreeEditable() {
    return treeEditable;
  }

  public void setTreeEditable(boolean editable) {
    treeEditable = editable;
  }

  public boolean getShowsIcons() {
    return showsIcons;
  }

  public void setShowsIcons(boolean show) {
    showsIcons = show;
  }

  public void setRootVisible(boolean visible) {
    tree.setRootVisible(visible);
  }

  public boolean getShowsRootHandles() {
    return tree.getShowsRootHandles();
  }

  public void setShowsRootHandles(boolean newValue) {
    tree.setShowsRootHandles(newValue);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if(tree != null) {
      tree.updateUI();
      setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());
    }
    LookAndFeel.installColorsAndFont(this, "Tree.background", "Tree.foreground", "Tree.font");
  }

  @Override
  public int getEditingRow() {
    return (getColumnClass(editingColumn) == TreeTableModel.class) ? -1 : editingRow;  
  }

  private int realEditingRow() {
    return editingRow;
  }

  @Override
  public void sizeColumnsToFit(int resizingColumn) {
    super.sizeColumnsToFit(resizingColumn);
    if(getEditingColumn() != -1 && getColumnClass(editingColumn) == TreeTableModel.class) {
      Rectangle cellRect = getCellRect(realEditingRow(), getEditingColumn(), false);
      Component component = getEditorComponent();
      component.setBounds(cellRect);
      component.validate();
    }
  }

  @Override
  public void setRowHeight(int rowHeight) {
    super.setRowHeight(rowHeight);
    if(tree != null && tree.getRowHeight() != rowHeight)
        tree.setRowHeight(getRowHeight()); 
  }

  public JTree getTree() {
    return tree;
  }

  @Override
  public boolean editCellAt(int row, int column, EventObject e) {
    boolean retValue = super.editCellAt(row, column, e);
    if(retValue && getColumnClass(column) == TreeTableModel.class)
      repaint(getCellRect(row, column, false));
    return retValue;
  }
  
  class JTreeTableCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Icon getClosedIcon()        { return (showsIcons ? super.getClosedIcon()        : null); }
    @Override
    public Icon getDefaultClosedIcon() { return (showsIcons ? super.getDefaultClosedIcon() : null); }
    @Override
    public Icon getDefaultLeafIcon()   { return (showsIcons ? super.getDefaultLeafIcon()   : null); }
    @Override
    public Icon getDefaultOpenIcon()   { return (showsIcons ? super.getDefaultOpenIcon()   : null); }
    @Override
    public Icon getLeafIcon()          { return (showsIcons ? super.getLeafIcon()          : null); }
    @Override
    public Icon getOpenIcon()          { return (showsIcons ? super.getOpenIcon()          : null); }
  }
  
  public class TreeTableCellRenderer extends JTree implements TableCellRenderer {
    protected int visibleRow;
    protected Border highlightBorder;
    private Font font;

    public TreeTableCellRenderer(TreeModel model) {
      super(model);
      font = getFont();
      setCellRenderer(new JTreeTableCellRenderer());
    }
    
    @Override
    public void updateUI() {
      super.updateUI();
      TreeCellRenderer tcr = getCellRenderer();
      if(tcr instanceof DefaultTreeCellRenderer) {
        DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer)tcr); 
        dtcr.setTextSelectionColor(UIManager.getColor("Table.selectionForeground"));
        dtcr.setBackgroundSelectionColor(UIManager.getColor("Table.selectionBackground"));
      }
    }
    
    @Override
    public void setRowHeight(int rowHeight) { 
      if(rowHeight > 0) {
        super.setRowHeight(rowHeight); 
        if(TreeTable.this != null && TreeTable.this.getRowHeight() != rowHeight)
          TreeTable.this.setRowHeight(getRowHeight()); 
      }
    }
    
    @Override
    public void setBounds(int x, int y, int w, int h) {
      super.setBounds(x, 0, w, TreeTable.this.getHeight());
    }
    
    @Override
    public void paint(Graphics g) {
      g.translate(0, -visibleRow * getRowHeight());
      super.paint(g);
      if(highlightBorder != null)
        highlightBorder.paintBorder(this, g, 0, visibleRow * getRowHeight(), getWidth(), getRowHeight());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Color background;
      Color foreground;

      TreePath path = tree.getPathForRow(row);
      if(path == null)
        return null;

      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();

      if(isSelected) {
        background = table.getSelectionBackground();
        foreground = table.getSelectionForeground();
      }else {
        background = table.getBackground();
        foreground = table.getForeground();
      }
      highlightBorder = null;
      if(realEditingRow() == row && getEditingColumn() == column) {
        background = UIManager.getColor("Table.focusCellBackground");
        foreground = UIManager.getColor("Table.focusCellForeground");
      }else if(hasFocus) {
        highlightBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
        if(isCellEditable(row, column)) {
          background = UIManager.getColor("Table.focusCellBackground");
          foreground = UIManager.getColor("Table.focusCellForeground");
        }
      }
      
      visibleRow = row;
      setBackground(background);
      
      TreeCellRenderer tcr = getCellRenderer();
      if(tcr instanceof DefaultTreeCellRenderer) {
        DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer)tcr); 
        if(isSelected) {
          dtcr.setTextSelectionColor(foreground);
          dtcr.setBackgroundSelectionColor(background);
        }else {
          dtcr.setTextNonSelectionColor(foreground);
          dtcr.setBackgroundNonSelectionColor(background);
        }
      }

      if(node instanceof SwingObject) {
        if(((SwingObject)node).getFont() != null)
          this.setFont(((SwingObject)node).getFont());
        else this.setFont(font);
      }
      return this;
    }
  }
  
  public class TreeTableCellEditor extends DefaultCellEditor {
    public TreeTableCellEditor() {
      super(new TreeTableTextField());
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int r, int c) {
      Component component = super.getTableCellEditorComponent(table, value, isSelected, r, c);
      boolean rv = getTree().isRootVisible();
      int offsetRow = rv ? r : r - 1;
      Rectangle bounds = getTree().getRowBounds(offsetRow);
      int offset = bounds.x;
      TreeCellRenderer tcr = getTree().getCellRenderer();
      if(tcr instanceof DefaultTreeCellRenderer) {
        Object node = getTree().getPathForRow(offsetRow).getLastPathComponent();
        Icon icon;
        if(getTree().getModel().isLeaf(node))
          icon = ((DefaultTreeCellRenderer)tcr).getLeafIcon();
        else if(tree.isExpanded(offsetRow))
          icon = ((DefaultTreeCellRenderer)tcr).getOpenIcon();
        else icon = ((DefaultTreeCellRenderer)tcr).getClosedIcon();
        
        if(icon != null)
          offset += ((DefaultTreeCellRenderer)tcr).getIconTextGap() + icon.getIconWidth();
      }
      ((TreeTableTextField)getComponent()).offset = offset;
      return component;
    }

    @Override
    public boolean isCellEditable(EventObject e)
    {
      if(e instanceof MouseEvent)
      {
        MouseEvent me = (MouseEvent)e;
        if(me.getModifiers() == 0 || me.getModifiers() == InputEvent.BUTTON1_MASK)
        {
          for(int counter = getColumnCount() - 1; counter >= 0; counter--)
          {
            if(getColumnClass(counter) == TreeTableModel.class)
            {
              MouseEvent newME = new MouseEvent(
                      TreeTable.this.tree, me.getID(),
                      me.getWhen(), me.getModifiers(),
                      me.getX() - getCellRect(0, counter, true).x,
                      me.getY(), me.getClickCount(),
                      me.isPopupTrigger());
              TreeTable.this.tree.dispatchEvent(newME);
              break;
            }
          }
        }
        if(me.getClickCount() >= 3)
          return treeEditable;
        return false;
      }
      if(e == null)
        return treeEditable;
      return false;
    }
  }


  static class TreeTableTextField extends JTextField
  {
    public int offset;

    @SuppressWarnings("deprecation")
    @Override
    public void reshape(int x, int y, int w, int h)
    {
      int newX = Math.max(x, offset);
      super.reshape(newX, y, w - (newX - x), h);
    }
  }


  class ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel
  {
    protected boolean updatingListSelectionModel;

    public ListToTreeSelectionModelWrapper()
    {
      super();
      getListSelectionModel().addListSelectionListener(createListSelectionListener());
    }

    ListSelectionModel getListSelectionModel()
    {
      return listSelectionModel; 
    }

    @Override
    public void resetRowSelection()
    {
      if(!updatingListSelectionModel)
      {
        updatingListSelectionModel = true;
        try{super.resetRowSelection();}
        finally{updatingListSelectionModel = false;}
      }
    }

    protected ListSelectionListener createListSelectionListener()
    {
      return new ListSelectionHandler();
    }

    protected void updateSelectedPathsFromSelectedRows()
    {
      if(!updatingListSelectionModel)
      {
        updatingListSelectionModel = true;
        try
        {
          int min = listSelectionModel.getMinSelectionIndex();
          int max = listSelectionModel.getMaxSelectionIndex();

          clearSelection();
          if(min != -1 && max != -1)
          {
            for(int counter = min; counter <= max; counter++)
            {
              if(listSelectionModel.isSelectedIndex(counter))
              {
                  TreePath selPath = tree.getPathForRow(counter);
                  if(selPath != null)
                    addSelectionPath(selPath);
              }
            }
          }
        }
        finally{updatingListSelectionModel = false;}
      }
    }

    class ListSelectionHandler implements ListSelectionListener {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        updateSelectedPathsFromSelectedRows();
      }
    }
  }
}
