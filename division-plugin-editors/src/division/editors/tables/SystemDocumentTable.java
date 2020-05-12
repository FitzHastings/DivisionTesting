package division.editors.tables;

import bum.editors.TableEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Document;
import bum.interfaces.ProductDocument.ActionType;
import bum.interfaces.Service.Owner;
import division.editors.objects.DocumentEditor;
import division.swing.DivisionComboBox;
import division.swing.table.CellColorController;
import division.swing.table.groupheader.ColumnGroup;
import division.swing.table.groupheader.ColumnGroupHeader;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import mapping.MappingObject;

public class SystemDocumentTable extends TableEditor {
  public SystemDocumentTable() {
    super(
            new String[] {
              "id",
              "Документ",
              "<html><center>Источник<br/>документа</center></html>",
              "<html><center>Плательщик<br/>НДС</center></html>",
              "Событие",
              "<html><center>Перемещение<br/>при событии</center></html>",
              "нал",
              "безнал",
              "нал",
              "безнал"},
            new String[]{"id","name","documentSource","ndsPayer","actionType","movable","moneyCash","moneyCashLess","tmcCash","tmcCashLess"},
            Document.class,
            DocumentEditor.class,
            "Шаблоны документов",
            MappingObject.Type.CURRENT);
    
    initComponents();
    initEvents();
  }
  
  private void initComponents() {
    setSortFields(new String[]{"name"});
    ColumnGroupHeader header = new ColumnGroupHeader(getTable());
    getTable().setTableHeader(header);

    ColumnGroup depositary = new ColumnGroup("Тип депозитария при перемещении");

    ColumnGroup money = new ColumnGroup("Валюта");
    money.add(getTable().findTableColumn(6));
    money.add(getTable().findTableColumn(7));
    depositary.add(money);

    ColumnGroup tmc = new ColumnGroup("ТМЦ");
    tmc.add(getTable().findTableColumn(8));
    tmc.add(getTable().findTableColumn(9));
    depositary.add(tmc);

    header.addColumnGroup(depositary);

    header.revalidate();

    addAutoEditingColumns(new Integer[]{2,3,4,5,6,7,8,9});
    setName("SystemDocumentTable");
    setAutoLoadAndStore(true);
    getClientFilter().AND_EQUAL("system", true);
    
    getTable().setCellEditableController((JTable table, int modelRow, int modelColumn) -> {
      if(!(table.getModel().getValueAt(modelRow, 4) instanceof JComboBox) || !(table.getModel().getValueAt(modelRow, 5) instanceof JComboBox))
          return false;
      ActionType type = ((JComboBox)table.getModel().getValueAt(modelRow, 4)).getSelectedIndex()>=0?(ActionType)((JComboBox)table.getModel().getValueAt(modelRow, 4)).getSelectedItem():null;
      String move     = (String)((JComboBox)table.getModel().getValueAt(modelRow, 5)).getSelectedItem();
      return modelColumn > 1 && modelColumn < 5 || modelColumn == 5 && type == ActionType.ОТГРУЗКА || modelColumn > 5 && move.equals("да");
    });
    
    getTable().setCellColorController(new CellColorController() {
      @Override
      public Color getCellColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        if(!(table.getModel().getValueAt(modelRow, 4) instanceof JComboBox) || !(table.getModel().getValueAt(modelRow, 5) instanceof JComboBox))
          return null;
        ActionType type = ((JComboBox)table.getModel().getValueAt(modelRow, 4)).getSelectedIndex()>=0?(ActionType)((JComboBox)table.getModel().getValueAt(modelRow, 4)).getSelectedItem():null;
        String move     = (String)((JComboBox)table.getModel().getValueAt(modelRow, 5)).getSelectedItem();
        return modelColumn > 1 && modelColumn < 5 || modelColumn == 5 && type == ActionType.ОТГРУЗКА || modelColumn > 5 && move.equals("да") ? null : isSelect ? header.getBackground().darker() : header.getBackground();
      }
    });
  }
  
  private void initEvents() {
    setRemoveAction(e -> {
      Integer[] ids = getSelectedId();
      if(ids.length > 0 && JOptionPane.showConfirmDialog(this.getGUI(), "Удалить документы из системных?", "Удаление...", JOptionPane.YES_NO_OPTION) == 0) {
        if(ObjectLoader.executeUpdate("UPDATE [Document] SET [Document(system)]=false WHERE id=ANY(?)", new Object[]{ids}) > 0)
          ObjectLoader.sendMessage(Document.class, "UPDATE", ids);
      }
    });
    
    setAddAction((ActionEvent e) -> {
      TableEditor usersDocumentTableEditor = new TableEditor(
              new String[]{"id","Документ"},
              new String[]{"id","name"},
              Document.class,
              DocumentEditor.class,
              "Шаблоны документов",
              MappingObject.Type.CURRENT);
      usersDocumentTableEditor.setName("DocumentTable");
      usersDocumentTableEditor.setAutoLoadAndStore(true);
      usersDocumentTableEditor.getClientFilter().AND_EQUAL("system", false);
      usersDocumentTableEditor.initData();
      Integer[] ids = usersDocumentTableEditor.get();
      if(ids.length > 0) {
        if(ObjectLoader.executeUpdate("UPDATE [Document] SET [Document(system)]=true WHERE id=ANY(?)", new Object[]{ids}) > 0)
          ObjectLoader.sendMessage(Document.class, "UPDATE", ids);
      }
    });
  }
  
  private DivisionComboBox createBox(Object... items) {
    DivisionComboBox box = new DivisionComboBox(items);
    box.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        if(getTable().getCellEditor() != null)
          getTable().getCellEditor().stopCellEditing();
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
      }
    });
    return box;
  }

  @Override
  public Object filterValue(Object value) {
    value = value==null?value:value.equals("да")?true:value.equals("нет")?false:value.equals("продавец")?Owner.SELLER:value.equals("покупатель")?Owner.CUSTOMER:value.equals("не важно")?null:value;
    return value;
  }

  @Override
  protected Object transformData(int modelRow, int modelColumn, Object object) {
    if(modelColumn == 2) {
      DivisionComboBox docSourceBox    = createBox("продавец","покупатель","не важно");
      docSourceBox.setSelectedIndex(object==null?2:object.equals(Owner.SELLER)?0:1);
      return docSourceBox;
    }
    if(modelColumn == 3) {
      DivisionComboBox ndsPayerBox     = createBox("да","нет","не важно");
      ndsPayerBox.setSelectedIndex(object==null?2:(boolean)object?0:1);
      return ndsPayerBox;
    }
    if(modelColumn == 4) {
      DivisionComboBox actionTypeBox   = createBox(ActionType.СТАРТ,ActionType.ОТГРУЗКА,ActionType.ОПЛАТА);
      if(object != null)
        actionTypeBox.setSelectedItem(object);
      else actionTypeBox.setSelectedIndex(-1);
      return actionTypeBox;
    }
    if(modelColumn == 5) {
      DivisionComboBox moveDispatchBox = createBox("да","нет","не важно");
      moveDispatchBox.setSelectedIndex(object==null?2:(boolean)object?0:1);
      return moveDispatchBox;
    }
    return object;
  }
  
  /*@Override
  protected void insertData(Vector<Vector> data, int startIndex) {
    data.stream().forEach((d) -> {
      DivisionComboBox docSourceBox    = createBox("продавец","покупатель","не важно");
      DivisionComboBox ndsPayerBox     = createBox("да","нет","не важно");
      DivisionComboBox actionTypeBox   = createBox(ActionType.СТАРТ.toString(),ActionType.ОТГРУЗКА.toString(),ActionType.ОПЛАТА.toString());
      DivisionComboBox moveDispatchBox = createBox("да","нет","не важно");
      
      docSourceBox.setSelectedIndex(d.get(2)==null?2:d.get(2).equals(Owner.SELLER.toString())?0:1);
      ndsPayerBox.setSelectedIndex(d.get(3)==null?2:(boolean)d.get(3)?0:1);
      if(d.get(4) != null)
        actionTypeBox.setSelectedItem(d.get(4));
      else actionTypeBox.setSelectedIndex(-1);
      moveDispatchBox.setSelectedIndex(d.get(5)==null?2:(boolean)d.get(5)?0:1);
      
      d.set(2, docSourceBox);
      d.set(3, ndsPayerBox);
      d.set(4, actionTypeBox);
      d.set(5, moveDispatchBox);
    });
    super.insertData(data, startIndex);
  }*/
}