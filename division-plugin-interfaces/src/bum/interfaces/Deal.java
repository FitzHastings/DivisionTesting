package bum.interfaces;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import mapping.MappingObject;

public interface Deal extends MappingObject {
  public Period getDuration() throws RemoteException;
  public void setDuration(Period duration) throws RemoteException;

  public Period getRecurrence() throws RemoteException;
  public void setRecurrence(Period recurrence) throws RemoteException;
  
  public ContractProcess getTempProcess() throws RemoteException;
  public void setTempProcess(ContractProcess tempProcess) throws RemoteException;
  
  public Service getService() throws RemoteException;
  public void setService(Service service) throws RemoteException;

  public List<DealPayment> getDealPayments() throws RemoteException;
  public void setDealPayments(List<DealPayment> dealPayments) throws RemoteException;

  public CompanyPartition getCustomerCompanyPartition() throws RemoteException;
  public void setCustomerCompanyPartition(CompanyPartition customerCompanyPartition) throws RemoteException;

  public CompanyPartition getSellerCompanyPartition() throws RemoteException;
  public void setSellerCompanyPartition(CompanyPartition sellerCompanyPartition) throws RemoteException;

  public Date getDealEndDate() throws RemoteException;
  public void setDealEndDate(Date dealEndDate) throws RemoteException;
  
  public Date getDealStartDate() throws RemoteException;
  public void setDealStartDate(Date dealStartDate) throws RemoteException;

  public List<DealPosition> getDealPositions() throws RemoteException;
  public void setDealPositions(List<DealPosition> dealPositions) throws RemoteException;
  
  public Contract getContract() throws RemoteException;
  public void setContract(Contract contract) throws RemoteException;

  //public Deal copyDeal(RemoteSession session, Date start, Date end) throws Exception;
  
  public CFC getSellerCfc() throws RemoteException;
  public void setSellerCfc(CFC sellerCfc) throws RemoteException;
  
  public CFC getCustomerCfc() throws RemoteException;
  public void setCustomerCfc(CFC customerCfc) throws RemoteException;
  
  public Company getSellerCompany() throws RemoteException;
  public void setSellerCompany(Company sellerCompany) throws RemoteException;
  
  public Company getCustomerCompany() throws RemoteException;
  public void setCustomerCompany(Company customerCompany) throws RemoteException;
  
  public static Color getColor(int startCount, int dispatchCount, BigDecimal dispatchAmount, int paymentCount, BigDecimal paymentAmount, BigDecimal cost) {
    Color color = Color.LIGHT_GRAY;
    
    if(startCount > 0) {
      if(dispatchCount > 0 && cost.compareTo(BigDecimal.ZERO) == 0 || dispatchCount > 0 && paymentCount > 0 && dispatchAmount.compareTo(paymentAmount) == 0)
        color = Color.GREEN;
      else if(dispatchCount == 0 && cost.compareTo(BigDecimal.ZERO) == 0 || dispatchAmount.compareTo(paymentAmount) < 0)
        color = Color.BLUE;
      else if(dispatchAmount.compareTo(paymentAmount) > 0)
        color = Color.RED;
      else color = Color.YELLOW;
    }
        
    return color;
  }
  
  public static Color getColor(int count, int startCount, int dispatchCount, int paymentCount, BigDecimal paymentAmount, BigDecimal cost) {
    Color color = Color.BLACK;
    
    BigDecimal bCount         = new BigDecimal(count);
    BigDecimal bStartCount    = new BigDecimal(startCount);
    BigDecimal bDispatchCount = new BigDecimal(dispatchCount);
    
    BigDecimal startPercent    = cost.compareTo(BigDecimal.ZERO)==0?BigDecimal.ZERO:bStartCount.divide(bCount, 2, RoundingMode.HALF_UP);
    BigDecimal dispatchPercent = cost.compareTo(BigDecimal.ZERO)==0?BigDecimal.ZERO:bDispatchCount.divide(bCount, 2, RoundingMode.HALF_UP);
    BigDecimal paymentPercent  = cost.compareTo(BigDecimal.ZERO)==1&&paymentAmount.compareTo(BigDecimal.ZERO)==1 ? 
            paymentAmount.divide(cost, MathContext.DECIMAL128) : 
            (cost.compareTo(BigDecimal.ZERO)==0&&paymentAmount.compareTo(BigDecimal.ZERO)==0&&paymentCount==0?BigDecimal.ONE:BigDecimal.ZERO);
    
    if(startPercent.compareTo(BigDecimal.ONE) == 0 && dispatchPercent.compareTo(BigDecimal.ONE) == 0 && paymentPercent.compareTo(BigDecimal.ONE) == 0)
      color = Color.GREEN;
    if(startPercent.compareTo(BigDecimal.ONE) == 0 && dispatchPercent.compareTo(BigDecimal.ONE) == 0 && paymentPercent.compareTo(BigDecimal.ZERO) == 0)
      color = Color.RED;
    if(startPercent.compareTo(BigDecimal.ONE) == 0 && dispatchPercent.compareTo(BigDecimal.ZERO) == 0 && paymentPercent.compareTo(BigDecimal.ONE) == 0)
      color = Color.BLUE;
    if(startPercent.compareTo(BigDecimal.ONE) == 0 && dispatchPercent.compareTo(BigDecimal.ZERO) == 0 && paymentPercent.compareTo(BigDecimal.ZERO) == 0)
      color = Color.YELLOW;
    if(startPercent.compareTo(BigDecimal.ZERO) == 0 && dispatchPercent.compareTo(BigDecimal.ZERO) == 0 && paymentPercent.compareTo(BigDecimal.ZERO) == 0)
      color = Color.LIGHT_GRAY;
    
    return color;
  }
  
  public static LocalDate getEndDeal(LocalDate start, Period dur) {
    LocalDate end = null;
    if(dur.getDays() > 0)
      end = start.plusDays(dur.getDays());
    if(dur.getMonths() > 0) {
      end = start.plusMonths(dur.getMonths()-1);
      end = LocalDate.of(end.getYear(), end.getMonthValue(), end.lengthOfMonth());
    }
    if(dur.getYears() > 0) {
      end = start.plusYears(dur.getYears()-1);
      end = LocalDate.of(end.getYear(), 12, end.lengthOfMonth());
    }
    return end;
  }
  
  public static LocalDate getStartDeal(LocalDate start, Period rec) {
    if(rec.getDays() > 0)
      start = start.plusDays(rec.getDays()+1);
    if(rec.getMonths() > 0)
      start = LocalDate.of(start.getYear(), start.getMonthValue(), 1).plusMonths(rec.getMonths());
    if(rec.getYears() > 0)
      start = LocalDate.of(start.getYear(), 1, 1).plusYears(rec.getYears());
    return start;
  }
}