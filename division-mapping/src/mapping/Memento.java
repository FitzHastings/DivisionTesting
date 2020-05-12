package mapping;

import java.rmi.Remote;

public interface Memento extends Remote {
  public Object getFieldValue(String fieldName) throws Exception;
}