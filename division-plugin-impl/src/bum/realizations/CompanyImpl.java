package bum.realizations;

import bum.annotations.*;
import bum.interfaces.*;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;
import util.DBRelation;

@Table(
  clientName="Предприятия",
  history=true/*,
  
  queryColumns={
    @QueryColumn(
        name="seller",
        desctiption="Наименование продавца",
        query="SELECT getCompanyPartitionName([Contract(sellerCompanyPartition)])")
  }*/)

@Procedures(
  procedures={
    @Procedure(
        name="getCompanyName",
        arguments={"integer"},
        procedureText=""
        + "DECLARE\n"
        + "  ownershipType text;\n"
        + "BEGIN\n"
        + "  SELECT [OwnershipType(name)] INTO ownershipType FROM [OwnershipType] \n"
        + "WHERE [OwnershipType(id)]=(SELECT [Company(ownershipType)] FROM [Company] WHERE [Company(id)]=$1);\n"
        + "  IF ownershipType IS NULL THEN ownershipType := ''; ELSE ownershipType := ownershipType||' '; END IF;\n"
        + "  RETURN (SELECT ownershipType||[Company(name)] FROM [Company] WHERE [Company(id)]=$1);\n"
        + "END\n"
        )})

@Triggers(triggers = {
  /*@Trigger(timeType=Trigger.TIMETYPE.AFTER,
  actionTypes={Trigger.ACTIONTYPE.UPDATE},
  procedureText=
    "DECLARE "
    + "BEGIN"
    + "  IF NEW.type != OLD.type THEN"
    + "    UPDATE [CompanyPartition] SET type=NEW.type WHERE [CompanyPartition(company)]=OLD.id;"
    + "  END IF;"
    + "  RETURN NEW;"
    + "END;"),*/
  @Trigger(timeType=Trigger.TIMETYPE.AFTER,
        actionTypes={Trigger.ACTIONTYPE.INSERT},
        procedureText=
        "DECLARE "
        + "BEGIN"
        + "  INSERT INTO [CompanyPartition]([!CompanyPartition(name)],[!CompanyPartition(company)],[!CompanyPartition(mainPartition)]) "
        + "  VALUES ('Основное подразделение',NEW.id,true);"
        + "RETURN NEW;"
        + "END;")
})

public class CompanyImpl extends MappingObjectImpl implements Company {
  @Column(description="Краткое наименование",index=true,length=-1)
  private String shotName;
  
  @ManyToOne(on_delete = DBRelation.ActionType.SET_NULL, description="Должность руководителя",viewFields={"name"},viewNames={"chifPlaceName"})
  private Place chiefPlace;
  
  @ManyToOne(on_delete = DBRelation.ActionType.SET_NULL, description="Должность фторого лица",viewFields={"name"},viewNames={"secondPlaceName"})
  private Place secondPerson;

  @Column(description="Ф.И.О. руководителя")
  private String chiefName;

  @ManyToOne(description="Форма собственности",viewFields={"name"},viewNames={"ownership"})
  private OwnershipType ownershipType;

  @Column(description="ИНН")
  private String inn;

  @Column(view=false,description="Основание действий")
  private String businessReason;

  @Column(view=false,description="В лице")
  private String businessPerson;

  @Column(description="ОГРН")
  private String ogrn;

  @Column(description="ОКВЕД")
  private String okved;

  @ManyToMany(mappedBy="companys",description="ЦФУ")
  private List<CFC> cfcs = new ArrayList<>();

  @OneToMany(mappedBy="company",description="Подразделения")
  private List<CompanyPartition> companyPartitions = new ArrayList<>();

  @Column
  private String bookkeeper;
  
  @Column(defaultValue="18")
  private BigDecimal defaultNds = new BigDecimal(18.0);
  
  @Column(defaultValue = "true")
  private Boolean ndsPayer = true;
  
  @Column
  private byte[] logo = new byte[0];
  
  @Column
  private byte[] stamp = new byte[0];
  
  @Column
  private byte[] chifSignature = new byte[0];
  
  @Column
  private byte[] bookkeeperSignature = new byte[0];
  
  public CompanyImpl() throws RemoteException {
    super();
  }
  
  @Override
  public byte[] getLogo() throws RemoteException {
    return logo;
  }
  
  @Override
  public void setLogo(byte[] logo) throws RemoteException {
    this.logo = logo;
  }

  @Override
  public byte[] getStamp() throws RemoteException {
    return stamp;
  }

  @Override
  public void setStamp(byte[] stamp) throws RemoteException {
    this.stamp = stamp;
  }

  @Override
  public byte[] getChifSignature() throws RemoteException {
    return chifSignature;
  }

  @Override
  public void setChifSignature(byte[] chifSignature) throws RemoteException {
    this.chifSignature = chifSignature;
  }

  @Override
  public byte[] getBookkeeperSignature() throws RemoteException {
    return bookkeeperSignature;
  }

  @Override
  public void setBookkeeperSignature(byte[] bookkeeperSignature) throws RemoteException {
    this.bookkeeperSignature = bookkeeperSignature;
  }

  @Override
  public Boolean isNdsPayer() throws RemoteException {
    return ndsPayer;
  }

  @Override
  public void setNdsPayer(Boolean ndsPayer) throws RemoteException {
    this.ndsPayer = ndsPayer;
  }

  @Override
  public String getShotName() throws RemoteException {
    return shotName;
  }

  @Override
  public void setShotName(String shotName) throws RemoteException {
    this.shotName = shotName;
  }

  @Override
  public BigDecimal getDefaultNds() throws RemoteException {
    return defaultNds;
  }

  @Override
  public void setDefaultNds(BigDecimal defaultNds) throws RemoteException {
    this.defaultNds = defaultNds;
  }

  @Override
  public String getBookkeeper() throws RemoteException {
    return bookkeeper;
  }

  @Override
  public void setBookkeeper(String bookkeeper) throws RemoteException {
    this.bookkeeper = bookkeeper;
  }

  @Override
  public String getChiefName() throws RemoteException {
    return chiefName;
  }

  @Override
  public void setChiefName(String chiefName) throws RemoteException {
    this.chiefName = chiefName;
  }

  @Override
  public Place getChiefPlace() throws RemoteException {
    return chiefPlace;
  }

  @Override
  public void setChiefPlace(Place chiefPlace) throws RemoteException {
    this.chiefPlace = chiefPlace;
  }

  public Place getSecondPerson() {
    return secondPerson;
  }

  public void setSecondPerson(Place secondPerson) {
    this.secondPerson = secondPerson;
  }

  @Override
  public String getOgrn() throws RemoteException {
    return ogrn;
  }

  @Override
  public void setOgrn(String ogrn) throws RemoteException {
    this.ogrn = ogrn;
  }

  @Override
  public String getOkved() throws RemoteException {
    return okved;
  }
  
  @Override
  public void setOkved(String okved) throws RemoteException {
    this.okved = okved;
  }
  
  @Override
  public List<CompanyPartition> getCompanyPartitions() throws RemoteException {
    return companyPartitions;
  }
  
  @Override
  public void setCompanyPartitions(List<CompanyPartition> companyPartitions) throws RemoteException {
    this.companyPartitions = companyPartitions;
  }
  
  @Override
  public OwnershipType getOwnershipType() throws RemoteException {
    return ownershipType;
  }
  
  @Override
  public void setOwnershipType(OwnershipType ownershipType) throws RemoteException {
    this.ownershipType = ownershipType;
  }
  
  @Override
  public void setBusinessReason(String businessReason) throws RemoteException {
    this.businessReason = businessReason;
  }
  
  @Override
  public String getBusinessReason() throws RemoteException {
    return this.businessReason;
  }

  @Override
  public void setInn(String inn) throws RemoteException {
    this.inn = inn;
  }
  
  @Override
  public String getInn() throws RemoteException {
    return this.inn;
  }
  
  @Override
  public List<CFC> getCfcs() throws RemoteException {
    return cfcs;
  }
  
  @Override
  public void setCfcs(List<CFC> cfcs) throws RemoteException {
    this.cfcs = cfcs;
  }
  
  @Override
  public void addCfcs(List<CFC> cfcs) throws RemoteException {
    this.cfcs.addAll(cfcs);
  }
  
  @Override
  public void removeCfcs(List<CFC> cfcs) throws RemoteException {
    this.cfcs.removeAll(cfcs);
  }
  
  @Override
  public String getBusinessPerson() throws RemoteException {
    return this.businessPerson;
  }
  
  @Override
  public void setBusinessPerson(String businessPerson) throws RemoteException {
    this.businessPerson = businessPerson;
  }
}