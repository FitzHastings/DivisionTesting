package division.store;

import java.math.BigDecimal;
import org.apache.commons.lang.ArrayUtils;

public class StoreId {
  private Integer[] ids;
  private Integer group;
  private Integer store;
  private String hashCode;

  private BigDecimal reserve;
  private BigDecimal dostupno;
  
  private Integer[] factors;

  public StoreId(Integer id, Integer store, Integer group, String hashCode, Integer[] factors, BigDecimal reserve, BigDecimal dostupno) {
    this(new Integer[]{id}, store, group, hashCode, factors, reserve, dostupno);
  }

  public StoreId(Integer[] ids, Integer store, Integer group, String hashCode, Integer[] factors, BigDecimal reserve, BigDecimal dostupno) {
    this.ids      = ids;
    this.hashCode = hashCode;
    this.store    = store;
    this.group    = group;
    this.factors = factors;
    this.reserve  = reserve;
    this.dostupno = dostupno;
  }
  
  public BigDecimal getReserve() {
    return reserve;
  }

  public BigDecimal getDostupno() {
    return dostupno;
  }

  public Integer[] getFactors() {
    return factors;
  }

  public Integer getGroup() {
    return group;
  }

  public Integer getStore() {
    return store;
  }

  public Integer[] getIds() {
    return ids;
  }

  public void setIds(Integer[] ids) {
    this.ids = ids;
  }

  public String getHashCode() {
    return hashCode;
  }

  public void addId(Integer id) {
    ids = (Integer[]) ArrayUtils.add(ids, id);
  }
  
  public void removeId(Integer id) {
    ids = (Integer[]) ArrayUtils.removeElement(ids, id);
  }

  public boolean contains(Integer id) {
    return ArrayUtils.contains(ids, id);
  }

  public boolean contains(String hashCode) {
    return this.hashCode.equals(hashCode);
  }
  
  public boolean containsFactor(Integer factorId) {
    return ArrayUtils.contains(factors, factorId);
  }
}