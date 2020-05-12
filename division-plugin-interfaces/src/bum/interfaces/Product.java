package bum.interfaces;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.time.Period;
import java.util.List;
import mapping.MappingObject;

public interface Product extends MappingObject {
  public enum CostType{CUSTOM_COST, PERCENT_OF_MEAN_PRIME_COST, PERCENT_OF_MAX_PRIME_COST}
  
  public Company getCompany() throws RemoteException;
  public void setCompany(Company company) throws RemoteException;
  
  public CostType getCostType() throws RemoteException;
  public void setCostType(CostType costType) throws RemoteException;
  
  public Integer[] getFormula() throws RemoteException;
  public void setFormula(Integer[] formula) throws RemoteException;
  
  public String getTechPassport() throws RemoteException;
  public void setTechPassport(String techPassport) throws RemoteException;
  
  public List<Factor> getFactors() throws RemoteException;
  public void setFactors(List<Factor> factors) throws RemoteException;

  public void addFactors(List<Factor> factors) throws RemoteException;
  public void removeFactors(List<Factor> factors) throws RemoteException;

  public Product getGlobalProduct() throws RemoteException;
  public void setGlobalProduct(Product globalProduct) throws RemoteException;

  public Period getDuration() throws RemoteException;
  public void setDuration(Period duration) throws RemoteException;

  public Period getRecurrence() throws RemoteException;
  public void setRecurrence(Period recurrence) throws RemoteException;

  public Boolean isPeriod() throws RemoteException;
  public void setPeriod(Boolean period) throws RemoteException;

  public BigDecimal getNds() throws RemoteException;
  public void setNds(BigDecimal nds) throws RemoteException;

  public PriceList getPriceList() throws RemoteException;
  public void setPriceList(PriceList priceList) throws RemoteException;
  
  public Service getService() throws RemoteException;
  public void setService(Service service) throws RemoteException;
  
  public Group getGroup() throws RemoteException;
  public void setGroup(Group group) throws RemoteException;
  
  public BigDecimal getCost() throws RemoteException;
  public void setCost(BigDecimal cost) throws RemoteException;
  
  public String getCurrency() throws RemoteException;
  public void setCurrency(String currency) throws RemoteException;
  
  public Boolean isFirstService() throws RemoteException;
  public void setFirstService(Boolean service) throws RemoteException;

  public boolean equals(Service service, Group group) throws RemoteException;
}