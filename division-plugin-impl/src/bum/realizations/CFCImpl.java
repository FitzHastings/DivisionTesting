package bum.realizations;

import bum.annotations.ManyToMany;
import bum.annotations.ManyToOne;
import bum.annotations.OneToMany;
import bum.annotations.Table;
import bum.interfaces.CFC;
import bum.interfaces.Company;
import bum.interfaces.Worker;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mapping.MappingObjectImpl;

@Table(clientName="Центры финансового учёта",history=true)
public class CFCImpl extends MappingObjectImpl implements CFC {
  @ManyToOne(description="Родительская группа", viewFields={"id"}, viewNames={"parent_id"})
  private CFC parent;
  
  @OneToMany(description="Подчинённые группы",mappedBy="parent")
  private List<CFC> childs = new ArrayList<>();
  
  @OneToMany(description="Участники ЦФУ",mappedBy="cfc")
  private List<Worker> users = new ArrayList<>();
  
  @ManyToMany(description="Предприятия")
  private List<Company> companys = new ArrayList<>();
  
  public CFCImpl() throws RemoteException {
    super();
  }

  @Override
  public List<CFC> getChilds() throws RemoteException {
    return childs;
  }

  @Override
  public void setChilds(List<CFC> childs) throws RemoteException {
    this.childs = childs;
  }

  @Override
  public CFC getParent() throws RemoteException  {
    return this.parent;
  }
  
  @Override
  public void setParent(CFC parent) throws RemoteException {
    this.parent = parent;
  }

  @Override
  public List<Worker> getUsers() throws RemoteException {
    return Arrays.asList(users.toArray(new Worker[this.users.size()]));
  }

  @Override
  public void setUsers(List<Worker> users) throws RemoteException {
    this.users = Arrays.asList(users.toArray(new Worker[users.size()]));
  }

  @Override
  public void addUsers(List<Worker> users) throws RemoteException {
    this.users.addAll(Arrays.asList(users.toArray(new Worker[users.size()])));
  }

  @Override
  public void removeUsers(List<Worker> users) throws RemoteException {
    this.users.removeAll(Arrays.asList(users.toArray(new Worker[users.size()])));
  }
  
  @Override
  public List<Company> getCompanys() throws RemoteException {
    return this.companys;
  }

  @Override
  public void setCompanys(List companys) throws RemoteException {
    this.companys = companys;
  }

  @Override
  public void addCompanys(List companys) throws RemoteException {
    this.companys.addAll(companys);
  }

  @Override
  public void removeCompanys(List companys) throws RemoteException {
    this.companys.removeAll(companys);
  }
}