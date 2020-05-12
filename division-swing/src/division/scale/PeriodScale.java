package division.scale;

import division.swing.guimessanger.Messanger;
import division.swing.DivisionTable;
import division.util.Utility;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import division.swing.table.groupheader.ColumnGroup;
import division.swing.table.groupheader.ColumnGroupHeader;
import org.apache.commons.lang3.ArrayUtils;

public class PeriodScale extends DivisionTable implements MouseListener, KeyListener, MouseMotionListener, DragGestureListener, DragSourceListener, DropTargetListener {
  private int dayWidth;
  private int previosMonthPeriod;
  private int nextMonthPeriod;
  private int DAYS_COUNT = 0;
  private int CURRENT_COLUMN;
  
  private int CURRENT_DAY;
  private int CURRENT_MONTH;
  private int CURRENT_YEAR;
  
  private long START_TIMESTAMP;
  private int START_MONTH;
  private int START_YEAR;
  
  private int END_MONTH;
  private int END_YEAR;
  
  private static long DAY_TIME = 24*60*60*1000;
  
  private Color cellSelectionColor = new Color(Color.BLUE.getRed(), Color.BLUE.getGreen(), Color.BLUE.getBlue(), 50);
  
  private Color currentDateColor = new Color(Color.BLACK.getRed(), Color.BLACK.getGreen(), Color.BLACK.getBlue(), 200);
  
  private Calendar          cp     = Calendar.getInstance();
  private ColumnGroupHeader header = new ColumnGroupHeader(this);
  private String[]          months = {"январь","февраль","март","апрель","май","июнь","июль","август","сентябрь","октябрь","ноябрь","декабрь"};
  
  private CopyOnWriteArrayList<ColorLabel> labels = new CopyOnWriteArrayList<>();
  private CopyOnWriteArrayList<CopyOnWriteArrayList<ObjectPeriod>> periods = new CopyOnWriteArrayList<>();
  
  private CopyOnWriteArrayList<ObjectPeriodScaleListener> listeners = new CopyOnWriteArrayList<>();
  private List<DivisionCellBox> selectedCellBoxes = new ArrayList<>();
  
  private PeriodDragger periodDragger = null;
  
  public PeriodScale(int previosMonthPeriod, int nextMonthPeriod, int dayWidth) {
    super();
    this.previosMonthPeriod = previosMonthPeriod;
    this.nextMonthPeriod = nextMonthPeriod;
    this.dayWidth = dayWidth;
    recalculate();
    initComponents();
    initEvents();
    
    DragSource dragSource = DragSource.getDefaultDragSource();
    dragSource.createDefaultDragGestureRecognizer(this,DnDConstants.ACTION_COPY_OR_MOVE,this);
    DropTarget dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE,this,true,null);
  }

  public int getPreviosMonthPeriod() {
    return previosMonthPeriod;
  }

  public int getNextMonthPeriod() {
    return nextMonthPeriod;
  }
  
  public PeriodScale() {
    this(1, 5, 3);
  }

  public PeriodDragger getPeriodDragger() {
    return periodDragger;
  }

  public void setPeriodDragger(PeriodDragger periodDragger) {
    this.periodDragger = periodDragger;
  }

  public Color getCurrentDateColor() {
    return currentDateColor;
  }

  public void setCurrentDateColor(Color currentDateColor) {
    this.currentDateColor = currentDateColor;
  }

  @Override
  public void paint(Graphics g) {
    Point p = MouseInfo.getPointerInfo().getLocation();
    SwingUtilities.convertPointFromScreen(p,this);
    Rectangle moveRectangle = getVisibleRect().contains(p)?getCellRect(rowAtPoint(p), columnAtPoint(p), true):null;
    
    Graphics2D g2 = (Graphics2D) g;
    
    g2.clearRect(0, 0, getWidth(), getHeight());
    g2.setColor(getBackground());
    g2.fillRect(0, 0, getWidth(), getHeight());
    g2.setColor(Color.LIGHT_GRAY);
    g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
    
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    
    g2.setColor(Color.GRAY);
    g2.setStroke(new BasicStroke(1));
    
    String dateString = "";
    if(moveRectangle != null)
      dateString = Utility.format(getDate(columnAtPoint(new Point(moveRectangle.x+1,moveRectangle.y+1))));
    
    
    for(DivisionCellBox cellBox:selectedCellBoxes) {
      g2.setColor(cellSelectionColor);
      g2.fill(cellBox.getBounds());
      g2.setColor(Color.LIGHT_GRAY);
      g2.draw(cellBox.getBounds());
    }
    
    for(int i=0;i<periods.size();i++) {
      for(ObjectPeriod period:periods.get(i)) {
        g2.setStroke(new BasicStroke(1));
        if(period.isVisible()) {
          boolean selected = isPeriodSelected(period);
          g2.setColor(new Color(period.getColor().getRed(),period.getColor().getGreen(),period.getColor().getBlue(),selected?255:50));
          g2.fill(period.getBounds());
          g2.setColor(selected?Color.BLACK:Color.GRAY);
          Rectangle rect = period.getBounds();
          g2.draw(period.getBounds());
          
          UserComment lastStopUserComment = period.getLastStopUserComment();
          boolean is = false;
          if(lastStopUserComment != null) {
            is = lastStopUserComment.getStopTime().getTime()+(24*60*60*1000) >= System.currentTimeMillis();
            if(is) {
              int x = rect.x+rect.width-rect.width/4;
              int y = rect.y+rect.height-3;
              g2.setColor(Color.BLACK);
              g2.drawLine(x, y, x, y-rect.height+6);
              y = y-rect.height+6;
              g2.setColor(Color.RED);
              g2.fillRect(x, y, 5, 5);
              g2.setColor(Color.BLACK);
              g2.drawRect(x, y, 5, 5);
            }
          }
          
          if(period.getUserCommentsCount() > 0 && !(period.getUserCommentsCount() == 1 && is)) {
            g2.setColor(Color.BLACK);
            int h = rect.height/2;
            int w = h>rect.width?rect.width:h;
            g2.drawPolygon(new int[]{rect.x,rect.x+w,rect.x}, new int[]{rect.y,rect.y,rect.y+h}, 3);
            g2.setColor(Color.LIGHT_GRAY);
            g2.fillPolygon(new int[]{rect.x+1,rect.x+w-1,rect.x+1}, new int[]{rect.y+1,rect.y+1,rect.y+h-1}, 3);
          }
        }
        if(moveRectangle != null && period.getComment() != null && !period.getComment().equals("") && period.getBounds().contains(moveRectangle)) {
          g2.setColor(Color.DARK_GRAY);
          
          int x = moveRectangle.x+moveRectangle.width+5;
          int y = moveRectangle.y+moveRectangle.height*2-2;
          if(y > getHeight()) {
            x += g2.getFontMetrics().stringWidth(dateString)+10;
            y = moveRectangle.y+moveRectangle.height-2;
          }
          g2.drawString(period.getComment(), x, y);
          
          x = moveRectangle.x+moveRectangle.width+5;
          y = moveRectangle.y+moveRectangle.height*3-2;
          String periodString = Utility.format(period.getStartDate())+" - "+Utility.format(period.getEndDate());
          if(y > getHeight()) {
            x = moveRectangle.x - g2.getFontMetrics().stringWidth(periodString)-10;
            y = moveRectangle.y+moveRectangle.height-2;
          }
          g2.drawString(periodString, x, y);
          
          g2.setStroke(new BasicStroke(2));
          g2.setColor(Color.DARK_GRAY);
          g2.draw(period.getBounds());
        }
      }
    }
    
    if(moveRectangle != null) {
      g2.setColor(Color.DARK_GRAY);
      g2.drawRect(0, moveRectangle.y, getWidth(), moveRectangle.height);
      g2.drawRect(moveRectangle.x, 0, moveRectangle.width, getHeight());
      g2.drawString(
              dateString, 
              moveRectangle.x+moveRectangle.width+5, moveRectangle.y+moveRectangle.height-2);
    }
    
    if(getRowCount() > 0) {
      Rectangle rect = getCellRect(0, getCurrentColumn(), true);
      g2.setColor(Color.BLACK);
      g2.drawRect(rect.x, 0, rect.width, getHeight());
      //g2.drawLine(rect.x+rect.width/2, 0, rect.x+rect.width/2, getHeight());
    }
  }
  
  private void initComponents() {
    setTableHeader(header);
    setGridColor(new Color(225, 225, 225));
    setSelectionBackground(getBackground());
    getTableHeader().setResizingAllowed(false);
    setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    setIntercellSpacing(new Dimension(0, 0));
    setColumnSelectionAllowed(true);
    
    final Component component = header.getDefaultRenderer().getTableCellRendererComponent(this, "header", false, false, 0, 0);
    final int height = component.getPreferredSize().height;
    header.setPreferredSize(new Dimension(header.getPreferredSize().width, height));
  }
  
  private void initEvents() {
    for(MouseListener l:getMouseListeners())
      removeMouseListener(l);
    
    addMouseListener(this);
    addMouseMotionListener(this);
    addKeyListener(this);
    getTableHeader().addKeyListener(this);
    
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e) {
        getParent().addKeyListener(PeriodScale.this);
      }
    });
    
    getTableHeader().addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        int shiftmask = MouseEvent.SHIFT_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;
        int ctrlmask  = MouseEvent.CTRL_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;
        boolean isCtrl  = (e.getModifiersEx() & ctrlmask) == ctrlmask;
        boolean isShift = (e.getModifiersEx() & shiftmask) == shiftmask;
        
        ColumnGroup cg = ((ColumnGroupHeader)getTableHeader()).getColumnGroup(columnAtPoint(e.getPoint()));
        int[] columns = new int[0];
        Enumeration em = cg.getColumns();
        while(em.hasMoreElements()) {
          columns = ArrayUtils.add(columns,((TableColumn)em.nextElement()).getModelIndex());
        }
        Arrays.sort(columns);
        if(!isCtrl) {
          if(isShift) {
            if(!selectedCellBoxes.isEmpty()) {
              DivisionCellBox startBox = selectedCellBoxes.get(0);
              int col = columns[0];
              if(startBox.getColumn0() > columns[columns.length-1]) {
                //columns[0] = columns[columns.length-1];
                columns[columns.length-1] = startBox.getColumn1();
              }else columns[0] = startBox.getColumn0();
            }
          }
          setColumnSelectionInterval(columns[0], columns[columns.length-1]);
        }else addColumnSelectionInterval(columns[0], columns[columns.length-1]);
        repaint();
      }
    });
    
    addMouseWheelListener((final MouseWheelEvent e) -> {
      boolean isCtrl  = (e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == MouseEvent.CTRL_DOWN_MASK;
      if (isCtrl) {
        if (e.getPreciseWheelRotation() < 0 && dayWidth+1 <= 10 || e.getPreciseWheelRotation() > 0 && dayWidth-1 >= 2) {
          Point p = e.getPoint();
          int column = columnAtPoint(p);
          int row = rowAtPoint(p);
          Rectangle visRect  = getVisibleRect();
          Rectangle cellRect = getCellRect(row, column, true);
          p = new Point(cellRect.x+cellRect.width/2, cellRect.y+cellRect.height/2);
          int dx = p.x-visRect.x;
          int dy = p.y-visRect.y;
          int x1;
          int y1;
          if(e.getPreciseWheelRotation() < 0) {
            setDayWidth(dayWidth+1);
            cellRect = getCellRect(row, column, true);
            cellRect.x += column;
          }else {
            setDayWidth(dayWidth-1);
            cellRect = getCellRect(row, column, true);
          }
          x1 = cellRect.x-dx+cellRect.width/2;
          y1 = cellRect.y-dy+cellRect.height/2;
          visRect = new Rectangle(x1, y1, visRect.width, visRect.height);
          scrollRectToVisible(visRect);
        }
      } else {
        Rectangle rect = getVisibleRect();
        if(e.getPreciseWheelRotation() > 0)
          rect.y += getRowHeight();
        else rect.y -= getRowHeight();
        scrollRectToVisible(rect);
      }
      repaint();
    });
  }
  
  private void reloadModel() {
    header.removeAll();
    
    int rowCount = getRowCount();
    setColumns(new Object[DAYS_COUNT]);
    for(int i=0;i<rowCount;i++)
      ((DefaultTableModel)getModel()).addRow(new Object[getColumnCount()]);

    DAYS_COUNT = 0;
    ColumnGroup cg = null;
    for(int i=START_YEAR;i<=END_YEAR;i++) {
      for(int j=(i==START_YEAR?START_MONTH:0);j<=(i==END_YEAR?END_MONTH:months.length-1);j++) {
        cp.set(i, j, 1);
        
        cg = new ColumnGroup(months[j].substring(0,3)+i);
        header.addColumnGroup(cg);
        
        for(int k=DAYS_COUNT;k<DAYS_COUNT+cp.getActualMaximum(Calendar.DAY_OF_MONTH);k++)
          cg.add(getColumnModel().getColumn(k));
        DAYS_COUNT += cp.getActualMaximum(Calendar.DAY_OF_MONTH);
      }
    }
    header.setFont(new Font("Arial Narrow",Font.PLAIN,10));
    header.revalidate();
    setDayWidth(dayWidth);
  }
  
  private void recalculate() {
    cp.setTimeInMillis(System.currentTimeMillis());

    CURRENT_YEAR = cp.get(Calendar.YEAR);
    CURRENT_MONTH = cp.get(Calendar.MONTH);
    CURRENT_DAY   = cp.get(Calendar.DAY_OF_MONTH);

    START_YEAR = cp.get(Calendar.YEAR)-(11-cp.get(Calendar.MONTH)+previosMonthPeriod)/12;
    END_YEAR   = cp.get(Calendar.YEAR)+(cp.get(Calendar.MONTH)+nextMonthPeriod)/12;

    START_MONTH = cp.get(Calendar.MONTH)-previosMonthPeriod%12;
    if(START_MONTH < 0) {
      START_MONTH += 12;
    }

    END_MONTH = cp.get(Calendar.MONTH)+nextMonthPeriod%12;
    if(END_MONTH > 11) {
      END_MONTH -= 12;
    }

    DAYS_COUNT = 0;
    for(int i=START_YEAR;i<=END_YEAR;i++) {
      for(int j=(i==START_YEAR?START_MONTH:0);j<=(i==END_YEAR?END_MONTH:months.length-1);j++) {
        cp.set(i, j, 1);
        if(i == CURRENT_YEAR && CURRENT_MONTH == j)
          CURRENT_COLUMN = DAYS_COUNT + CURRENT_DAY - 1;
        DAYS_COUNT += cp.getActualMaximum(Calendar.DAY_OF_MONTH);
      }
    }
    
    cp.set(START_YEAR, START_MONTH, 1, 0, 0, 0);
    cp.set(Calendar.MILLISECOND, 0);
    START_TIMESTAMP = cp.getTimeInMillis();
    reloadModel();
    
    System.out.println("CURRENT_MONTH = "+CURRENT_MONTH);
    System.out.println("CURRENT_YEAR  = "+CURRENT_YEAR);
    System.out.println("previosMonthPeriod = "+previosMonthPeriod);
    System.out.println("START_MONTH = "+START_MONTH);
    System.out.println("START_YEAR  = "+START_YEAR);
  }
  
  @Override
  public void clearSelection() {
    super.clearSelection();
    if(selectedCellBoxes != null)
      selectedCellBoxes.clear();
    /*if(periods != null)
      for(CopyOnWriteArrayList<ObjectPeriod> listPeriods: periods)
        for(ObjectPeriod p:listPeriods)
          p.setSelected(false);*/
    repaint();
  }
  
  public void setVisibleColor(Color... colors) {
    for(CopyOnWriteArrayList<ObjectPeriod> listPeriods: periods) {
      for(ObjectPeriod p:listPeriods) {
        p.setVisible(ArrayUtils.contains(colors, p.getColor()));
      }
    }
    repaint();
  }
  
  public void clearVisibleColors() {
    for(CopyOnWriteArrayList<ObjectPeriod> listPeriods: periods) {
      for(ObjectPeriod p:listPeriods) {
        p.setVisible(true);
      }
    }
    repaint();
  }
  
  public void setColor(Map<Integer,Color> map) {
    for(CopyOnWriteArrayList<ObjectPeriod> listPeriods: periods) {
      for(ObjectPeriod p:listPeriods) {
        Color c = map.get(p.getId());
        if(c != null)
          p.setColor(c);
      }
    }
    repaint();
  }
  
  public CopyOnWriteArrayList<CopyOnWriteArrayList<ObjectPeriod>> getPeriods() {
    return periods;
  }
  
  public ObjectPeriod getPeriod(Integer id) {
    for(CopyOnWriteArrayList<ObjectPeriod> listPeriods: periods)
      for(ObjectPeriod p:listPeriods)
        if(p.getId().equals(id))
          return p;
    return null;
  }
  
  public List<DivisionCellBox> getSelectedCellBoxes() {
    return selectedCellBoxes;
  }
  
  public void setSelectedCellBoxex(List<DivisionCellBox> selectedCellBoxes) {
    this.selectedCellBoxes = selectedCellBoxes;
  }

  @Override
  public int[] getSelectedRows() {
    int[] rows = new int[0];
    for(DivisionCellBox cellBox:selectedCellBoxes) {
      for(int i=cellBox.getRow0();i<=cellBox.getRow1();i++)
        rows = ArrayUtils.add(rows, i);
    }
    return rows;
  }

  @Override
  public int getSelectedRow() {
    int[] rows = getSelectedRows();
    Arrays.sort(rows);
    return rows[0];
  }

  @Override
  public int[] getSelectedColumns() {
    int[] columns = new int[0];
    for(DivisionCellBox cellBox:selectedCellBoxes) {
      for(int i=cellBox.getColumn0();i<=cellBox.getColumn1();i++)
        if(!ArrayUtils.contains(columns, i))
          columns = ArrayUtils.add(columns, i);
    }
    return columns;
  }

  @Override
  public int getSelectedColumn() {
    int[] columns = getSelectedColumns();
    Arrays.sort(columns);
    return columns[0];
  }

  @Override
  public int getSelectedColumnCount() {
    return getSelectedColumns().length;
  }
  
  private boolean isConstrains(DivisionCellBox box) {
    for(DivisionCellBox cellBox:selectedCellBoxes)
      if(cellBox.contains(box))
        return true;
    return false;
  }

  @Override
  public void addColumnSelectionInterval(int index0, int index1) {
    DivisionCellBox cellBox = new DivisionCellBox(0, index0, getRowCount()-1, index1);
    if(!isConstrains(cellBox)) {
      selectedCellBoxes.add(cellBox);
      fireTableSelectionChange();
      //super.addColumnSelectionInterval(index0, index1);
      //firePeriodsSelected();
      fireSelected();
    }
  }

  @Override
  public void addRowSelectionInterval(int index0, int index1) {
    DivisionCellBox cellBox = new DivisionCellBox(index0, 0, index1, getColumnCount()-1);
    if(!isConstrains(cellBox)) {
      selectedCellBoxes.add(cellBox);
      fireTableSelectionChange();
      //super.addRowSelectionInterval(index0, index1);
      //firePeriodsSelected();
      fireSelected();
    }
  }

  @Override
  public void setRowSelectionInterval(int index0, int index1) {
    clearSelection();
    selectedCellBoxes.add(new DivisionCellBox(index0, 0, index1, getColumnCount()-1));
    fireTableSelectionChange();
    //super.setRowSelectionInterval(index0, index1);
    //firePeriodsSelected();
    fireSelected();
  }

  @Override
  public void setColumnSelectionInterval(int index0, int index1) {
    clearSelection();
    selectedCellBoxes.add(new DivisionCellBox(0, index0, getRowCount()-1, index1));
    fireTableSelectionChange();
    //super.setColumnSelectionInterval(index0, index1);
    //firePeriodsSelected();
    fireSelected();
  }
  
  public boolean isPeriodSelected(ObjectPeriod period) {
    for(DivisionCellBox box:selectedCellBoxes) {
      if(box.intersection(period))
        return true;
    }
    return false;
  }
  
  public List<ObjectPeriod> getSelectedPeriods() {
    List<ObjectPeriod> ps = new ArrayList<>();
    for(DivisionCellBox box:selectedCellBoxes) {
      for(int i=0;i<periods.size();i++) {
        for(ObjectPeriod p:periods.get(i)) {
          if(box.intersection(p) && p.isVisible())
            ps.add(p);
        }
      }
    }
    return ps;
  }
  
  public List<Date> getIntersectionDatesOfPeriods(List<ObjectPeriod> listPeriods) {
    List<Date> dates = new ArrayList<>();
    long start = -1;
    long end = -1;
    for(ObjectPeriod period:listPeriods) {
      if(start == -1)
        start = period.getStartDate();
      if(end == -1)
        end = period.getEndDate();
      
      if(period.getStartDate() > end || period.getEndDate() < start)
        return dates;
      
      if(period.getStartDate() > start && period.getStartDate() <= end)
        start = period.getStartDate();
      
      if(period.getEndDate() < end && period.getEndDate() >= start)
        end = period.getEndDate();
    }
    if(start != -1 && end != -1 && start <= end) {
      
      long time = start;
      while(time < end) {
        dates.add(new Date(time));
        time += DAY_TIME;
      }
    }
    return dates;
  }
  
  public Integer[] getSelectedIds() {
    Integer[] ids = new Integer[0];
    for(ObjectPeriod period:getSelectedPeriods())
      if(!ArrayUtils.contains(ids, period.getId()))
        ids = (Integer[]) ArrayUtils.add(ids, period.getId());
    return ids;
  }
  
  public Date getSelectedStartDate() {
    int[] columns = getSelectedColumns();
    Arrays.sort(columns);
    return getDate(columns[0]);
  }
  
  public Date getSelectedEndDate() {
    int[] columns = getSelectedColumns();
    Arrays.sort(columns);
    return getDate(columns[columns.length-1]);
  }
  
  public TreeMap<Integer,List<Date>> getSelectedDates() {
    TreeMap<Integer,List<Date>> dates = new TreeMap<>();
    for(DivisionCellBox cellBox:selectedCellBoxes) {
      for(int i=cellBox.getRow0();i<=cellBox.getRow1();i++) {
        List<Date> d = dates.get(i);
        if(d == null) {
          d = new ArrayList<>();
          dates.put(i, d);
        }
        for(int j=cellBox.getColumn0();j<=cellBox.getColumn1();j++)
          d.add(getDate(j));
      }
    }
    return dates;
  }

  public int getColumn(Date date) {
    return getColumn(date.getTime());
  }
  
  
  public int getColumn(long date) {
    cp.setTimeInMillis(date);
    int year = cp.get(Calendar.YEAR);
    
    cp.set(START_YEAR, START_MONTH, 1, 0, 0, 0);
    cp.set(Calendar.MILLISECOND, 0);
    
    int column = 0;
    for(int y=START_YEAR;y<year;y++) {
      cp.set(Calendar.YEAR, y);
      column += cp.getActualMaximum(Calendar.DAY_OF_YEAR);
    }
    
    cp.setTimeInMillis(date);
    
    column += cp.get(Calendar.DAY_OF_YEAR);
    
    cp.setTimeInMillis(START_TIMESTAMP);
    
    column -= cp.get(Calendar.DAY_OF_YEAR);
    
    return column;
  }
  
  public Date getDate(int column) {
    cp.setTimeInMillis(START_TIMESTAMP);
    
    column -= (cp.getActualMaximum(Calendar.DAY_OF_YEAR) - cp.get(Calendar.DAY_OF_YEAR));
    
    while(column > 0) {
      cp.set(Calendar.YEAR, cp.get(Calendar.YEAR)+1);
      column -= cp.getActualMaximum(Calendar.DAY_OF_YEAR);
    }
    
    cp.set(Calendar.DAY_OF_YEAR, cp.getActualMaximum(Calendar.DAY_OF_YEAR)+column);
    
    return cp.getTime();
  }

  public int getDayWidth() {
    return dayWidth;
  }
  
  public void setDayWidth(int dayWidth) {
    setShowGrid(dayWidth > 1);
    
    this.dayWidth = dayWidth;
    for(int i=0;i<getColumnCount();i++) {
      getColumnModel().getColumn(i).setMinWidth(dayWidth);
      getColumnModel().getColumn(i).setMaxWidth(dayWidth);
      getColumnModel().getColumn(i).setPreferredWidth(dayWidth);
    }
    
    for(CopyOnWriteArrayList<ObjectPeriod> list:periods)
      for(ObjectPeriod period:list)
        period.recalculate();
    
    for(DivisionCellBox box:selectedCellBoxes)
      box.recalculate();
    fireDayWidthChanged();
  }
  
  public void addColorLabel(long date, Color color, String name) {
    addColorLabel(new ColorLabel(date, color, name));
  }
  
  public void addColorLabel(Date date, Color color, String name) {
    addColorLabel(new ColorLabel(date, color, name));
  }
  
  public void addColorLabel(java.sql.Date date, Color color, String name) {
    addColorLabel(new ColorLabel(date, color, name));
  }
  
  public void addColorLabel(ColorLabel colorLabel) {
    if(!labels.contains(colorLabel))
      labels.add(colorLabel);
  }
  
  public void removeColorLabel(java.sql.Date date) {
    removeColorLabel(date.getTime());
  }
  
  public void removeColorLabel(Date date) {
    removeColorLabel(date.getTime());
  }
  
  public void removeColorLabel(long date) {
    for(int i=labels.size()-1;i>=0;i--)
      if(labels.get(i).getDate() == date)
        labels.remove(i);
        
  }
  
  public void removeColorLabel(String name) {
    for(int i=labels.size()-1;i>=0;i--)
      if(labels.get(i).getText().equals(name))
        labels.remove(i);
  }
  
  public void removeColorLabel(ColorLabel colorLabel) {
    labels.remove(colorLabel);
  }
  
  
  
  public ObjectPeriod createPeriod(int rowIndex, Integer id, java.sql.Date start, java.sql.Date end, Color color, String comment) {
    return new ObjectPeriod(this, rowIndex, start, end, color, id, comment);
  }
  
  public ObjectPeriod createPeriod(int rowIndex, Integer id, Timestamp start, Timestamp end, Color color, String comment) {
    return new ObjectPeriod(this, rowIndex, start, end, color, id, comment);
  }
  
  public ObjectPeriod createPeriod(int rowIndex, Integer id, Date start, Date end, Color color, String comment) {
    return new ObjectPeriod(this, rowIndex, start, end, color, id, comment);
  }
  
  
  public void addPeriod(int rowIndex, Integer id, java.sql.Date start, java.sql.Date end, Color color, String comment) {
    addPeriods(new ObjectPeriod[]{createPeriod(rowIndex, id, start, end, color, comment)});
  }
  
  public void addPeriod(int rowIndex, Integer id, Timestamp start, Timestamp end, Color color, String comment) {
    addPeriods(new ObjectPeriod[]{createPeriod(rowIndex, id, start, end, color, comment)});
  }
  
  public void addPeriod(int rowIndex, Integer id, Date start, Date end, Color color, String comment) {
    addPeriods(new ObjectPeriod[]{createPeriod(rowIndex, id, start, end, color, comment)});
  }
  
  public void addPeriods(List<ObjectPeriod> listPeriods) {
    addPeriods(listPeriods.toArray(new ObjectPeriod[0]));
  }
  
  public void addPeriods(ObjectPeriod[] listPeriods) {
    int rowIndex;
    for(ObjectPeriod period:listPeriods) {
      rowIndex = period.getRowIndex();
      if(rowIndex>=periods.size())
        for(int i=periods.size();i<=rowIndex;i++)
          periods.add(new CopyOnWriteArrayList<>());
      CopyOnWriteArrayList<ObjectPeriod> c = periods.get(rowIndex);
      if(c == null) {
        c = new CopyOnWriteArrayList<>();
        periods.add(rowIndex, c);
      }
      if(!c.contains(period))
        c.add(period);
    }
    repaint();
    firePeriodsSelected();
  }
  
  public ObjectPeriod periodAtId(Integer id) {
    for(CopyOnWriteArrayList<ObjectPeriod> period:periods)
      for(ObjectPeriod p:period)
        if(p.getId().equals(id))
          return p;
    return null;
  }
  
  public void removePeriods(Integer[] ids) {
    for(int i=periods.size()-1;i>=0;i--) {
      CopyOnWriteArrayList<ObjectPeriod> period = periods.get(i);
      for(int j=period.size()-1;j>=0;j--) {
        if(ArrayUtils.contains(ids, period.get(j).getId()))
          period.remove(j);
      }
    }
    repaint();
  }
  
  public void removePeriod(Integer id) {
    removePeriods(new Integer[]{id});
  }
  
  @Override
  public void clear() {
    periods.clear();
    ((DefaultTableModel)getModel()).getDataVector().clear();
    ((DefaultTableModel)getModel()).fireTableDataChanged();
  }
  
  public void clear(int rowIndex) {
    if(periods.size() > rowIndex)
      periods.remove(rowIndex);
    if(periods.isEmpty())
      clear();
  }
  
  public ObjectPeriod periodAtRowColumn(int row, int column) {
    return periodAtRowColumn(row, column, true);
  }
  
  public ObjectPeriod periodAtRowColumn(int row, int column, boolean visible) {
    if(column >= 0 && row >= 0 && periods.size() > row) {
      for(ObjectPeriod p:periods.get(row)) {
        if((visible && p.isVisible() || !visible) && column >= p.getStartColumn() && column <= p.getEndColumn())
          return p;
      }
    }
    return null;
  }
  
  public ObjectPeriod periodAtPoint(Point point) {
    return periodAtRowColumn(rowAtPoint(point), columnAtPoint(point));
  }
  
  private void fireSelected() {
    if(isEnabled()) {
      SwingUtilities.invokeLater(() -> {
        final List<ObjectPeriod> ps = getSelectedPeriods();
        TreeMap<Integer,List<Date>> days = getSelectedDates();
        if(!ps.isEmpty())
          firePeriodsSelected(ps);
        else if(!days.isEmpty())
          fireDaysSelected(days);
      });
    }
  }
  
  private void firePeriodsSelected() {
    if(isEnabled()) {
      SwingUtilities.invokeLater(() -> {
        final List<ObjectPeriod> ps = getSelectedPeriods();
        if(!ps.isEmpty())
          firePeriodsSelected(ps);
      });
    }
  }
  
  public void firePeriodsSelected(List<ObjectPeriod> ps) {
    if(isEnabled())
      for(ObjectPeriodScaleListener listener:listeners)
        listener.objectPeriodsSelected(ps);
  }

  public void firePeriodDoubleClicked(ObjectPeriod period) {
    if(isEnabled())
      for(ObjectPeriodScaleListener listener:listeners)
        listener.objectPeriodDoubleClicked(period);
  }

  public void fireDaysSelected(TreeMap<Integer,List<Date>> days) {
    if(isEnabled())
      for(ObjectPeriodScaleListener listener:listeners)
        listener.daysSelected(days);
  }

  public void fireDayDoubleClicked(int rowIndex, Date day) {
    if(isEnabled())
      for(ObjectPeriodScaleListener listener:listeners)
        listener.dayDoubleClicked(rowIndex, day);
  }
  
  public void fireDayWidthChanged() {
    if(isEnabled())
      for(ObjectPeriodScaleListener listener:listeners)
        listener.dayWidthChanged(getDayWidth());
  }

  public void addObjectPeriodScaleListener(ObjectPeriodScaleListener listener) {
    listeners.add(listener);
  }

  public void removeObjectPeriodScaleListener(ObjectPeriodScaleListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void mouseClicked(MouseEvent e) {
  }
  
  private void validateCellBoxes() {
    for(int i=selectedCellBoxes.size()-1;i>=0;i--) {
      for(int j=selectedCellBoxes.size()-1;j>=0;j--) {
        if(i != j) {
          Rectangle rect = selectedCellBoxes.get(i).getBounds().intersection(selectedCellBoxes.get(j).getBounds());
          if(rect != null && !rect.isEmpty()) {
            selectedCellBoxes.get(i).setBounds(selectedCellBoxes.get(i).getBounds().union(selectedCellBoxes.get(j).getBounds()));
            selectedCellBoxes.remove(j);
          }
        }
      }
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if(e.getModifiers() != MouseEvent.META_MASK) {
      int column = columnAtPoint(e.getPoint());
      int row    = rowAtPoint(e.getPoint());
      if(column >= 0 && row >= 0) {
        int shiftmask = MouseEvent.SHIFT_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;
        int ctrlmask  = MouseEvent.CTRL_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;
        boolean isCtrl  = (e.getModifiersEx() & ctrlmask) == ctrlmask;
        boolean isShift = (e.getModifiersEx() & shiftmask) == shiftmask;
        
        if(!isCtrl && !isShift)
          clearSelection();
        
        DivisionCellBox box = new DivisionCellBox(row, column, row, column);
        
        ObjectPeriod p = periodAtRowColumn(row,column);
        if(p != null)
          box = new DivisionCellBox(row, p.getStartColumn(), row, p.getEndColumn());
        
        if(isShift && !selectedCellBoxes.isEmpty()) {
          DivisionCellBox startBox = selectedCellBoxes.get(selectedCellBoxes.size()-1);
          if(row >= startBox.getRow0() && column > startBox.getColumn0())
            box = new DivisionCellBox(startBox.getRow0(), startBox.getColumn0(), row, column);
          if(row >= startBox.getRow0() && column < startBox.getColumn1())
            box = new DivisionCellBox(startBox.getRow0(), column, row, startBox.getColumn1());
          if(row < startBox.getRow1() && column < startBox.getColumn1())
            box = new DivisionCellBox(row, column, startBox.getRow1(), startBox.getColumn1());
          if(row < startBox.getRow1() && column > startBox.getColumn0())
            box = new DivisionCellBox(row, startBox.getColumn0(), startBox.getRow1(), column);
        }
        selectedCellBoxes.add(box);
        
        List<ObjectPeriod> ps = getSelectedPeriods();
        TreeMap<Integer,List<Date>> days = getSelectedDates();
        if(e.getClickCount() >= 2) {
          if(!ps.isEmpty())
            firePeriodDoubleClicked(ps.get(0));
          else if(!days.isEmpty())
            fireDayDoubleClicked(days.firstKey(),days.get(days.firstKey()).get(0));
        }else {
          if(!ps.isEmpty())
            firePeriodsSelected(ps);
          else if(!days.isEmpty())
            fireDaysSelected(days);
        }
        
        fireTableSelectionChange();
        repaint();
      }
      validateCellBoxes();
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
  }

  @Override
  public void mouseEntered(MouseEvent e) {
  }

  @Override
  public void mouseExited(MouseEvent e) {
    repaint();
  }
  
  @Override
  public void mouseDragged(MouseEvent e) {
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    repaint();
  }

  @Override
  public void keyTyped(KeyEvent e) {
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK)==KeyEvent.CTRL_DOWN_MASK && e.getKeyCode()==KeyEvent.VK_RIGHT) {
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
  }
  
  public Date getStartDate() {
    return getDate(0);
  }
  
  public Date getEndDate() {
    return getDate(DAYS_COUNT);
  }
  
  
  
  public void scrollToDate(final Date date) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Rectangle rec = getCellRect(0, getColumn(date), true);
        rec.setLocation(rec.x-getVisibleRect().width/2, rec.y);
        rec.setSize(getVisibleRect().width, rec.height);
        scrollRectToVisible(rec);
      }
    });
  }
  
  public void scrollToCurrentDate() {
    scrollToDate(getDate(CURRENT_COLUMN));
  }
  
  protected String getToolTipText(int row, int column) {
    Date date = getDate(column);
      
    String labelName = "";
    for(ColorLabel label:labels)
      if(label.getDate() == date.getTime())
        labelName = "<br/><b>"+label.getText()+"</b><br/>";

    String toolTipText = Utility.format(date);
    ObjectPeriod period = periodAtRowColumn(row, column);
    if(period != null) {
      toolTipText += "<br/><b>"+period.getComment()+"<br/>Период: c "+Utility.format(new Date(period.getStartDate()))+" по "+Utility.format(new Date(period.getEndDate()))+"</b>";
    }
    
    return toolTipText+labelName;
  }
  
  public int rowAtId(Integer id) {
    for(int i=0;i<periods.size();i++) {
      for(ObjectPeriod period:periods.get(i)) {
        if(period.getId().equals(id))
          return i;
      }
    }
    return -1;
  }
  
  public int rowAtPeriod(ObjectPeriod period) {
    return rowAtId(period.getId());
  }
  
  public int getCurrentColumn() {
    return this.CURRENT_COLUMN;
  }

  public void setSelected(Integer[] selectIds) {
    for(int i=0;i<periods.size();i++) {
      for(ObjectPeriod p:periods.get(i)) {
        if(ArrayUtils.contains(selectIds, p.getId())) {
          DivisionCellBox cellBox = new DivisionCellBox(i, p.getStartColumn(), i, p.getEndColumn());
          if(!selectedCellBoxes.contains(cellBox)) {
            selectedCellBoxes.add(cellBox);
          }
        }
      }
    }
    fireTableSelectionChange();
  }
  
  private int getPeriodCount() {
    int count = 0;
    for(CopyOnWriteArrayList<ObjectPeriod> listPeriods: periods)
      count += listPeriods.size();
    return count;
  }
  
  class PeriodsTransferable implements Transferable {
    private TreeMap<String, Object> data = new TreeMap<>();

    public PeriodsTransferable(Integer id, int currentColumn) {
      ObjectPeriod period = getPeriod(id);
      data.put("periodId", id);
      data.put("columnCount", period.getEndColumn()-period.getStartColumn());
      data.put("startDragColumn", (currentColumn - period.getStartColumn()));
      
      data.put("row", period.getRowIndex());
      data.put("startDate", period.getStartDate());
      data.put("endDate", period.getEndDate());
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
      return new DataFlavor[]{DataFlavor.stringFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return flavor.equals(DataFlavor.stringFlavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
      if(isDataFlavorSupported(flavor))
        return data;
      return null;
    }
  }
  
  @Override
  public void dragGestureRecognized(DragGestureEvent dge) {
    if(getPeriodDragger() != null && dge.getTriggerEvent().getModifiers() != InputEvent.META_MASK) {
      Integer[] ids = getSelectedIds();
      if(ids.length == 1) {
        Point point = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(point, this);
        dge.startDrag(new Cursor(Cursor.MOVE_CURSOR), new PeriodsTransferable(ids[0],columnAtPoint(point)), this);
        clearSelection();
      }
    }
  }

  @Override
  public void dragEnter(DragSourceDragEvent dsde) {
    //System.out.println("dragEnter");
  }

  @Override
  public void dragOver(DragSourceDragEvent dsde) {
    //System.out.println("dragOver");
  }

  @Override
  public void dropActionChanged(DragSourceDragEvent dsde) {
    //System.out.println("dropActionChanged");
  }

  @Override
  public void dragExit(DragSourceEvent dse) {
    //System.out.println("dragExit");
  }

  @Override
  public void dragDropEnd(DragSourceDropEvent dsde) {
    //System.out.println("dragDropEnd");
  }

  @Override
  public void dragEnter(DropTargetDragEvent dtde) {
    //System.out.println("dragEnter");
  }
  
  public boolean isConstrains(int row, int newStartColumn, int newEndColumn, ObjectPeriod period) {
    boolean is = false;
    if(row >= 0 && periods.size() > row) {
      for(ObjectPeriod p:periods.get(row)) {
        if(!p.equals(period)) {
          if(p.getStartColumn() >= newStartColumn && p.getStartColumn() <= newEndColumn || p.getEndColumn() >= newStartColumn && p.getEndColumn() <= newEndColumn) {
            is = true;
            break;
          }
        }
      }
    }
    return is;
  }

  @Override
  public void dragOver(final DropTargetDragEvent dtde) {
    if(getPeriodDragger() != null) {
      try {
        TreeMap<String, Object> data = (TreeMap<String, Object>) dtde.getTransferable().getTransferData(DataFlavor.stringFlavor);
        ObjectPeriod period = getPeriod((Integer) data.get("periodId"));

        int startDragColumn = (int) data.get("startDragColumn");
        int columnCount     = (int) data.get("columnCount");
        int currentColumn   = columnAtPoint(dtde.getLocation());

        int row = rowAtPeriod(period);
        int delta = currentColumn-(period.getStartColumn()+startDragColumn);
        int startColumn = period.getStartColumn()+delta;

        int newRow = rowAtPoint(dtde.getLocation());

        Calendar c = Calendar.getInstance();
        c.setTime(getDate(startColumn+columnCount));
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);

        if(getPeriodDragger().isMove(period, newRow, getDate(startColumn).getTime(), c.getTimeInMillis()) && !isConstrains(newRow, startColumn, startColumn+columnCount, period)) {
          if(newRow != row)
            period.setRowIndex(newRow);
          period.setStartDate(getDate(startColumn).getTime());
          period.setEndDate(c.getTimeInMillis());
          period.recalculate();
          repaint();
        }
      }catch(UnsupportedFlavorException | IOException ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }

  @Override
  public void dropActionChanged(DropTargetDragEvent dtde) {
  }

  @Override
  public void dragExit(DropTargetEvent dte) {
  }

  @Override
  public void drop(DropTargetDropEvent dtde) {
    if(getPeriodDragger() != null) {
      try {
        TreeMap<String, Object> data = (TreeMap<String, Object>) dtde.getTransferable().getTransferData(DataFlavor.stringFlavor);
        ObjectPeriod period = getPeriod((Integer) data.get("periodId"));
        if(period.getRowIndex() != (int)data.get("row") || period.getStartDate() != (long)data.get("startDate") || period.getEndDate() != (long)data.get("endDate"))
          periodDragger.moved(period, (int)data.get("row"), (long)data.get("startDate"), (long)data.get("endDate"));
      }catch(UnsupportedFlavorException | IOException | HeadlessException ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  public class DivisionCellBox {
    private int row0;
    private int column0;
    
    private int row1;
    private int column1;
    
    private Rectangle bounds = null;

    public DivisionCellBox(int row0, int column0, int row1, int column1) {
      this.row0    = row0;
      this.column0 = column0;
      this.row1    = row1;
      this.column1 = column1;
    }
    
    private void setBounds(Rectangle rect) {
      row0 = rowAtPoint(new Point(rect.x, rect.y+1));
      row1 = rowAtPoint(new Point(rect.x, rect.y+rect.height-1));
      column0 = columnAtPoint(new Point(rect.x, rect.y));
      column1 = columnAtPoint(new Point(rect.x+rect.width-1, rect.y));
    }

    public Rectangle getBounds() {
      if(bounds == null) {
        Rectangle r0 = getCellRect(row0, column0, true);
        Rectangle r1 = getCellRect(row1, column1, true);
        bounds = new Rectangle(r0.x,r0.y,r1.x+r1.width-r0.x,r1.y+r1.height-r0.y);
      }
      return bounds;
    }
    
    public void recalculate() {
      bounds = null;
    }
    
    public boolean contains(DivisionCellBox box) {
      return getBounds().contains(box.getBounds());
    }
    
    public boolean contains(int row, int column) {
      return getBounds().contains(getCellRect(row, column, true).getLocation());
    }
    
    public boolean contains(ObjectPeriod period) {
      return getBounds().contains(period.getBounds());
    }
    
    public boolean intersection(ObjectPeriod period) {
      return getBounds().intersects(period.getBounds());
    }

    public int getColumn0() {
      return column0;
    }

    public int getColumn1() {
      return column1;
    }

    public int getRow0() {
      return row0;
    }

    public int getRow1() {
      return row1;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final DivisionCellBox other = (DivisionCellBox) obj;
      if (this.row0 != other.row0) {
        return false;
      }
      if (this.column0 != other.column0) {
        return false;
      }
      if (this.row1 != other.row1) {
        return false;
      }
      if (this.column1 != other.column1) {
        return false;
      }
      return true;
    }
  }
}