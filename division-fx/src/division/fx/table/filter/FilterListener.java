package division.fx.table.filter;

import division.util.IDStore;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;

public interface FilterListener extends Comparable<FilterListener>, EventHandler {
  public long getId();
  
  public static FilterListener create(EventHandler handler) {
    return new FilterListener() {
      private long id = IDStore.createID();
      
      @Override
      public long getId() {
        return id;
      }

      @Override
      public int compareTo(FilterListener o) {
        return getId() == o.getId() ? 0 : getId() > o.getId() ? 1: -1;
      }

      @Override
      public void handle(Event event) {
        Platform.runLater(() -> handler.handle(event));
      }
    };
  }
}