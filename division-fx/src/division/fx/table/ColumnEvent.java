package division.fx.table;

import javafx.event.Event;
import javafx.event.EventType;

public class ColumnEvent extends Event {
  private final Column column;
  private final Object row;
  private final Object oldValue;
  private final Object newValue;

  public ColumnEvent(Column column, Object row, Object oldValue, Object newValue) {
    super(EventType.ROOT);
    this.column = column;
    this.row = row;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  public Column getColumn() {
    return column;
  }

  public Object getRow() {
    return row;
  }

  public Object getOldValue() {
    return oldValue;
  }

  public Object getNewValue() {
    return newValue;
  }
}