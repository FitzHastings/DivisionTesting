package division.fx.table;

import java.util.Objects;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Pojo {
  private final IntegerProperty id    = new SimpleIntegerProperty();
  private final StringProperty  name  = new SimpleStringProperty();

  public Pojo() {
  }
  
  public Pojo(Integer id, String name) {
    this.id.setValue(id);
    this.name.setValue(name);
  }

  public Integer getId() {
    return id.getValue();
  }

  public void setId(Integer id) {
    this.id.setValue(id);
  }
  
  public IntegerProperty idProperty() {
    return id;
  }

  public String getName() {
    return name.getValue();
  }

  public void setName(String name) {
    this.name.setValue(name);
  }
  
  public StringProperty nameProperty() {
    return name;
  }

  @Override
  public String toString() {
    return name.getValue();
  }

  @Override
  public boolean equals(Object obj) {
    return !(obj == null || getClass() != obj.getClass() || !Objects.equals(this.id.get(), ((Pojo) obj).id.getValue()));
  }
}