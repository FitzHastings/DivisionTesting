package division.mail;

public interface EmailListener {
  public default void documentsSent(Integer[] ids) {};
  public default void XMLSent() {};
}