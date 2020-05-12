package division.fx.table;

import com.sun.javafx.scene.control.skin.TableColumnHeader;
import com.sun.javafx.scene.control.skin.TableHeaderRow;
import com.sun.javafx.scene.control.skin.TableViewSkinBase;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import com.sun.media.jfxmediaimpl.MediaDisposer;
import division.fx.DivisionTextField;
import division.fx.PropertyMap;
import division.fx.gui.FXDisposable;
import division.fx.table.filter.TableFilter;
import division.util.Utility;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import org.apache.commons.lang3.ArrayUtils;

public class FXDivisionTable<S extends PropertyMap> extends TableView<S> implements FXDisposable {
  private TableFilter<S> filter;
  
  private Popup pop;
  private DivisionTextField findText;
  
  private BooleanProperty findable = new SimpleBooleanProperty(true);
  
  private final ObjectProperty<TableRow<S>> lastSelectedRow = new SimpleObjectProperty();
  
  private SortedList<S> sortedData;
  
  public FXDivisionTable() {
    pop = new Popup();
    findText = new DivisionTextField();
    
    getColumns().addListener((ListChangeListener.Change<? extends TableColumn<S, ?>> c) -> getColumns().stream().forEach(column -> setFilters(column)));
    
    setRowFactory((TableView<S> param) -> {
      TableRow<S> row = new TableRow();
      row.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
        if(newValue)
          lastSelectedRow.set(row);
      });
      return row;
    });
    
    pop.setAutoHide(true);
    pop.setAutoFix(true);
    pop.setHideOnEscape(true);
    pop.getContent().add(findText);
    
    findText.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!newValue)
        pop.hide();
    });
    
    findText.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> findNextText(0));
    
    findText.setOnKeyPressed(e -> {
      if(e.getCode() == KeyCode.F3)
        findNextText(getSelectionModel().getSelectedIndex() + 1);
      if(e.getCode() == KeyCode.F2)
        findPreviosText(getSelectionModel().getSelectedIndex()-1);
    });
    
    pop.setOnHiding(e -> getSelectionModel().setCellSelectionEnabled(false));
    setOnMousePressed(e -> pop.hide());
    
    
    
    setOnKeyPressed(e -> {
      if(findable.getValue() && getEditingCell() == null && !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && !e.isShiftDown() && !e.isShortcutDown()) {
        TableRow<S> row = lastSelectedRowProperty().getValue();
        if(!pop.isShowing() && row != null && !getSelectionModel().getSelectedCells().isEmpty()) {
          TableViewSkinBase skin = (TableViewSkinBase)getSkin();
          TableHeaderRow headerRow = skin.getTableHeaderRow();
          TableColumnHeader header = headerRow.getColumnHeaderFor(getSelectionModel().getSelectedCells().get(0).getTableColumn());
          if(header != null) {
            findText.setPrefSize(header.getWidth(), header.getHeight()-4);
            findText.setMinSize(header.getWidth(), header.getHeight()-4);
            findText.setMaxSize(header.getWidth(), header.getHeight()-4);

            Double y = header.localToScreen(header.getBoundsInLocal()).getMinY(); //row.localToScreen(row.getBoundsInLocal()).getMaxY();
            Double x = header.localToScreen(header.getBoundsInLocal()).getMinX();
            pop.show(this, x, y);
            findText.setText("");
            //findText.setText(e.getText());
            findText.requestFocus();
            findText.positionCaret(findText.getText().length());
          }
        }
      }
    });
    
    addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
      if(getRowAtPoint(e.getX(),e.getY()) < 0)
        getSelectionModel().clearSelection();
    });
  }
  
  public int getRowAtPoint (double x, double y) {
    VirtualFlow vf = (VirtualFlow)getChildrenUnmodifiable().get(1);
    y -= vf.getBoundsInParent().getMinY();
    for(int i=0;i<vf.getCellCount();i++) {
      if(vf.getVisibleCell(i) != null && vf.getVisibleCell(i).getBoundsInParent().contains(x, y))
        return i;
    }
    return -1;
  }
  
  private void setFilters(TableColumn column) {
    if(!column.getColumns().isEmpty())
      column.getColumns().stream().forEach(c -> setFilters((TableColumn)c));
    if(column instanceof Column && ((Column)column).getColumnFilter() != null && getTableFilter().getFilter(column) == null)
      getTableFilter().addFilter(column, ((Column)column).getColumnFilter());
  }
  
  public FXDivisionTable(TableColumn... columns) {
    this(false, columns);
  }
  
  public FXDivisionTable(boolean editable, TableColumn... columns) {
    this();
    setEditable(editable);
    getColumns().setAll(columns);
  }

  public BooleanProperty findableProperty() {
    return findable;
  }
  
  private void findPreviosText(int startRow) {
    String value;
    String text = findText.getText().toLowerCase();
    TableColumn column = getSelectionModel().getSelectedCells().get(0).getTableColumn();
    for(int i=startRow<0?0:startRow;i>=0;i--) {
      value = (
              column.getCellData(i) instanceof LocalDate ? Utility.format((LocalDate)column.getCellData(i)) : 
              column.getCellData(i) instanceof Date ? Utility.format((Date)column.getCellData(i)) : 
              column.getCellData(i) instanceof java.sql.Date ? Utility.format((java.sql.Date)column.getCellData(i)) : 
                      column.getCellData(i).toString()).toLowerCase();
      if(value != null && value.contains(text)) {
        getSelectionModel().select(i,column);
        scrollTo(i);
        break;
      }
    }
  }
  
  private void findNextText(int startRow) {
    String value;
    String text = findText.getText().toLowerCase();
    TableColumn column = getSelectionModel().getSelectedCells().get(0).getTableColumn();
    for(int i=startRow<0?0:startRow;i<getItems().size();i++) {
      if(column.getCellData(i) != null) {
        value = (
                column.getCellData(i) instanceof LocalDate ? Utility.format((LocalDate)column.getCellData(i)) : 
                column.getCellData(i) instanceof Date ? Utility.format((Date)column.getCellData(i)) : 
                column.getCellData(i) instanceof java.sql.Date ? Utility.format((java.sql.Date)column.getCellData(i)) : 
                        column.getCellData(i).toString()).toLowerCase();
        if(value != null && value.contains(text)) {
          getSelectionModel().select(i,column);
          scrollTo(i);
          break;
        }
      }
    }
  }
  
  public ObjectProperty<TableRow<S>> lastSelectedRowProperty() {
    return lastSelectedRow;
  }
  
  public Column getColumn(String columnName) {
    return getColumn(getColumns(), columnName);
  }
  
  public Column getColumn(ObservableList columns, String columnName) {
    Column returnColumn = null;
    for(Object c:columns) {
      if(columnName.equals(((Column)c).getColumnName()) || columnName.equals(((Column)c).getDatabaseColumnName()))
        returnColumn = (Column)c;
      else returnColumn = getColumn(((Column)c).getColumns(), columnName);
      if(returnColumn != null)
        break;
    }
    return returnColumn;
  }

  public TableFilter<S> getTableFilter() {
    if(filter == null)
      setTableFilter(new TableFilter());
    return filter;
  }
  
  private List<FXDisposable> disposeList = new ArrayList<>();

  @Override
  public List<FXDisposable> disposeList() {
    return disposeList;
  }

  public void setTableFilter(TableFilter filter) {
    if(this.filter != null)
      this.filter.dispose();
    this.filter = null;
    this.filter = filter;
    
    disposeList.add(this.filter);
    
    sortedData = new SortedList(getTableFilter().getFilteredList());
    sortedData.comparatorProperty().bind(comparatorProperty());
    
    setItems(sortedData);
    //setItems(filter.getFilteredList());
  }
  
  public SortedList<S> getSortedDataList() {
    return sortedData;
  }
  
  public ObservableList<S> getSourceItems() {
    return (ObservableList<S>)getTableFilter().getItems();
  }
  
  public void addFilter(TableColumn column, division.fx.table.filter.ColumnFilter columnFilter) {
    getTableFilter().addFilter(column, columnFilter);
  }
  
  public TableColumn[] getAllColumns(TableColumn column, boolean last) {
    TableColumn[] columns = new TableColumn[]{column};
    if(column == null || last && !column.getColumns().isEmpty())
      columns = new TableColumn[0];
    
    ObservableList cols = column == null ? getColumns() :  column.getColumns();
    for(Object col:cols)
      columns = ArrayUtils.addAll(columns, getAllColumns((TableColumn)col, last));
    return columns;
  }
  
  public void setHorizontScrollBarPolicyAlways() {
    Observable[] p = new Observable[]{widthProperty()};
    for(TableColumn col:getAllColumns(null, false))
      p = ArrayUtils.addAll(p, col.widthProperty(), col.visibleProperty());
    
    TableColumn c = new TableColumn(" ");
    c.setSortable(false);
    c.setResizable(false);
    getColumns().add(c);
    c.visibleProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
    if(!newValue)
      c.setVisible(true);
    });
    
    getColumns().addListener((ListChangeListener.Change<? extends TableColumn<S, ?>> c1) -> {
      Platform.runLater(() -> {
        if(!getColumns().contains(c))
          getColumns().add(c);
        else if(getColumns().indexOf(c) != getColumns().size()-1) {
          getColumns().remove(c);
          getColumns().add(c);
        }
      });
    });
    
    c.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
      double w = getWidth()+1;
      for(Object col:getAllColumns(null, true))
        if(((TableColumn)col).isVisible() && !col.equals(c))
          w -= ((TableColumn)col).getWidth();
      return w > 0 ? w : 0;
    }, p));
  }
  
  public static void bindScrollBars(Orientation orientation, Node... scrollNodes) {
    Set<ScrollBar> bars = new HashSet<>();
    
    for(Node scrollNode:scrollNodes) {
      ScrollBar bar = getScrollBar(scrollNode, orientation);
      if(bar != null) {
        bars.add(bar);
        bar.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
          if(!bar.isDisable()) {
            bars.stream().forEach(sb -> {
              sb.setDisable(true);
              sb.valueProperty().setValue(newValue);
              sb.setDisable(false);
            });
          }
        });
      }
    }
  }
  
  public static ScrollBar getScrollBar(Node scrollNode, Orientation orientation) {
    Set<Node> set = scrollNode.lookupAll(".scroll-bar");
    for(Node node: set)
      if(((ScrollBar)node).getOrientation() == orientation)
        return (ScrollBar)node;
    return null;
  }
  
  public static <S extends PropertyMap> ObservableList<S> setGeneralItems(FXDivisionTable... tables) {
    TableFilter<S> filter = new TableFilter();
    for(FXDivisionTable table:tables) {
      for(TableColumn c:((TableFilter<PropertyMap>)table.getTableFilter()).getFilters().keySet())
        filter.addFilter(c, ((TableFilter<PropertyMap>)table.getTableFilter()).getFilters().get(c));
      table.setTableFilter(filter);
    }
    return filter.getItems();
  }

  public void clear() {
    getSourceItems().clear();
  }
  
  public static void alwaysShowScrollBar(final TableView view, Orientation orientation) {
    new Thread(() -> {
        while (true) {
            Set<Node> nodes = view.lookupAll(".scroll-bar");
            if (nodes.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                }
                continue;
            }
            Node node = view.lookup(".virtual-flow");
            if (node == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                }
                continue;
            }
            final VirtualFlow flow = (VirtualFlow) node;
            for (Node n : nodes) {
                if (n instanceof ScrollBar) {
                    final ScrollBar bar = (ScrollBar) n;
                    if (bar.getOrientation().equals(orientation)) {
                        bar.visibleProperty().addListener(l -> {
                            if (bar.isVisible()) {
                                return;
                            }
                            Field f = getVirtualFlowField("needLengthBar");
                            Method m = getVirtualFlowMethod("updateViewportDimensions");
                            try {
                                f.setBoolean(flow, true);
                                m.invoke(flow);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            bar.setVisible(true);
                        });
                        Platform.runLater(() -> {
                            bar.setVisible(true);
                        });
                        break;
                    }
                }
            }
            break;
        }
    }).start();
}

private static Field getVirtualFlowField(String name) {
    Field field = null;
    try {
        field = VirtualFlow.class.getDeclaredField(name);
        field.setAccessible(true);
    } catch (NoSuchFieldException e) {
        e.printStackTrace();
    }
    return field;
}

private static Method getVirtualFlowMethod(String name) {
    Method m = null;
    try {
        m = VirtualFlow.class.getDeclaredMethod(name);
        m.setAccessible(true);
    } catch (Exception e) {
        e.printStackTrace();
    }
    return m;
}

  @Override
  public void finaly() {
    clear();
    filter = null;
    sortedData.clear();
    sortedData = null;
  }
}
