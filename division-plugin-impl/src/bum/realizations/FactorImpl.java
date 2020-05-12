package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToMany;
import bum.annotations.ManyToOne;
import bum.annotations.Table;
import bum.interfaces.Factor;
import bum.interfaces.Group;
import bum.interfaces.Product;
import bum.interfaces.Service;
import bum.interfaces.Unit;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;

@Table(history=true)
public class FactorImpl extends MappingObjectImpl implements Factor {
  @ManyToOne(viewFields = {"name"},viewNames = {"unit-name"})
  private Unit unit;

  @Column(defaultValue="число")
  private FactorType factorType = Factor.FactorType.число;

  @Column(length=1000)
  private String listValues;  

  @Column(nullable=false, name="unique_", defaultValue="false")
  private Boolean unique;

  @ManyToMany(mappedBy="factors")
  private List<Group> groups = new ArrayList<>();

  @ManyToMany(mappedBy="factors")
  private List<Product> products = new ArrayList<>();
  
  @ManyToMany(mappedBy="factors")
  private List<Service> processes = new ArrayList<>();
  
  @Column(defaultValue="false")
  private Boolean productFactor = false;
  
  public FactorImpl() throws RemoteException {
    super();
  }

  @Override
  public Boolean isProductFactor() throws RemoteException {
    return productFactor;
  }

  @Override
  public void setProductFactor(Boolean productFactor) throws RemoteException {
    this.productFactor = productFactor;
  }

  @Override
  public List<Group> getGroups() throws RemoteException {
    return groups;
  }

  @Override
  public void setGroups(List<Group> groups) throws RemoteException {
    this.groups = groups;
  }

  @Override
  public List<Product> getProducts() throws RemoteException {
    return products;
  }

  @Override
  public void setProducts(List<Product> products) throws RemoteException {
    this.products = products;
  }

  @Override
  public List<Service> getProcesses() throws RemoteException {
    return processes;
  }

  @Override
  public void setProcesses(List<Service> processes) throws RemoteException {
    this.processes = processes;
  }

  @Override
  public Boolean isUnique() throws RemoteException {
    return this.unique;
  }

  @Override
  public void setUnique(Boolean unique) throws RemoteException {
    this.unique = unique;
  }
  
  @Override
  public void setUnit(Unit unit) throws RemoteException {
    this.unit = unit;
  }

  @Override
  public Unit getUnit() throws RemoteException {
    return this.unit;
  }
  
  @Override
  public void setListValues(String listValues) throws RemoteException {
    this.listValues = listValues;
  }

  @Override
  public String getListValues() throws RemoteException {
    return this.listValues;
  }
  
  @Override
  public FactorType getFactorType() throws RemoteException {
    return this.factorType;
  }
  
  @Override
  public void setFactorType(FactorType factorType) throws RemoteException {
    this.factorType = factorType;
  }
}