package bum.interfaces;

import java.rmi.RemoteException;
import java.sql.Date;
import java.util.List;
import mapping.MappingObject;

public interface People extends MappingObject {
  public String getSurName() throws RemoteException;
  public void setSurName(String surName) throws RemoteException;
  
  public String getLastName() throws RemoteException;
  public void setLastName(String lastName) throws RemoteException;
  
  public Date getBirthday() throws RemoteException;
  public void setBirthday(Date birthday) throws RemoteException;
  
  public String getNationality() throws RemoteException;
  public void setNationality(String nationality) throws RemoteException;
  
  public String getSerialPasport() throws RemoteException;
  public void setSerialPasport(String serialPasport) throws RemoteException;
  
  public String getNumberPasport() throws RemoteException;
  public void setNumberPasport(String numberPasport) throws RemoteException;
  
  public String getWhoTake() throws RemoteException;
  public void setWhoTake(String hwoTake) throws RemoteException;
  
  public Date getTakeDate() throws RemoteException;
  public void setTakeDate(Date takeDate) throws RemoteException;
  
  public String getCodeDepartament() throws RemoteException;
  public void setCodeDepartament(String codeDepartament) throws RemoteException;
  
  public String getRegistrationAddress() throws RemoteException;
  public void setRegistrationAddress(String registrationAddress) throws RemoteException;
  
  public String getPostAddress() throws RemoteException;
  public void setPostAddress(String postAddress) throws RemoteException;
  
  public String getHomeTelephon() throws RemoteException;
  public void setHomeTelephon(String homeTelephon) throws RemoteException;
  
  public String getMobileTelephon() throws RemoteException;
  public void setMobileTelephon(String mobileTelephon) throws RemoteException;
  
  public String getEmail() throws RemoteException;
  public void setEmail(String email) throws RemoteException;
  
  public Integer getNumber() throws RemoteException;
  public void setNumber(Integer number) throws RemoteException;
  
  public List<Worker> getWorkers() throws RemoteException;
  public void setWorkers(List<Worker> worker) throws RemoteException;
  
  public List<StateUnitPeople> getStateUnitPeoples() throws RemoteException;
  public void setStateUnitPeoples(List<StateUnitPeople> stateUnitPeoples) throws RemoteException;
}