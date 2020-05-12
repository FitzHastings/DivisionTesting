package bum.realizations;

import bum.annotations.ManyToOne;
import bum.annotations.OneToMany;
import bum.annotations.Table;
import bum.interfaces.Company;
import bum.interfaces.Department;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;

@Table
public class DepartmentImpl extends MappingObjectImpl implements Department {
  @ManyToOne(nullable=false)
  private Company company;
  
  @ManyToOne(viewFields={"id"}, viewNames={"parent_id"})
  private Department parent;
  
  @OneToMany(mappedBy="parent")
  private List<Department> childs = new ArrayList<>();
  
  public DepartmentImpl() throws RemoteException {
    super();
  }
  
  @Override
  public Department getParent() throws RemoteException {
    return this.parent;
  }
  
  @Override
  public void setParent(Department parent) throws RemoteException {
    this.parent = parent;
  }

  @Override
  public Company getCompany() throws RemoteException {
    return this.company;
  }

  @Override
  public void setCompany(Company company) throws RemoteException {
    this.company = company;
  }

  @Override
  public List<Department> getChilds() throws RemoteException {
    return childs;
  }

  @Override
  public void setChilds(List<Department> childs) throws RemoteException {
    this.childs = childs;
  }
}