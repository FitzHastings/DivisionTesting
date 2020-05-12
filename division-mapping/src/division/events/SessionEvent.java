package division.events;

import java.io.Serializable;

public class SessionEvent implements Serializable {
  private enum EventType{COMMIT, ROLLBACK};

  private String    sourceName;
  private EventType eventType;
}