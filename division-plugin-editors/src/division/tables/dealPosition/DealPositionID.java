package division.tables.dealPosition;

import division.swing.DivisionComboBox;
import division.swing.DivisionItem;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Vector;
import org.apache.commons.lang.ArrayUtils;

public class DealPositionID {
  private final Integer id;
  private final Integer processId;
  private final Integer productId;
  private final Integer equipmentId;
  private final String hashCode;
  private final Long startId;
  private final Timestamp startDate;
  private final Long dispatchId;
  private final Timestamp dispatchDate;
  private final boolean objectMove;
  private DivisionItem[] targetStoreItems = new DivisionItem[0];
  
  private final Integer sourceStore;
  private final Integer targetStore;
  
  private static final String[] idColumns = new String[]{
    "id", 
    "process_id",
    "product",
    "equipment",
    "hashcode",
    "startId",
    "startDate",
    "dispatchId",
    "dispatchDate",
    "query:SELECT [Deal(object-move)] FROM [Deal] WHERE [Deal(id)]=[DealPosition(deal)]",
    "query: ARRAY(SELECT id||' - '||name FROM [Store] WHERE tmp=false AND type='CURRENT' AND [Store(companyPartition)]=[DealPosition(customer_partition_id)] AND "
          + "[Store(objectType)]=[DealPosition(target-store-object-type)] AND [Store(storeType)]=[DealPosition(target-store-type)])",
    /*"query:SELECT array_agg(a) FROM unnest(ARRAY(SELECT [Store(id)]||' - '||[Store(objectType)]||' - '||[Store(storeType)]||' - '||[Store(name)] FROM [Store] "
          + "WHERE tmp=false AND type='CURRENT' AND [Store(companyPartition)]=[DealPosition(customer_partition_id)] AND "
          + "[Store(objectType)]=[DealPosition(target-store-object-type)] AND "
          + "[Store(storeType)]=[DealPosition(target-store-type)])) a",*/
    "sourceStore",
    "targetStore"
  };

  public DealPositionID(Collection d) {
    this(new Vector(d));
  }

  public DealPositionID(Vector d) {
    this((Integer) d.get(0), (Integer) d.get(1), (Integer) d.get(2), (Integer) d.get(3), (String) d.get(4), (Long) d.get(5), (Timestamp) d.get(6), 
            (Long) d.get(7), (Timestamp) d.get(8), (boolean) d.get(9), (Object[]) d.get(10), (Integer) d.get(11), (Integer) d.get(12));
  }

  public DealPositionID(Integer id, Integer processId, Integer productId, Integer equipmentId, String hashCode, Long startId, Timestamp startDate, 
          Long dispatchId, Timestamp dispatchDate, boolean objectMove, Object[] storeItems, Integer sourceStore, Integer targetStore) {
    this.id           = id;
    this.processId    = processId;
    this.productId    = productId;
    this.equipmentId  = equipmentId;
    this.hashCode     = hashCode;
    this.startId      = startId;
    this.startDate    = startDate;
    this.dispatchId   = dispatchId;
    this.dispatchDate = dispatchDate;
    this.objectMove   = objectMove;
    if(storeItems != null) {
      String item;
      for(Object it:storeItems) {
        item = (String) it;
        this.targetStoreItems = (DivisionItem[]) ArrayUtils.add(this.targetStoreItems, 
                new DivisionItem(
                Integer.valueOf(item.substring(0, item.indexOf(" - "))), 
                item.substring(item.lastIndexOf(" - ")+3, item.length()-1), 
                        "Store"));
      }
    }
    this.sourceStore = sourceStore;
    this.targetStore = targetStore;
  }
  
  public DivisionComboBox createStoreCombobox() {
    DivisionComboBox storeCombobox = new DivisionComboBox(targetStoreItems);
    storeCombobox.setSelectedItem(targetStore);
    return storeCombobox;
  }

  public boolean isObjectMove() {
    return objectMove;
  }

  public Integer getTargetStore() {
    return targetStore;
  }

  public Integer getSourceStore() {
    return sourceStore;
  }
  
  public static String[] getIdColumns() {
    return idColumns;
  }
  
  public static int size() {
    return idColumns.length;
  }

  public Integer getId() {
    return id;
  }

  public Integer getProcessId() {
    return processId;
  }

  public Integer getProductId() {
    return productId;
  }

  public Integer getEquipmentId() {
    return equipmentId;
  }

  public String getHashCode() {
    return hashCode;
  }

  public Long getStartId() {
    return startId;
  }

  public Timestamp getStartDate() {
    return startDate;
  }

  public Long getDispatchId() {
    return dispatchId;
  }

  public Timestamp getDispatchDate() {
    return dispatchDate;
  }
}