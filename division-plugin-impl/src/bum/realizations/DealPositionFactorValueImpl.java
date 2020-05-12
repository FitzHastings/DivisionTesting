package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToOne;
import bum.annotations.QueryColumn;
import bum.annotations.Table;
import bum.interfaces.DealPosition;
import bum.interfaces.DealPositionFactorValue;
import bum.interfaces.Factor;
import java.rmi.RemoteException;
import java.sql.SQLException;
import mapping.MappingObjectImpl;

@Table(
  queryColumns={
    @QueryColumn(
      name="group_id",
      desctiption="Идентификатор группы",
      query="SELECT [Equipment(group)] FROM [Equipment] WHERE [Equipment(id)]="
        + "(SELECT [DealPosition(equipment)] FROM [DealPosition] "
        + "WHERE [DealPosition(id)]=[DealPositionFactorValue(dealPosition)])")}
)

public class DealPositionFactorValueImpl extends MappingObjectImpl implements DealPositionFactorValue {
  @ManyToOne(nullable=false,
          viewFields={"id","name","unit","factorType","identity","unique","listValues"},
          viewNames={"factor_id","factor_name","factor_unit","factor_factorType","factor_identity","factor_unique","factor_listValues"})
  private Factor factor;
  
  @ManyToOne(nullable=false,viewFields={"id","equipment","deal"},viewNames={"dealposition_id","equipment_id","deal_id"})
  public DealPosition dealPosition;
  
  @Column(defaultValue="true")
  private Boolean active = true;
  
  public DealPositionFactorValueImpl() throws RemoteException {
    super();
  }
  
  @Override
  public Boolean isActive() throws RemoteException {
    return this.active;
  }

  @Override
  public void setActive(Boolean active) throws RemoteException {
    this.active = active;
  }

  @Override
  public DealPosition getDealPosition() throws RemoteException {
    return this.dealPosition;
  }

  @Override
  public void setDealPosition(DealPosition dealPosition) throws RemoteException {
    this.dealPosition = dealPosition;
  }
  
  @Override
  public Factor getFactor() throws RemoteException {
    return factor;
  }

  @Override
  public void setFactor(Factor factor) throws RemoteException {
    this.factor = factor;
  }
  
  @Override
  public Double getCost() throws RemoteException {
    return 0.0;
  }

  @Override
  public Double getCostQuery() throws RemoteException, SQLException {
    double cost = 0.0;
    /*try {
      RMITable table    = DBTableFactory.getInstance().get(DealPositionProductFactorValue.class);
      RMITable dpTable  = DBTableFactory.getInstance().get(DealPosition.class);
      RMITable pfTable  = DBTableFactory.getInstance().get(ProductFactor.class);
      RMITable pfvTable = DBTableFactory.getInstance().get(ProductFactorRule.class);
      RMITable gfTable  = DBTableFactory.getInstance().get(GroupFactor.class);
      RMITable gfvTable = DBTableFactory.getInstance().get(GroupFactorValue.class);
      RMITable fTable   = DBTableFactory.getInstance().get(Factor.class);

      RemoteSession session = getSession();
      Vector<Vector> rez = session.executeQuery("SELECT "+table.getColumnName("dealPosition")+"," +
        table.getColumnName("productFactor")+","+table.getColumnName("productFactorValue")+"," +
        "(SELECT "+dpTable.getColumnName("equipment")+" FROM "+dpTable.getTableName()+
        " WHERE id="+table.getColumnName("dealPosition")+") AS equipment_id," +
        "(SELECT "+pfTable.getColumnName("factor")+" FROM "+pfTable.getTableName()+
        " WHERE id="+table.getColumnName("productFactor")+") AS factor_id," +
        "(SELECT "+pfTable.getColumnName("groupFactor")+" FROM "+pfTable.getTableName()+
        " WHERE id="+table.getColumnName("productFactor")+") AS group_factor_id " +
        "FROM "+table.getTableName()+" WHERE id="+getId());

      Integer productFactorId      = (Integer)rez.get(0).get(1);
      Integer productFactorValueId = (Integer)rez.get(0).get(2);
      Integer equipmentId          = (Integer)rez.get(0).get(3);
      Integer groupFactorId        = (Integer)rez.get(0).get(5);

      if(groupFactorId != null) {
        rez = session.executeQuery("SELECT id,name FROM "+gfvTable.getTableName()+
          " WHERE "+gfvTable.getColumnName("equipment")+"="+equipmentId+
          " AND "+gfvTable.getColumnName("groupFactor")+"="+groupFactorId);
        if(!rez.isEmpty()) {
          String groupFactorValueName  = (String)rez.get(0).get(1);
          TreeMap<Double,Integer> treeMap = new TreeMap<Double, Integer>();
          rez = session.executeQuery("SELECT id,"+pfvTable.getColumnName("parameter")+"," +
            pfvTable.getColumnName("condition")+" FROM "+pfvTable.getTableName()+
            " WHERE "+pfvTable.getColumnName("productFactor")+"="+productFactorId);
          for(Vector pfv:rez) {
            if(pfv.get(2).equals(">"))
              if(Double.parseDouble(groupFactorValueName) > ((Double)pfv.get(1)).doubleValue())
                treeMap.put(Double.parseDouble(groupFactorValueName) - ((Double)pfv.get(1)).doubleValue(), (Integer)pfv.get(0));
            if(pfv.get(2).equals("<"))
              if(Double.parseDouble(groupFactorValueName) < ((Double)pfv.get(1)).doubleValue())
                treeMap.put(Double.parseDouble(groupFactorValueName) - ((Double)pfv.get(1)).doubleValue(), (Integer)pfv.get(0));
            if(pfv.get(2).equals(">="))
              if(Double.parseDouble(groupFactorValueName) >= ((Double)pfv.get(1)).doubleValue())
                treeMap.put(0.0, (Integer)pfv.get(0));
            if(pfv.get(2).equals("<="))
              if(Double.parseDouble(groupFactorValueName) <= ((Double)pfv.get(1)).doubleValue())
                treeMap.put(0.0, (Integer)pfv.get(0));
            if(pfv.get(2).equals("=")) {
              String type = (String)session.executeQuery("SELECT id,"+fTable.getColumnName("factorType")+" FROM "+fTable.getTableName()+
                " WHERE id=(SELECT "+gfTable.getColumnName("factor")+" FROM "+gfTable.getTableName()+
                " WHERE id="+groupFactorId+")").get(0).get(0);
              if(type.equals("NUMBER")) {
                if(Double.parseDouble(groupFactorValueName) == ((Double)pfv.get(1)).doubleValue())
                  treeMap.put(0.0, (Integer)pfv.get(0));
              }else {
                if(groupFactorValueName.equals(pfv.get(1)))
                  treeMap.put(0.0, (Integer)pfv.get(0));
              }
            }
          }
          if(treeMap.firstEntry() != null)
            productFactorValueId = treeMap.firstEntry().getValue();
        }
      }
      if(productFactorValueId != null) {
        rez = session.executeQuery("SELECT "+pfvTable.getColumnName("operator")+
          ","+pfvTable.getColumnName("meaning")+" FROM "+pfvTable.getTableName()+
          " WHERE id="+productFactorValueId);
        cost = Double.parseDouble((String)rez.get(0).get(0)+rez.get(0).get(1));
      }
    }catch(SQLException ex) {
      throw new SQLException(ex.getMessage());
    }
    System.out.println("DEALPOSITIONPRODUCTFACTORVALUE GETCOST END");
    System.out.println("--------------COST = "+cost);*/
    return cost;
  }

  /*@Override
  public Object[] getRow() throws RemoteException {
    DealPositionProductFactorValue dealPositionProductFactorValue = 
            (DealPositionProductFactorValue)getThis();
    
    Object[] row = new Object[5];
    row[0] = dealPositionProductFactorValue.getId();
    row[1] = dealPositionProductFactorValue.isActive();
    
    DealPosition  dealPosition_  = dealPositionProductFactorValue.getDealPosition();
    Equipment     equipment      = dealPosition_.getEquipment();
    Factor        factor         = productFactor_.getFactor();
    GroupFactor   groupFactor    = productFactor_.getGroupFactor();
    
    List<ProductFactorRule> productFactorValues = productFactor_.getProductFactorValues();
    ProductFactorRule productFactorValue_ = dealPositionProductFactorValue.getProductFactorValue();
    if(groupFactor != null) {
      factor = groupFactor.getFactor();
      for(GroupFactorValue gfv:equipment.getGroupFactorValues()) {
        if(groupFactor.equals(gfv.getGroupFactor()) && gfv.getName() != null) {
          TreeMap<Double,ProductFactorRule> treeMap = new TreeMap<Double, ProductFactorRule>();
          for(ProductFactorRule pfv:productFactorValues) {
            if(pfv.getCondition().equals(">")) {
              if(Double.valueOf(gfv.getName()).doubleValue() > Double.valueOf(pfv.getParameter()).doubleValue()) {
                treeMap.put(Double.valueOf(gfv.getName()).doubleValue()-Double.valueOf(pfv.getParameter()).doubleValue(), pfv);
              }
            }
            if(pfv.getCondition().equals("<")) {
              if(Double.valueOf(gfv.getName()).doubleValue() < Double.valueOf(pfv.getParameter()).doubleValue()) {
                treeMap.put(Double.valueOf(pfv.getParameter()).doubleValue()-Double.valueOf(gfv.getName()).doubleValue(), pfv);
              }
            }
            if(pfv.getCondition().equals(">=")) {
              if(Double.valueOf(gfv.getName()).doubleValue() >= Double.valueOf(pfv.getParameter()).doubleValue()) {
                treeMap.put(0d, pfv);
              }
            }
            if(pfv.getCondition().equals("<=")) {
              if(Double.valueOf(gfv.getName()).doubleValue() <= Double.valueOf(pfv.getParameter()).doubleValue()) {
                treeMap.put(0d, pfv);
              }
            }
            if(pfv.getCondition().equals("=")) {
              if(factor.getFactorType() != Factor.FactorType.число) {
                if(gfv.getName().equals(pfv.getParameter())) {
                  treeMap.put(0d, pfv);
                }
              }else if(Double.valueOf(gfv.getName()).doubleValue() == Double.valueOf(pfv.getParameter()).doubleValue()) {
                treeMap.put(0d, pfv);
              }
            }
          }
          if(treeMap.firstEntry() != null)
            productFactorValue_ = treeMap.firstEntry().getValue();
          row[3] = gfv.getName();
          break;
        }
      }
    }else {
      Object[] values = new Object[productFactorValues.size()+1];
      for(int i=0;i<productFactorValues.size();i++)
        values[i] = new Object[]{productFactorValues.get(i).getParameter()+" "+factor.getUnit(),productFactorValues.get(i).getId()};
      values[values.length-1] = productFactorValue_==null?null:productFactorValue_.getParameter()+" "+factor.getUnit();
      row[3] = values;
    }
    row[4] = productFactorValue_==null?null:productFactorValue_.getOperator()+productFactorValue_.getMeaning();
    row[2] = factor.getName();
    return row;
  }*/
}