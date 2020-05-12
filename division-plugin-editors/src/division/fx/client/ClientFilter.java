package division.fx.client;

import javafx.beans.property.*;

public class ClientFilter {
  private static final ObjectProperty<Integer[]> CFC       = new SimpleObjectProperty();
  private static final ObjectProperty<Integer[]> company   = new SimpleObjectProperty();
  private static final ObjectProperty<Integer[]> partition = new SimpleObjectProperty();

  private ClientFilter() {}

  public static ObjectProperty<Integer[]> CFCProperty() {
    return CFC;
  }

  public static ObjectProperty<Integer[]> companyProperty() {
    return company;
  }

  public static ObjectProperty<Integer[]> partitionProperty() {
    return partition;
  }
}