package bum.realizations;

import bum.annotations.*;
import bum.interfaces.Product;
import bum.interfaces.Service;
import bum.interfaces.XMLContractTemplate;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;

@Table(history=true)

@Triggers(triggers={
  @Trigger(
        timeType=Trigger.TIMETYPE.AFTER,
        actionTypes={Trigger.ACTIONTYPE.UPDATE},
        procedureText=""
        + "DECLARE "
        + "BEGIN"
        + "  IF NEW.tmp != OLD.tmp THEN"
        + "    UPDATE [Service] SET tmp=NEW.tmp WHERE [Service(parent)]=NEW.id;"
        + "    UPDATE [Product] SET tmp=NEW.tmp WHERE [Product(service)]=ANY(getServiceChilds(OLD.id));"
        + "  END IF;"
        + "  IF NEW.type != OLD.type THEN"
        + "    UPDATE [Service] SET type=NEW.type WHERE [Service(parent)]=NEW.id;"
        + "    UPDATE [Product] SET type=NEW.type WHERE [Product(service)]=ANY(getServiceChilds(OLD.id));"
        + "  END IF;"
        + "  RETURN NEW;"
        + "END;"
        )
})

@Procedures(procedures={
  @Procedure(
        name="getServiceChilds",
        arguments={"integer"},
        returnType="integer[]",
        procedureText=
          "DECLARE"
        + "  parentId     integer:=$1;"
        + "  childs       integer[];"
        + "  ids          record;"
        + "BEGIN"
        + "  FOR ids IN SELECT id FROM [Service] WHERE [Service(parent)]=parentId "
        + "  LOOP"
        + "    SELECT array_append(childs, ids.id) INTO childs;"
        + "    SELECT array_cat(childs,getServiceChilds(ids.id)) INTO childs;"
        + "  END LOOP;"
        + "  RETURN childs;"
        + "END;"
        )})

public class ServiceImpl extends MappingObjectImpl implements Service {
  @ManyToOne(viewFields={"id"}, viewNames={"parent_id"})
  private Service parent;
  
  @OneToMany(mappedBy="parent")
  private List<Service> childs = new ArrayList<>();
  
  @OneToMany(mappedBy="service")
  private List<Product> products = new ArrayList<>();
  
  @Column(defaultValue="CUSTOMER")
  private Owner owner = Owner.CUSTOMER;
  
  @Column(defaultValue="false", nullable = false)
  private Boolean moveStorePosition = false;
  
  @ManyToMany(mappedBy = "processes")
  private List<XMLContractTemplate> contractTemplates = new ArrayList<>();

  public ServiceImpl() throws RemoteException {
    super();
  }

  @Override
  public List<XMLContractTemplate> getContractTemplates() throws RemoteException {
    return contractTemplates;
  }

  @Override
  public void setContractTemplates(List<XMLContractTemplate> contractTemplates) throws RemoteException {
    this.contractTemplates = contractTemplates;
  }

  @Override
  public Boolean isMoveStorePosition() throws RemoteException {
    return moveStorePosition;
  }

  @Override
  public void setMoveStorePosition(Boolean moveStorePosition) throws RemoteException {
    this.moveStorePosition = moveStorePosition;
  }
  
  @Override
  public Owner getOwner() throws RemoteException {
    return owner;
  }

  @Override
  public void setOwner(Owner owner) throws RemoteException {
    this.owner = owner;
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
   public Service getParent() throws RemoteException {
    return this.parent;
  }
  
  @Override
  public void setParent(Service parent) throws RemoteException {
    this.parent = parent;
  }

  @Override
  public List<Service> getChilds() throws RemoteException {
    return childs;
  }

  @Override
  public void setChilds(List<Service> childs) throws RemoteException {
    this.childs = childs;
  }
}