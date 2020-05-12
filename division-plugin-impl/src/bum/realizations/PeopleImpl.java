package bum.realizations;

import bum.annotations.Column;
import bum.annotations.OneToMany;
import bum.annotations.Table;
import bum.interfaces.People;
import bum.interfaces.StateUnitPeople;
import bum.interfaces.Worker;
import java.rmi.RemoteException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;

@Table
public class PeopleImpl extends MappingObjectImpl implements People {
  @Column(description="Фамилия")
  private String surName;
  
  @Column(description="Отчество")
  private String lastName;
  
  @Column(description="Дата рождения")
  private Date birthday;
  
  @Column(description="Гражданство")
  private String nationality;
  
  @Column(description="Серия паспорта")
  private String serialPasport;
  
  @Column(description="Номер паспорта")
  private String numberPasport;
  
  @Column(description="Кем выдан паспорт")
  private String whoTake;
  
  @Column(description="Дата выдачи паспорта")
  private Date takeDate;
  
  @Column(description="Код подразделения выдавшего паспорт")
  private String codeDepartament;
  
  @Column(description="Адрес регистрации")
  private String registrationAddress;
  
  @Column(description="Адрес фактического проживания")
  private String postAddress;
  
  @Column(description="Домашний телефон")
  private String homeTelephon;
  
  @Column(description="Мобильный телефон")
  private String mobileTelephon;
  
  @Column(description="Адрес электронной почты")
  private String email;
  
  @Column(description="Номер")
  private Integer number;
  
  @OneToMany(mappedBy="people", updateOnChanged=true)
  private List<Worker> workers = new ArrayList<>();
    
  @OneToMany(mappedBy="people")
  private List<StateUnitPeople> stateUnitPeoples = new ArrayList<>();

  public PeopleImpl() throws RemoteException {
    super();
  }
  
  @Override
  public void setSurName(String surName) throws RemoteException {
    this.surName = surName;
  }
  @Override
  public String getSurName() throws RemoteException {
    return this.surName;
  }
  
  @Override
  public void setLastName(String lastName) throws RemoteException {
    this.lastName = lastName;
  }
  @Override
  public String getLastName() throws RemoteException {
    return this.lastName;
  }
  
  @Override
  public void setBirthday(Date birthday) throws RemoteException {
    this.birthday = birthday;
  }
  @Override
  public Date getBirthday() throws RemoteException {
    return this.birthday;
  }
  
  @Override
  public void setNationality(String nationality) throws RemoteException {
    this.nationality = nationality;
  }
  @Override
  public String getNationality() throws RemoteException {
    return this.nationality;
  }
  
  @Override
  public void setSerialPasport(String serialPasport) throws RemoteException {
    this.serialPasport = serialPasport;
  }
  @Override
  public String getSerialPasport() throws RemoteException {
    return this.serialPasport;
  }
  
  @Override
  public void setNumberPasport(String numberPasport) throws RemoteException {
    this.numberPasport = numberPasport;
  }
  @Override
  public String getNumberPasport() throws RemoteException {
    return this.numberPasport;
  }
  
  @Override
  public void setWhoTake(String whoTake) throws RemoteException {
    this.whoTake = whoTake;
  }
  @Override
  public String getWhoTake() throws RemoteException {
    return this.whoTake;
  }
  
  @Override
  public void setTakeDate(Date takeDate) throws RemoteException {
    this.takeDate = takeDate;
  }
  @Override
  public Date getTakeDate() throws RemoteException {
    return this.takeDate;
  }
  
  @Override
  public void setCodeDepartament(String codeDepartament) throws RemoteException {
    this.codeDepartament = codeDepartament;
  }
  @Override
  public String getCodeDepartament() throws RemoteException {
    return this.codeDepartament;
  }
  
  @Override
  public void setRegistrationAddress(String registrationAddress) throws RemoteException {
    this.registrationAddress = registrationAddress;
  }
  @Override
  public String getRegistrationAddress() throws RemoteException {
    return this.registrationAddress;
  }
  
  @Override
  public void setPostAddress(String postAddress) throws RemoteException {
    this.postAddress = postAddress;
  }
  @Override
  public String getPostAddress() throws RemoteException {
    return this.postAddress;
  }
  
  @Override
  public void setHomeTelephon(String homeTelephon) throws RemoteException {
    this.homeTelephon = homeTelephon;
  }
  @Override
  public String getHomeTelephon() throws RemoteException {
    return this.homeTelephon;
  }
  
  @Override
  public void setMobileTelephon(String mobileTelephon) throws RemoteException {
    this.mobileTelephon = mobileTelephon;
  }
  @Override
  public String getMobileTelephon() throws RemoteException {
    return this.mobileTelephon;
  }
  
  @Override
  public void setEmail(String email) throws RemoteException {
    this.email = email;
  }
  @Override
  public String getEmail() throws RemoteException {
    return this.email;
  }
  
  @Override
  public void setNumber(Integer number) throws RemoteException {
    this.number = number;
  }

  @Override
  public Integer getNumber() throws RemoteException {
    return this.number;
  }

  @Override
  public List<StateUnitPeople> getStateUnitPeoples() throws RemoteException {
    return stateUnitPeoples;
  }

  @Override
  public void setStateUnitPeoples(List<StateUnitPeople> stateUnitPeoples) throws RemoteException {
    this.stateUnitPeoples = stateUnitPeoples;
  }

  @Override
  public List<Worker> getWorkers() throws RemoteException
  {
    return workers;
  }

  @Override
  public void setWorkers(List<Worker> workers) throws RemoteException
  {
    this.workers = workers;
  }
}