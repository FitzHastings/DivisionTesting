package division;

public interface ClientMainListener {
  public default void changedCFC(Integer[] ids) {};
  public default void changedCompany(Integer[] ids) {};
}