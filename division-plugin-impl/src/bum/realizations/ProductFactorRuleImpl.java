package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToOne;
import bum.annotations.Table;
import bum.interfaces.Factor;
import bum.interfaces.Product;
import bum.interfaces.ProductFactorRule;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table
public class ProductFactorRuleImpl extends MappingObjectImpl implements ProductFactorRule {
  @ManyToOne(nullable=false)
  private Product product;

  @ManyToOne(nullable=false,
          viewFields={"id","name","unit","factorType","listValues","unique"},
          viewNames={"factor_id","factor_name","factor_unit","factor_factorType","factor_listValues","factor_unique"})
  private Factor factor;

  @Column(name="condition_")
  private String condition = "=";

  @Column
  private String parameter;

  @Column()
  private String operator;

  @Column()
  private Double meaning;
  
  public ProductFactorRuleImpl() throws RemoteException {
    super();
  }

  /*@Override
  public Object[] getRow() throws RemoteException {
    Session session = new Session();
    session.begin();
    
    ProductFactorRule productFactorValue = (ProductFactorRule)getThis();
    productFactorValue.setSession(session);
    
    Factor factor;
    ProductFactor productFactor_ = productFactorValue.getProductFactor();
    factor = productFactor_.getFactor();
    if(factor == null)
      factor = productFactor_.getGroupFactor().getFactor();
    Object[] row = new Object[5];
    row[0] = getId();
    if(factor.getListValues() != null) {
      row[1] = "=";
      String[] vals = factor.getListValues().split(";");
      String[] values = Arrays.copyOf(vals, vals.length+1);
      values[values.length-1] = productFactorValue.getParameter();
      row[2] = values;
    }else {
      row[1] = new String[]{"=",">","<",">=","<=",productFactorValue.getCondition()};
      row[2] = productFactorValue.getParameter();
    }
    row[3] = new String[]{"+","-","*","/",productFactorValue.getOperator()};
    row[4] = productFactorValue.getMeaning();
    
    session.commit();
    return row;
  }*/
  
  @Override
  public Product getProduct() throws RemoteException {
    return product;
  }

  @Override
  public void setProduct(Product product) throws RemoteException {
    this.product = product;
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
  public String getCondition() throws RemoteException {
    return this.condition;
  }

  @Override
  public void setCondition(String condition) throws RemoteException {
    this.condition = condition;
  }

  @Override
  public String getParameter() throws RemoteException {
    return this.parameter;
  }

  @Override
  public void setParameter(String parameter) throws RemoteException {
    this.parameter = parameter;
  }

  @Override
  public void setOperator(String operator) throws RemoteException {
    this.operator = operator;
  }

  @Override
  public String getOperator() throws RemoteException {
    return this.operator;
  }

  @Override
  public void setMeaning(Double meaning) throws RemoteException {
    this.meaning = meaning;
  }

  @Override
  public Double getMeaning() throws RemoteException {
    return this.meaning;
  }
}