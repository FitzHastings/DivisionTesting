package bum.interfaces;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.List;

public interface Group extends RMIDBNodeObject<Group> {
  public enum ObjectType{ТМЦ, ВАЛЮТА, ЦЕННАЯ_БУМАГА, НЕМАТЕРИАЛЬНЫЙ_АКТИВ}
  
  public BigDecimal getCost() throws RemoteException;
  public void setCost(BigDecimal cost) throws RemoteException;
  
  public Boolean isMain() throws RemoteException;
  public void setMain(Boolean main) throws RemoteException;
  
  public ObjectType getGroupType() throws RemoteException;
  public void setGroupType(ObjectType groupType) throws RemoteException;
  
  public List<Equipment> getEquipments() throws RemoteException;
  public void setEquipments(List<Equipment> equipments) throws RemoteException;

  public List<Product> getProducts() throws RemoteException;
  public void setProducts(List<Product> products) throws RemoteException;

  public void addChilds(List<RMIDBNodeObject> childs) throws RemoteException;

  public void setUnit(Unit unit) throws RemoteException;
  public Unit getUnit() throws RemoteException;
  
  public Factor getIdentificator() throws RemoteException;
  public void setIdentificator(Factor identificator) throws RemoteException;
  
  public List<Factor> getFactors() throws RemoteException;
  public void setFactors(List<Factor> factors) throws RemoteException;
  public void addFactors(List<Factor> factors) throws RemoteException;
  public void removeFactors(List<Factor> factors) throws RemoteException;

  public String getBarcode() throws RemoteException;
  public void setBarcode(String barcode) throws RemoteException;

  public String getDescription() throws RemoteException;
  public void setDescription(String description) throws RemoteException;

  public byte[] getImage() throws RemoteException;
  public void setImage(byte[] image) throws RemoteException;
}