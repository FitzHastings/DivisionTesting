package division.store;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.Vector;
import org.apache.commons.lang.ArrayUtils;

public class StoreRow extends Vector {
  private Integer[]    id        = new Integer[0];
  private String[]     hashcode  = new String[0];
  
  private Integer[]    store     = new Integer[0];
  private String[]     storeName = new String[0];
  
  private Integer[]    group     = new Integer[0];
  private String[]     groupName = new String[0];
  private String[]     groupUnit = new String[0];
  
  private BigDecimal[] nalichie  = new BigDecimal[0];
  private BigDecimal[] dostupno  = new BigDecimal[0];
  private BigDecimal[] reserve   = new BigDecimal[0];
  
  private TreeMap<Integer, Object>[] factorValues = new TreeMap[0];
  
  public void add(
          Integer    id, 
          String     hashcode, 
          
          Integer    store, 
          String     storeName, 
          
          Integer    group, 
          String     groupName, 
          String     groupUnit, 
          
          BigDecimal nalichie, 
          BigDecimal dostupno, 
          BigDecimal reserve) {
    
    this.id        = (Integer[])    ArrayUtils.add(this.id, id);
    this.hashcode  = (String[])     ArrayUtils.add(this.hashcode, hashcode);
    
    this.store     = (Integer[])    ArrayUtils.add(this.store, store);
    this.storeName = (String[])     ArrayUtils.add(this.storeName, storeName);
    
    this.group     = (Integer[])    ArrayUtils.add(this.group, group);
    this.groupName = (String[])     ArrayUtils.add(this.groupName, groupName);
    this.groupUnit = (String[])     ArrayUtils.add(this.groupUnit, groupUnit);
    
    this.nalichie  = (BigDecimal[]) ArrayUtils.add(this.nalichie, nalichie);
    this.dostupno  = (BigDecimal[]) ArrayUtils.add(this.dostupno, dostupno);
    this.reserve   = (BigDecimal[]) ArrayUtils.add(this.reserve, reserve);
  }
  
  public boolean containsId(Integer id) {
    return Arrays.binarySearch(this.id, id) >= 0;
  }
  
  public boolean containsHashcode(String hashcode) {
    return Arrays.binarySearch(this.hashcode, hashcode) >= 0;
  }
  
  public Object getFactorValue(Integer factorId) {
    Object value = null;
    for(int i=0;i<factorValues.length;i++) {
      if(value == null)
        value = factorValues[i].get(factorId);
      if(value != factorValues[i].get(factorId) || !value.equals(factorValues[i].get(factorId)))
        return null;
    }
    return value;
  }
  
  private Object get(Object[] arr) {
    if(arr == null || arr.length == 0)
      return null;
    Object o = arr[0];
    for(int i=1;i<arr.length;i++)
      if(!arr[i].equals(o))
        return null;
    return o;
  }
  
  private BigDecimal get(BigDecimal[] arr) {
    if(arr == null || arr.length == 0)
      return BigDecimal.ZERO;
    BigDecimal o = arr[0];
    for(int i=1;i<arr.length;i++)
      o = o.add(arr[i]);
    return o;
  }
  ///////////////////////////////////////////
  public String getHashCode() {
    return (String)get(hashcode);
  }
  
  public String getStoreName() {
    return (String)get(storeName);
  }
  
  public String getGroupName() {
    return (String)get(groupName);
  }
  
  public String getGroupUnit() {
    return (String)get(groupUnit);
  }
  
  public BigDecimal getNalichie() {
    return get(nalichie);
  }
  
  public BigDecimal getDostupno() {
    return get(dostupno);
  }
  
  public BigDecimal getReserve() {
    return get(reserve);
  }
  ///////////////////////////////////////////
  public Integer[] getIds() {
    return id;
  }

  public String[] getHashcode() {
    return hashcode;
  }

  public Integer[] getStores() {
    return store;
  }

  public String[] getStoreNames() {
    return storeName;
  }

  public Integer[] getGroups() {
    return group;
  }

  public String[] getGroupNames() {
    return groupName;
  }

  public String[] getGroupUnits() {
    return groupUnit;
  }

  public BigDecimal[] getNalichies() {
    return nalichie;
  }

  public BigDecimal[] getDostupnos() {
    return dostupno;
  }

  public BigDecimal[] getReserves() {
    return reserve;
  }
}