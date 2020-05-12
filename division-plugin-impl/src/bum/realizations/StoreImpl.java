package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToOne;
import bum.annotations.OneToMany;
import bum.annotations.Table;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Group;
import bum.interfaces.Group.ObjectType;
import bum.interfaces.Store;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;

@Table
public class StoreImpl extends MappingObjectImpl implements Store {
  @Column(defaultValue = "false")
  private Boolean main;
  
  @ManyToOne
  private CompanyPartition companyPartition;
  
  @Column(nullable = false,  defaultValue = "ТМЦ")
  private ObjectType objectType;
  
  @Column(nullable = false, defaultValue = "НАЛИЧНЫЙ")
  private StoreType storeType;
  
  @Column(defaultValue = "false")
  private Boolean controllIn  = false;
  
  @Column(defaultValue = "false")
  private Boolean controllOut = false;
  
  @ManyToOne(description="Родительская группа", viewFields={"id"}, viewNames={"parent_id"})
  private Store parent;
  
  @OneToMany(description="Подчинённые группы",mappedBy="parent")
  private List<Store> childs = new ArrayList<>();
  
  @ManyToOne(viewFields = {"name"}, viewNames = {"currency-name"})
  private Group currency;
  
  public StoreImpl() throws RemoteException {
    super();
  }

  @Override
  public Group getCurrency() throws RemoteException {
    return currency;
  }

  @Override
  public void setCurrency(Group currency) throws RemoteException {
    this.currency = currency;
  }
  
  @Override
  public List<Store> getChilds() throws RemoteException {
    return childs;
  }

  @Override
  public void setChilds(List<Store> childs) throws RemoteException {
    this.childs = childs;
  }

  @Override
  public Store getParent() throws RemoteException  {
    return this.parent;
  }
  
  @Override
  public void setParent(Store parent) throws RemoteException {
    this.parent = parent;
  }

  @Override
  public Boolean isControllIn() {
    return controllIn;
  }

  @Override
  public void setControllIn(Boolean controllIn) {
    this.controllIn = controllIn;
  }

  @Override
  public Boolean isControllOut() {
    return controllOut;
  }

  @Override
  public void setControllOut(Boolean controllOut) {
    this.controllOut = controllOut;
  }

  @Override
  public Boolean isMain() throws RemoteException {
    return this.main;
  }

  @Override
  public void setMain(Boolean main) throws RemoteException {
    this.main = main;
  }

  @Override
  public CompanyPartition getCompanyPartition() throws RemoteException {
    return this.companyPartition;
  }

  @Override
  public void setCompanyPartition(CompanyPartition companyPartition) throws RemoteException {
    this.companyPartition = companyPartition;
  }

  @Override
  public StoreType getStoreType() throws RemoteException {
    return storeType;
  }

  @Override
  public void setStoreType(StoreType storeType) throws RemoteException {
    this.storeType = storeType;
  }

  @Override
  public ObjectType getObjectType() throws RemoteException {
    return this.objectType;
  }

  @Override
  public void setObjectType(ObjectType objectType) throws RemoteException {
    this.objectType = objectType;
  }
}