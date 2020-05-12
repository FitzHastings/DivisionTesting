package bum.realizations;

import bum.annotations.ManyToOne;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Employee;
import bum.interfaces.People;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

public class EmployeeImpl extends MappingObjectImpl implements Employee {
  @ManyToOne(viewFields = {"surName","name","lastName"}, viewNames = {"people-surName","people-name","people-lastName"})
  private People people;
  @ManyToOne
  private CompanyPartition partition;

  public EmployeeImpl() throws RemoteException {
    super();
  }

  @Override
  public People getPeople() {
    return people;
  }

  @Override
  public void setPeople(People people) {
    this.people = people;
  }

  @Override
  public CompanyPartition getPartition() {
    return partition;
  }

  @Override
  public void setPartition(CompanyPartition partition) {
    this.partition = partition;
  }
}