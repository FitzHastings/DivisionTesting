package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToOne;
import bum.annotations.Table;
import bum.interfaces.People;
import bum.interfaces.StateUnit;
import bum.interfaces.StateUnitPeople;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table
public class StateUnitPeopleImpl extends MappingObjectImpl implements StateUnitPeople {
  @ManyToOne(nullable=false)
  private StateUnit stateUnit;
  
  @ManyToOne(nullable=false,viewFields={"surName","name","lastName"},viewNames={"people_surname","people_name","people_lastname"})
  private People people;
  
  @Column
  private Double count;
  
  public StateUnitPeopleImpl() throws RemoteException {
    super();
  }
  
  @Override
  public void setStateUnit(StateUnit stateUnit) throws RemoteException {
    this.stateUnit = (StateUnit)stateUnit;
  }

  @Override
  public StateUnit getStateUnit() throws RemoteException {
    return this.stateUnit;
  }

  @Override
  public void setPeople(People people) throws RemoteException {
    this.people = (People)people;
  }

  @Override
  public People getPeople() throws RemoteException {
    return this.people;
  }
  
  @Override
  public void setCount(Double count) throws RemoteException {
    this.count = count;
  }
  
  @Override
  public Double getCount() throws RemoteException {
    return this.count;
  }
}