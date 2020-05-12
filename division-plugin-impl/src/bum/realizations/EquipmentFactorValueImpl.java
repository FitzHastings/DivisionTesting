package bum.realizations;

import bum.annotations.ManyToOne;
import bum.annotations.QueryColumn;
import bum.annotations.Table;
import bum.annotations.UnicumFields;
import bum.interfaces.Equipment;
import bum.interfaces.EquipmentFactorValue;
import bum.interfaces.Factor;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table(
  unicumFields={
    @UnicumFields(fields={"equipment","factor","tmp","type"})
  },
  history=true,
  queryColumns={
    @QueryColumn(
      name="company_name",
      query="SELECT name FROM [bum.interfaces.Company] "
          + "WHERE [bum.interfaces.Company(id)]=(SELECT [bum.interfaces.CompanyPartition(company)] FROM [bum.interfaces.CompanyPartition] "
          + "WHERE [bum.interfaces.CompanyPartition(id)]=[bum.interfaces.Equipment(companyPartition)])"),
    @QueryColumn(
      name="identity_id",
      query="SELECT [bum.interfaces.Group(identificator)] FROM [bum.interfaces.Group] "
          + "WHERE [bum.interfaces.Group(id)]=[bum.interfaces.Equipment(group)]"),
    @QueryColumn(
      name="identity_name",
      query="SELECT [bum.interfaces.Factor(name)] FROM [bum.interfaces.Factor] "
          + "WHERE [bum.interfaces.Factor(id)]=(SELECT [bum.interfaces.Group(identificator)] FROM [bum.interfaces.Group] "
          + "WHERE [bum.interfaces.Group(id)]=[bum.interfaces.Equipment(group)])"),
    @QueryColumn(
      name="identity_value_id",
      query="SELECT id FROM [bum.interfaces.EquipmentFactorValue] "
          + "WHERE [bum.interfaces.EquipmentFactorValue(equipment)]=[bum.interfaces.Equipment(id)] AND "
          + "[bum.interfaces.EquipmentFactorValue(factor)]="
          + "(SELECT [bum.interfaces.Group(identificator)] FROM [bum.interfaces.Group] "
          + "WHERE [bum.interfaces.Group(id)]=[bum.interfaces.Equipment(group)])"),
    @QueryColumn(
      name="identity_value_name",
      query="SELECT name FROM [bum.interfaces.EquipmentFactorValue] "
          + "WHERE [bum.interfaces.EquipmentFactorValue(equipment)]=[bum.interfaces.Equipment(id)] AND "
          + "[bum.interfaces.EquipmentFactorValue(factor)]="
          + "(SELECT [bum.interfaces.Group(identificator)] FROM [bum.interfaces.Group] "
          + "WHERE [bum.interfaces.Group(id)]=[bum.interfaces.Equipment(group)])"),
    @QueryColumn(
        name="factor_identity",
        query="SELECT (NULLTOZERO((SELECT [bum.interfaces.Group(identificator)] FROM [bum.interfaces.Group] "
        + "WHERE [bum.interfaces.Group(id)]=(SELECT [bum.interfaces.Equipment(group)] FROM [bum.interfaces.Equipment] "
        + "WHERE [bum.interfaces.Equipment(id)]=[bum.interfaces.EquipmentFactorValue(equipment)])))=[bum.interfaces.EquipmentFactorValue(factor)])")})

public class EquipmentFactorValueImpl extends MappingObjectImpl implements EquipmentFactorValue {
  @ManyToOne(nullable=false,
          viewFields={"id","name","unit","factorType","unique","listValues","tmp","type"},
          viewNames={"factor_id","factor_name","factor_unit","factor_factorType","factor_unique","factor_listValues","factor_tmp","factor_type"})
  private Factor factor;

  @ManyToOne(nullable=false,viewFields={"id", "group"},viewNames={"equipment_id", "group"})
  private Equipment equipment;
  
  public EquipmentFactorValueImpl() throws RemoteException {
    super();
  }

  @Override
  public boolean checkValue(String value) throws RemoteException {
    return true;
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
  public Equipment getEquipment() throws RemoteException {
    return this.equipment;
  }

  @Override
  public void setEquipment(Equipment equipment) throws RemoteException {
    this.equipment = (Equipment)equipment;
  }
}