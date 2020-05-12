package bum.realizations;

import bum.annotations.*;
import bum.interfaces.*;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mapping.MappingObjectImpl;

@Table(
  name = "group_of_equipment",
  history=true,
  queryColumns={
    @QueryColumn(
      name="equipment_count",
      query="SELECT COUNT([bum.interfaces.Equipment(id)]) FROM [bum.interfaces.Equipment] WHERE "
          + "[bum.interfaces.Equipment(group)]=[bum.interfaces.Group(id)] AND "
          + "[bum.interfaces.Equipment(tmp)]=false")})

@Triggers(triggers={
  @Trigger(
        timeType=Trigger.TIMETYPE.AFTER,
        actionTypes={Trigger.ACTIONTYPE.UPDATE},
        procedureText=""
        + "DECLARE "
        + "BEGIN"
        + "  IF NEW.tmp != OLD.tmp THEN"
        + "    UPDATE [Group] SET tmp=NEW.tmp WHERE [Group(parent)]=NEW.id;"
        + "    UPDATE [Product] SET tmp=NEW.tmp WHERE [Product(group)]=ANY(getGroupChilds(OLD.id));"
        + "    UPDATE [Equipment] SET tmp=NEW.tmp WHERE [Equipment(group)]=ANY(getGroupChilds(OLD.id));"
        + "  END IF;"
        + "  IF NEW.type != OLD.type THEN"
        + "    UPDATE [Group] SET type=NEW.type WHERE [Group(parent)]=NEW.id;"
        + "    UPDATE [Product] SET type=NEW.type WHERE [Product(group)]=ANY(getGroupChilds(OLD.id));"
        + "    UPDATE [Equipment] SET type=NEW.type WHERE [Equipment(group)]=ANY(getGroupChilds(OLD.id));"
        + "  END IF;"
        + "  RETURN NEW;"
        + "END;"
        )
})

@Procedures(procedures={
  @Procedure(
        name="getGroupChilds",
        arguments={"integer"},
        returnType="integer[]",
        procedureText=
          "DECLARE"
        + "  parentId     integer:=$1;"
        + "  childs       integer[];"
        + "  ids          record;"
        + "BEGIN"
        + "  FOR ids IN SELECT id FROM [Group] WHERE [Group(parent)]=parentId "
        + "  LOOP"
        + "    SELECT array_append(childs, ids.id) INTO childs;"
        + "    SELECT array_cat(childs,getGroupChilds(ids.id)) INTO childs;"
        + "  END LOOP;"
        + "  RETURN childs;"
        + "END;"
        )})

public class GroupImpl extends MappingObjectImpl implements Group {
  @ManyToOne(viewFields={"id"},viewNames={"parent_id"})
  private Group parent;
  
  @ManyToOne(viewFields = {"name"},viewNames = {"unit-name"})
  private Unit unit;
  
  @OneToMany(mappedBy="parent")
  private List<Group> childs = new ArrayList<>();
  
  @OneToMany(mappedBy="group")
  private List<Equipment> equipments = new ArrayList<>();
  
  @OneToMany(mappedBy="group")
  private List<Product> products = new ArrayList<>();

  @ManyToMany
  private List<Factor> factors = new ArrayList<>();
  
  @ManyToOne(viewFields={"id","name","factorType","unit","listValues"}, viewNames={"iden_id","iden_name","iden_factorType","iden_unit","iden_listValue"})
  private Factor identificator;

  @Column(defaultValue="")
  private String barcode;

  @Column(defaultValue="")
  private String description;

  @Column
  private byte[] image = new byte[0];
  
  @Column(defaultValue = "ТМЦ")
  private ObjectType groupType = ObjectType.ТМЦ;
  
  @Column(defaultValue = "1.00", nullable = false)
  private BigDecimal cost;
  
  @Column(defaultValue = "false", nullable = false)
  private Boolean main;

  public GroupImpl() throws RemoteException {
    super();
  }
  
  @Override
  public BigDecimal getCost() throws RemoteException {
    return cost;
  }
  
  @Override
  public void setCost(BigDecimal cost) throws RemoteException {
    this.cost = cost;
  }
  
  @Override
  public Boolean isMain() throws RemoteException {
    return main;
  }
  
  @Override
  public void setMain(Boolean main) throws RemoteException {
    this.main = main;
  }

  @Override
  public ObjectType getGroupType() throws RemoteException {
    return groupType;
  }

  @Override
  public void setGroupType(ObjectType groupType) throws RemoteException {
    this.groupType = groupType;
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
  public Factor getIdentificator() throws RemoteException {
    return identificator;
  }

  @Override
  public void setIdentificator(Factor identificator) throws RemoteException {
    this.identificator = identificator;
  }

  @Override
  public List<Factor> getFactors() throws RemoteException {
    return factors;
  }

  @Override
  public void setFactors(List<Factor> factors) throws RemoteException {
    this.factors = factors;
  }

  @Override
  public void addFactors(List<Factor> factors) throws RemoteException {
    this.factors.addAll(factors);
  }

  @Override
  public void removeFactors(List<Factor> factors) throws RemoteException {
    this.factors.removeAll(factors);
  }

  @Override
  public String getBarcode() throws RemoteException {
    return barcode;
  }

  @Override
  public void setBarcode(String barcode) throws RemoteException {
    this.barcode = barcode;
  }

  @Override
  public String getDescription() throws RemoteException {
    return description;
  }

  @Override
  public void setDescription(String description) throws RemoteException {
    this.description = description;
  }

  @Override
  public byte[] getImage() throws RemoteException {
    return image;
  }

  @Override
  public void setImage(byte[] image) throws RemoteException {
    this.image = image;
  }

  @Override
  public List<Equipment> getEquipments() throws RemoteException {
    return equipments;
  }

  @Override
  public void setEquipments(List<Equipment> equipments) throws RemoteException {
    this.equipments = equipments;
  }

  @Override
  public Group getParent() throws RemoteException {
    return parent;
  }
  
  @Override
  public void setParent(Group parent) throws RemoteException {
    this.parent = parent;
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
  public List<Group> getChilds() throws RemoteException {
    return childs;
  }

  @Override
  public void setChilds(List<Group> childs) throws RemoteException {
    this.childs = childs;
  }
  
  @Override
  public void addChilds(List<RMIDBNodeObject> childs) throws RemoteException {
    this.childs.addAll(Arrays.asList(childs.toArray(new Group[childs.size()])));
  }
}