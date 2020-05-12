package division.events;

import java.io.Serializable;
import java.util.Arrays;

public class ObjectEvent implements Serializable {
  private enum EventType{CREATE, UPDATE, DELETE};

  private String    sourceName;
  private EventType eventType;
  private Integer[] id;

  public ObjectEvent(String sourceName, EventType eventType, Integer[] id) {
    this.sourceName = sourceName;
    this.eventType = eventType;
    this.id = id;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null || getClass() != obj.getClass())
      return false;
    ObjectEvent other = (ObjectEvent)obj;
    if((this.sourceName == null) ? (other.sourceName != null) : !this.sourceName.equals(other.sourceName))
      return false;
    if(this.eventType != other.eventType)
      return false;
    if(!Arrays.deepEquals(this.id, other.id))
      return false;
    return true;
  }
}