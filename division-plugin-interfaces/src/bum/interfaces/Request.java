package bum.interfaces;

import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.List;
import mapping.MappingObject;

public interface Request extends MappingObject {
  public String getNumber() throws RemoteException;
  public void setNumber(String number) throws RemoteException;
  
  public List<Equipment> getEquipments() throws RemoteException;
  public void setEquipments(List<Equipment> equipments) throws RemoteException;
  
  public LocalDateTime getStartDate() throws RemoteException;
  public void setStartDate(LocalDateTime startDate) throws RemoteException;

  public LocalDateTime getAcceptDate() throws RemoteException;
  public void setAcceptDate(LocalDateTime acceptDate) throws RemoteException;

  public LocalDateTime getExecutDate() throws RemoteException;
  public void setExecutDate(LocalDateTime executDate) throws RemoteException;

  public LocalDateTime getFinishDate() throws RemoteException;
  public void setFinishDate(LocalDateTime finishDate) throws RemoteException;

  public LocalDateTime getExitDate() throws RemoteException;
  public void setExitDate(LocalDateTime exitDate) throws RemoteException;
  
  public Worker getPerformerWorker() throws RemoteException;
  public void setPerformerWorker(Worker performerWorker) throws RemoteException;
  
  public Company getApplicant() throws RemoteException;
  public void setApplicant(Company applicant) throws RemoteException;

  public CFC getPerformer() throws RemoteException;
  public void setPerformer(CFC performer) throws RemoteException;

  public String getReason() throws RemoteException;
  public void setReason(String reason) throws RemoteException;
}