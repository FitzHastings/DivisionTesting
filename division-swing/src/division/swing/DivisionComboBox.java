package division.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import mapping.MappingObject;
import mapping.MappingObject.Type;

/**
 * Отличается от родителя тем, что  PreferedSize как у JTextField
 * @athor Платонов Р.А.
 */
public class DivisionComboBox extends JComboBox implements FocusListener, ComponentListener {
  private List hideItems = new ArrayList();
  
  private JTextComponent component;
  private String emptyText = null;
  private Color  emptyColor        = Color.LIGHT_GRAY;
  private Color  defaultForeground = getForeground();
  
  public DivisionComboBox() {
    this(new Object[0], null);
  }
  
  public DivisionComboBox(String emptyText) {
    this(new Object[0], emptyText);
  }
  
  
  public DivisionComboBox(Object... items) {
    this(items, null);
  }
  
  public DivisionComboBox(Object[] items, String emptyText) {
    super(items);
    this.emptyText = emptyText;
    
    setRenderer(new MyRenderer());
    setEditor(new MyEditor());
    
    component = (JTextComponent) getEditor().getEditorComponent();
    component.addFocusListener(this);
    addComponentListener(this);
    
    Dimension pref = getPreferredSize();
    Dimension max = getMaximumSize();
    Dimension min = getMinimumSize();
    setMaximumSize(new Dimension(max.width,20));
    setMinimumSize(new Dimension(min.width,20));
    setPreferredSize(new Dimension(pref.width,20));
  }

  @Override
  public void componentHidden(ComponentEvent e) {
  }

  @Override
  public void componentMoved(ComponentEvent e) {
  }

  @Override
  public void componentResized(ComponentEvent e) {
    focusLost(null);
  }

  @Override
  public void componentShown(ComponentEvent e) {
  }
  
  @Override
  public void focusGained(FocusEvent e) {
    if(emptyText != null && component.getText().equals(emptyText)) {
      component.setForeground(defaultForeground);
      component.setText("");
    }
  }

  @Override
  public void focusLost(FocusEvent e) {
    if(emptyText != null && component.getText().equals("")) {
      defaultForeground = getForeground();
      component.setForeground(emptyColor);
      component.setText(emptyText);
      hidePopup();
    }
  }
  
  
  public void hideItems(Integer[] indexes) {
    for(Integer index:indexes)
      hideItem(index);
  }
  
  public void hideItem(Integer index) {
    hideItems.add(getItemAt(index));
    removeItemAt(index);
  }
  
  public void hideItem(Object[] objs) {
    for(Object o:objs)
      hideItem(o);
  }
  
  public void hideItem(Object o) {
    hideItems.add(o);
    removeItem(o);
  }
  
  public void showAllItems() {
    addItems(hideItems.toArray());
  }

  public void clear() {
    hideItems.clear();
    this.removeAllItems();
  }

  public Object[] getItems() {
    Object[] items = new Object[getItemCount()];
    for(int i=0;i<items.length;i++)
      items[i] = getItemAt(i);
    return items;
  }

  @Override
  public void addItem(Object o) {
    if(o instanceof MappingObject && getItemCount() > 0) {
      for(int i=0;i<getItemCount();i++) {
        Object obj = getItemAt(i);
        if(obj instanceof MappingObject && obj.equals(o))
          return;
      }
      super.addItem(o);
    }else super.addItem(o);
  }

  public void addItems(Object[] items) {
    for(Object o:items)
      addItem(o);
  }
  
  public void addItems(MappingObject[] items, Class<? extends DivisionItem> itemClass) throws InstantiationException, IllegalAccessException, NoSuchMethodException, RemoteException {
    for(MappingObject o:items) {
      DivisionItem item = itemClass.newInstance();
      item.setObject(o);
      addItem(item);
    }
  }

  @Override
  public void setSelectedItem(Object obj) {
    if(obj instanceof Integer)
      setSelectedItem((Integer)obj);
    else {
      if(obj instanceof MappingObject && getItems()[0] instanceof DivisionItem) {
        try {
          obj = new DivisionItem((MappingObject)obj);
        }catch(Exception ex) {}
      }
      super.setSelectedItem(obj);
    }
  }
  
  public void setSelectedItem(Integer id) {
    if(id == null)
      setSelectedIndex(-1);
    else {
      for(Object item:getItems()) {
        if(item instanceof DivisionItem && ((DivisionItem)item).getId().equals(id))
          setSelectedItem(item);
      }
    }
  }

  /*@Override
  public Object getSelectedItem() {
    Object o = super.getSelectedItem();
    if(o instanceof DivisionItem)
      return ((DivisionItem)o).getObject();
    else return o;
  }*/

  @Override
  public void removeItem(Object o) {
    try {
      if(o instanceof Integer) {
        for(int i=0;i<getItemCount();i++) {
          Object obj = getItemAt(i);
          if(obj instanceof MappingObject && ((MappingObject)obj).getId().intValue() == ((Integer)o).intValue())
            removeItemAt(i);
          else if(obj instanceof DivisionItem) {
            if(((DivisionItem)obj).getId().intValue() == ((Integer)o).intValue())
              removeItemAt(i);
          }
        }
      }else super.removeItem(o);
    }catch(RemoteException ex) {
    }
  }

  public void removeItems(Object[] objects) {
    for(Object o:objects)
      removeItem(o);
  }

  class MyEditor implements ComboBoxEditor {
    private Object item;
    private JTextField comp = new JTextField();
    private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
    private boolean popup = true;
    private boolean setText = true;
    private int searchPosition = 0;

    public MyEditor() {
      comp.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if(e.getKeyCode() == KeyEvent.VK_F3) {
            searchPosition++;
            search();
          }
        }
      });

      comp.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
          search();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
          searchPosition = 0;
          search();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
          search();
        }
      });
    }

    private void search() {
      if(popup) {
        if(!DivisionComboBox.this.isPopupVisible()) {
          searchPosition = 0;
          if(DivisionComboBox.this.isShowing())
            DivisionComboBox.this.showPopup();
        }
        DefaultComboBoxModel model = (DefaultComboBoxModel)DivisionComboBox.this.getModel();
        String text = comp.getText();
        for(int i=searchPosition;i<model.getSize();i++) {
          if(model.getElementAt(i).toString().indexOf(text) != -1) {
            searchPosition = i;
            setText = false;
            DivisionComboBox.this.setSelectedIndex(i);
            setText = true;
            break;
          }
          if(i == model.getSize()-1)
            searchPosition = 0;
        }
      }
    }

    @Override
    public Component getEditorComponent() {
      return this.comp;
    }

    @Override
    public void setItem(Object item) {
      this.item = item;
      if(item != null) {
        popup = false;
        if(setText) {
          if(item instanceof MappingObject)
            try {
              this.comp.setText(((MappingObject)this.item).getName());
            }catch(RemoteException ex) {System.out.println(ex.getMessage());}
          else this.comp.setText(this.item.toString());
        }
        popup = true;
      }
    }

    @Override
    public Object getItem() {
      return this.item;
    }

    @Override
    public void selectAll() {
      this.comp.select(0, comp.getText().length());
    }

    @Override
    public void addActionListener(ActionListener l) {
      if(!listeners.contains(l))
        listeners.add(l);
    }

    @Override
    public void removeActionListener(ActionListener l) {
      listeners.remove(l);
    }
  }

  

  class MyRenderer extends DefaultListCellRenderer {
    private final Border SAFE_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);
    
    @Override
    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
      Component component = this;
      setComponentOrientation(list.getComponentOrientation());

      Color bg = null;
      Color fg = null;

      JList.DropLocation dropLocation = list.getDropLocation();
      if (dropLocation != null
              && !dropLocation.isInsert()
              && dropLocation.getIndex() == index) {

        bg = UIManager.getColor("List.dropCellBackground");
        fg = UIManager.getColor("List.dropCellForeground");

        isSelected = true;
      }

      if(isSelected) {
        setBackground(bg == null ? list.getSelectionBackground() : bg);
        setForeground(fg == null ? list.getSelectionForeground() : fg);
      }else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }
      
      if(value instanceof Icon) {
        setIcon((Icon)value);
        setText("");
      }else if(value instanceof MappingObject) {
        try {
          setIcon(null);
          setText((value == null) ? "" : ((MappingObject)value).getName()+(((MappingObject)value).getType()==Type.ARCHIVE?" (архив)":""));
        }catch(RemoteException ex){ex.printStackTrace();}
      }else {
        setIcon(null);
        setText((value == null) ? "" : value.toString());
      }

      setEnabled(list.isEnabled());
      setFont(list.getFont());

      Border border = null;
      if(cellHasFocus) {
        if(isSelected)
          border = UIManager.getBorder("List.focusSelectedCellHighlightBorder");
        if(border == null)
          border = UIManager.getBorder("List.focusCellHighlightBorder");
      }else border = getNoFocusBorder();
      setBorder(border);
    	return component;
    }
    
    private Border getNoFocusBorder() {
      if (System.getSecurityManager() != null) {
          return SAFE_NO_FOCUS_BORDER;
      } else {
          return noFocusBorder;
      }
    }
  }
}