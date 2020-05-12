package division.fx.table;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.control.Cell;
import javafx.stage.Popup;

public interface DivisionCellEditor<T> {
  public void            setContent(Parent content);
  public Parent          getContent();
  public ObjectProperty<T>  dataPpoperty();
  public ObjectProperty<T>  resultPpoperty();
  public Cell<T>         getCell();
  public void            setCell(Cell<T> cell);
  public void            setPopup(Popup pop);
  public Popup           getPopup();
  
  public static DivisionCellEditor create(Parent content) {
    return create(content, e -> {
      if(((DivisionCellEditor)e.getSource()).getCell() != null)
        ((DivisionCellEditor)e.getSource()).getCell().cancelEdit();
    });
  }
  
  public static <T> DivisionCellEditor create(Parent content, EventHandler actionWhenHiding) {
    System.out.println("DivisionCellEditor");
    
    DivisionCellEditor<T> divisionCellEditor =  new DivisionCellEditor() {
      private Parent content;
      private Cell cell;
      private Popup pop;
      private ObjectProperty<T>  data    = new SimpleObjectProperty();
      private ObjectProperty<T>  result  = new SimpleObjectProperty();

      @Override
      public Popup getPopup() {
        return pop;
      }

      @Override
      public void setPopup(Popup pop) {
        this.pop = pop;
        this.pop.setOnHiding(e -> actionWhenHiding.handle(new Event(this, e.getTarget(), e.getEventType())));
      }

      @Override
      public Parent getContent() {
        return content;
      }

      @Override
      public ObjectProperty<T> dataPpoperty() {
        return data;
      }

      @Override
      public ObjectProperty<T> resultPpoperty() {
        return result;
      }

      @Override
      public void setContent(Parent content) {
        this.content = content;
      }
      
      @Override
      public void setCell(Cell cell) {
        this.cell = cell;
      }

      @Override
      public Cell getCell() {
        return cell;
      }
    };
    
    divisionCellEditor.setContent(content);
    return divisionCellEditor;
  }
}