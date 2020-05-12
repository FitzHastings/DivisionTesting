package division.store;

import bum.interfaces.Factor;
import java.io.Serializable;
import java.util.Objects;

public class FactorColumn implements Serializable, Comparable<FactorColumn> {
  private final Integer id;
  private final String  name;
  private final Factor.FactorType  factorType;
  private final String             listValues;
  private final boolean productFactor;

  public FactorColumn(Integer id, String name, Factor.FactorType factorType, String listValues, boolean productFactor) {
    this.id = id;
    this.name = name;
    this.factorType = factorType;
    this.listValues = listValues;
    this.productFactor = productFactor;
  }

  public boolean isProductFactor() {
    return productFactor;
  }

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Factor.FactorType getFactorType() {
    return factorType;
  }

  public String getListValues() {
    return listValues;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 37 * hash + Objects.hashCode(this.id);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final FactorColumn other = (FactorColumn) obj;
    if (!Objects.equals(this.id, other.id)) {
      return false;
    }
    return true;
  }

  @Override
  public int compareTo(FactorColumn o) {
    return o==null||getId()>o.getId()?-1:getId()<o.getId()?1:0;
  }
}