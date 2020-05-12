package division.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import org.apache.commons.lang3.ArrayUtils;

public class bum_Calendar extends JPanel {
  private JButton ok = new JButton("ok");
  private DivisionTable table = new DivisionTable();
  private JScrollPane scroll = new JScrollPane(table);

  private TableRenderer renderer = new TableRenderer();

  private ArrayList listeners = new ArrayList();
  private ArrayList ListenersAllwaysAction = new ArrayList();

  private String[] months = {
                              "январь","февраль","март","апрель","май","июнь",
                              "июль","август","сентябрь","октябрь","ноябрь","декабрь"
                             };
  private int currentMonth;
  private int currentYear;

  private Calendar cp = Calendar.getInstance();
  private boolean active = true;
  private JSpinner mm = new JSpinner(new CircularSpinnerModel(months));
  private JSpinner yy = new JSpinner(new SpinnerNumberModel(cp.get(Calendar.YEAR),Integer.MIN_VALUE,Integer.MAX_VALUE,1));

  private DistanceList distances = new DistanceList();

  private long lastDateClick;
  
  private List<Date> dates;
  
  public enum Type {BEFORE,AFTER}
  private Date xDate = null;
  private Type type;
  
  public bum_Calendar() {
    this((Date)null, null);
  }

  public bum_Calendar(List<Date> dates) {
    super(new GridBagLayout());
    this.dates = dates;
    initComponents();
    initEvents();
    cp.setTime(new Date());
    set(cp.get(Calendar.MONTH),cp.get(Calendar.YEAR));
    setOkVisible(false);
  }
  
  public bum_Calendar(Date beforeDate, Type type) {
    super(new GridBagLayout());
    
    if(beforeDate != null && type != null) {
      cp.setTime(beforeDate);
      cp.set(Calendar.HOUR_OF_DAY, 0);
      cp.set(Calendar.MINUTE, 0);
      cp.set(Calendar.SECOND, 0);
      cp.set(Calendar.MILLISECOND, 0);

      this.xDate = cp.getTime();
      this.type = type;
    }
    
    initComponents();
    initEvents();
    cp.setTime(new Date());
    set(cp.get(Calendar.MONTH),cp.get(Calendar.YEAR));
    setOkVisible(false);
  }

  public void setOkVisible(boolean visible) {
    ok.setVisible(visible);
  }

  public boolean isOkVisible() {
    return ok.isVisible();
  }

  public int getMonth() {
    return getMonth(mm.getValue().toString());
  }

  public int getYear() {
    return Integer.parseInt(yy.getValue().toString());
  }


  public Date getDate() {
    return getDate(false);
  }
  
  public Date getDate(boolean ignoreEnabled) {
    return distances.isEmpty()||(!isEnabled() && !ignoreEnabled)?null:distances.get(0).getStartDate();
  }

  public DistanceList getDates() {
    return distances;
  }

  public Date getDate(int year, int month, int day) {
    cp.set(year, month, day, 0, 0, 0);
    cp.set(Calendar.MILLISECOND, 0);
    return cp.getTime();
  }

  public void setDateInCalendar(int dd,int mm,int yyyy, boolean activeAction) {
    cp.set(yyyy, mm, dd);
    setDateInCalendar(cp.getTime(), activeAction);
  }

  public void clear() {
    distances.clear();
  }

  public void setDateInCalendar(Date date, boolean activeAction) {
    if(date == null)
      date = new Date();
    cp.setTime(date);
    cp.set(Calendar.HOUR_OF_DAY, 0);
    cp.set(Calendar.MINUTE, 0);
    cp.set(Calendar.SECOND, 0);
    cp.set(Calendar.MILLISECOND, 0);
    date = cp.getTime();

    distances.add(new DistanceTime(date.getTime(), date.getTime()+24*60*60*1000-1));
    lastDateClick = date.getTime();
    setEnabled(true);
    active = false;
    this.mm.setValue(months[cp.get(Calendar.MONTH)]);
    this.yy.setValue(Integer.valueOf(String.valueOf(cp.get((Calendar.YEAR)))));
    if(activeAction)
      active = true;
    sendToListeners(false);
    //EnterAndClosePopup();
    active = true;
    table.grabFocus();
  }
  
  
  public void setDateInCalendar(int dd,int mm,int yyyy) {
    setDateInCalendar(dd, mm, yyyy, true);
  }

  public void setDateInCalendar(java.sql.Date date) {
    setDateInCalendar(new Date(date == null?System.currentTimeMillis():date.getTime()));
  }
  
  public void setDateInCalendar(Date date) {
    setDateInCalendar(date, true);
  }
  
  public void setDistances(DistanceList distances) {
    this.distances = distances;
  }

  @Override
  public void setEnabled(boolean isEnabled) {
    super.setEnabled(isEnabled);
    for(int i=0;i<getComponentCount();i++)
      if(getComponent(i) instanceof JComponent)
        setEnabledComponent((JComponent)getComponent(i));
  }

  private void setEnabledComponent(JComponent com) {
    com.setEnabled(isEnabled());
    for(int i=0;i<com.getComponentCount();i++) {
      if(com.getComponent(i) instanceof JComponent)
        setEnabledComponent((JComponent)com.getComponent(i));
    }
  }

  /*private void addKeyListenerToComponent(JComponent comp) {
    comp.addKeyListener(keyAdapter);
    for(Component c:comp.getComponents())
      if(c instanceof JComponent)
        addKeyListenerToComponent((JComponent)c);
  }*/

  //boolean shift = false;
  //boolean ctrl  = false;

  /*public KeyAdapter keyAdapter = new KeyAdapter() {
    @Override
    public void keyPressed(KeyEvent e) {
      if(e.getKeyCode() == KeyEvent.VK_SHIFT)
        shift = true;
      if(e.getKeyCode() == KeyEvent.VK_CONTROL)
        ctrl = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
      if(e.getKeyCode() == KeyEvent.VK_SHIFT)
        shift = false;
      if(e.getKeyCode() == KeyEvent.VK_CONTROL)
        ctrl = false;
    }
  };*/

  @Override
  public boolean handleEvent(Event e) {
    return true;
  }

  private void initEvents() {
    addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        table.repaint();
      }
    });


    ok.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EnterAndClosePopup();
      }
    });
    
    //addKeyListenerToComponent(this);

    table.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        int row      = table.rowAtPoint(e.getPoint());
        int column   = table.columnAtPoint(e.getPoint());
        if(row != -1 && column != -1 && isEnabled()) {
          Date date = getDate(row, column);
          if(
                  dates == null && xDate == null || 
                  date != null && dates != null && dates.contains(date) || 
                  date != null && xDate != null && (
                    type == Type.BEFORE && date.getTime() <= xDate.getTime() ||
                    type == Type.AFTER && date.getTime() >= xDate.getTime()
                  )
                  ) {
            table.setRowSelectionInterval(row, row);
            table.setColumnSelectionInterval(column, column);
          }else table.clearSelection();
        }
      }
    });

    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2)
          EnterAndClosePopup();
      }

      @Override
      public void mousePressed(MouseEvent e) {
        int row      = table.rowAtPoint(e.getPoint());
        int column   = table.columnAtPoint(e.getPoint());
        Date date = getDate(row, column);
        if(
                (
                dates == null && xDate == null || 
                date != null && dates != null && dates.contains(date) || 
                date != null && xDate != null && (
                  type == Type.BEFORE && date.getTime() <= xDate.getTime() || 
                  type == Type.AFTER && date.getTime() >= xDate.getTime())
                ) && 
                bum_Calendar.this.isEnabled()
                ) {
          long dateTime = date.getTime();
          
          int shiftmask = MouseEvent.SHIFT_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;
          int ctrlmask  = MouseEvent.CTRL_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;
          boolean isCtrl  = (e.getModifiersEx() & ctrlmask) == ctrlmask;
          boolean isShift = (e.getModifiersEx() & shiftmask) == shiftmask;
          
          if(isCtrl) {
            DistanceTime distanceTime = distances.getDistanceTime(dateTime);
            if(distanceTime != null) {
              distances.remove(distanceTime);
              long start = distanceTime.getStart();
              long end   = dateTime-24*60*60*1000;
              if(start <= end)
                distances.add(new DistanceTime(start, end));
              start = dateTime+24*60*60*1000;
              end   = distanceTime.getEnd();
              if(start <= end)
                distances.add(new DistanceTime(start, end));
            }else {
              distances.add(new DistanceTime(dateTime, dateTime));
              lastDateClick = dateTime;
            }
          }else if(isShift) {
            DistanceTime distanceTime;
            if(dateTime > lastDateClick)
              distanceTime = new DistanceTime(lastDateClick, dateTime+24*60*60*1000-1);
            else distanceTime = new DistanceTime(dateTime, lastDateClick+24*60*60*1000-1);

            DistanceTime dt = distances.getDistanceTime(distanceTime);
            if(dt != null)
              distances.remove(dt);
            distances.add(distanceTime);
            distances.validate();
          }else {
            distances.clear();
            distances.add(new DistanceTime(dateTime, dateTime+24*60*60*1000-1));
            lastDateClick = dateTime;
          }
          table.repaint();
          sendToListeners(false);
        }
      }
    });

    mm.addChangeListener((ChangeEvent e) -> {
      if(getMonth() > currentMonth && getMonth() - currentMonth > 1)
        yy.setValue(getYear()-1);
      
      if(getMonth() < currentMonth && currentMonth - getMonth() > 1)
        yy.setValue(getYear()+1);
      
      set(getMonth(),getYear());
      sendToListeners(false);
    });

    yy.addChangeListener((ChangeEvent e) -> {
      set(getMonth(),getYear());
      sendToListeners(false);
    });
  }
  
  private Date getDate(int row, int column) {
    Object value = table.getValueAt(row, column);
    if(!"".equals(value) && bum_Calendar.this.isEnabled()) {
      int year   = Integer.parseInt(yy.getValue().toString());
      int month  = getMonth(mm.getValue().toString());
      int day    = Integer.parseInt(value.toString());
      return getDate(year, month, day);
    }
    return null;
  }

  public int getMonth(String m) {
    return ArrayUtils.indexOf(months, m);
  }

  public void EnterAndClosePopup() {
    sendToListeners(true);
    Container parent = getParent();
    while(parent != null) {
      if(parent instanceof JPopupMenu) {
        ((JPopupMenu)parent).setVisible(false);
        break;
      }
      parent = parent.getParent();
    }
  }
  
  private void set(int mm, int yy) {
    currentYear  = yy;
    currentMonth = mm;
    for(int i=0;i<table.getRowCount();i++)
      for(int j=0;j<table.getColumnCount();j++)
        table.setValueAt("",i,j);
    
    GregorianCalendar c = new GregorianCalendar();
    c.set(yy, mm, 1, 0, 0, 0);
    c.set(GregorianCalendar.MILLISECOND, 0);
    
    int leadGap = c.get(Calendar.DAY_OF_WEEK)-2;
    if(leadGap == -1)
      leadGap = 6;
    
    int row = 0;
    int col = leadGap;
    for(int i=1;i<=c.getActualMaximum(Calendar.DAY_OF_MONTH);i++) {
      if(col != 0 && col % 7 == 0) {
        row++;
        col = 0;
      }
      table.setValueAt(String.valueOf(i),row,col);
      col++;
    }
  }

  private void initComponents() {
    table.setFindable(false);
    table.setSelectionBackground(table.getBackground());
    table.setSelectionForeground(Color.BLACK);
    table.setShowGrid(false);
    table.setSortable(false);
    table.getTableHeader().setReorderingAllowed(false);
    table.getTableHeader().setResizingAllowed(false);
    table.getTableHeader().setDefaultRenderer(new Renderer());
    table.setColumnSelectionAllowed(true);
    table.setColumns(new Object[]{"Пн","Вт","Ср","Чт","Пт","Сб","Вс"});

    for(int i=0;i<table.getColumnCount();i++) {
      table.findTableColumn(i).setCellRenderer(renderer);
      table.repaint();
    }

    table.getTableModel().addRow(new Object[]{"","","","","","",""});
    table.getTableModel().addRow(new Object[]{"","","","","","",""});
    table.getTableModel().addRow(new Object[]{"","","","","","",""});
    table.getTableModel().addRow(new Object[]{"","","","","","",""});
    table.getTableModel().addRow(new Object[]{"","","","","","",""});
    table.getTableModel().addRow(new Object[]{"","","","","","",""});

    int h = table.getRowCount()*table.getRowHeight();
    scroll.setPreferredSize(new Dimension(200,h+25));
    scroll.getViewport().setBackground(table.getBackground());
    
    ((JSpinner.DefaultEditor)mm.getEditor()).getTextField().setBackground(Color.WHITE);
    mm.setPreferredSize(new Dimension(100, mm.getPreferredSize().height));
    mm.setMinimumSize(new Dimension(100, mm.getPreferredSize().height));

    add(mm,     new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    add(yy,     new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    add(scroll, new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 0, 5), 0, 0));
    add(ok,     new GridBagConstraints(0, 2, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
  }

  public void addToListenerAllwaysAction(CalendarChangeListener cl) {
    ListenersAllwaysAction.add(cl);
  }

  public void delFromListenerAllwaysAction(CalendarChangeListener cl) {
    ListenersAllwaysAction.remove(cl);
  }

  public void addToListener(CalendarChangeListener cl) {
    listeners.add(cl);
  }

  public void delFromListener(CalendarChangeListener cl) {
    listeners.remove(cl);
  }
  
  private void sendToListeners(boolean endSelect) {
    for(int i=0;i<ListenersAllwaysAction.size();i++) {
      ((CalendarChangeListener)ListenersAllwaysAction.get(i)).CalendarChangeDate(distances);
      if(endSelect)
        ((CalendarChangeListener)ListenersAllwaysAction.get(i)).CalendarSelectedDate(distances);
    }
    if(isEnabled() && active) {
      for(int i=0;i<listeners.size();i++) {
        ((CalendarChangeListener)listeners.get(i)).CalendarChangeDate(distances);
        if(endSelect)
          ((CalendarChangeListener)listeners.get(i)).CalendarSelectedDate(distances);
      }
    }
  }


  private class TableRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      JComponent com = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

      if(!bum_Calendar.this.isEnabled()) {
        com.setBorder(BorderFactory.createEmptyBorder());
        com.setBackground(Color.LIGHT_GRAY);
      }else {
        com.setBorder(BorderFactory.createEmptyBorder());
        com.setBackground(Color.WHITE);
        
        Date date = getDate(row, column);
        if(
                date != null && (
                dates == null || 
                dates.contains(date) || 
                xDate == null || 
                xDate.getTime() == date.getTime())) {
          int year   = Integer.parseInt(yy.getValue().toString());
          int month  = getMonth(mm.getValue().toString());
          int day    = Integer.parseInt(value.toString());
					if(distances.getDistanceTime(getDate(year, month, day).getTime()) != null) {
            com.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            com.setBackground(Color.YELLOW);
          }else {
            if(isSelected) {
              com.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            }
          }
        }
        
        if(
                date != null && (
                dates != null && !dates.contains(date) || 
                xDate != null && (type == Type.BEFORE && date.getTime() > xDate.getTime() || type == Type.AFTER && date.getTime() < xDate.getTime())
                ))
          com.setForeground(Color.GRAY);
        else com.setForeground(Color.BLACK);
      }
      return com;
    }
  }

  private class Renderer implements TableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      JLabel l = new JLabel((String)value);
      l.setFont(new Font("Dialog",Font.BOLD,12));

      l.setHorizontalTextPosition(JLabel.LEFT);
      l.setHorizontalAlignment(JLabel.LEFT);

      if(column == 5 || column == 6)
        l.setForeground(Color.red);
      else l.setForeground(Color.black);

      return l;
    }
  }
}
