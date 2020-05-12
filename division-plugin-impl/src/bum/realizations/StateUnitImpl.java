package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToOne;
import bum.annotations.OneToMany;
import bum.annotations.Table;
import bum.interfaces.Company;
import bum.interfaces.Department;
import bum.interfaces.StateUnit;
import bum.interfaces.StateUnitPeople;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;

@Table
public class StateUnitImpl extends MappingObjectImpl implements StateUnit {
  @Column
  private Double count;
  
  @ManyToOne(nullable=false)
  private Department department;
  
  @ManyToOne
  private Company company;
  
  @OneToMany(mappedBy="stateUnit")
  private List<StateUnitPeople> stateUnitPeoples = new ArrayList<>();
  
  public StateUnitImpl() throws RemoteException {
    super();
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
  public Company getCompany() throws RemoteException {
    return this.company;
  }
  
  @Override
  public void setCompany(Company company) throws RemoteException {
    this.company = (Company)company;
  }

  @Override
  public Department getDepartment() throws RemoteException {
    return this.department;
  }

  @Override
  public void setDepartment(Department department) throws RemoteException {
    this.department = (Department)department;
  }

  @Override
  public Double getCount() throws RemoteException {
    return this.count;
  }

  @Override
  public void setCount(Double count) throws RemoteException {
    this.count = count;
  }
}