package bum.realizations;

import bum.annotations.ManyToOne;
import bum.annotations.Table;
import bum.interfaces.CFC;
import bum.interfaces.People;
import bum.interfaces.Worker;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table
public class WorkerImpl extends MappingObjectImpl implements Worker {
  @ManyToOne(viewFields={"id","name"},viewNames={"parent_id","cfc_name"},nullable=false)
  private CFC cfc;
  
  @ManyToOne(nullable=false,viewFields={"surName","name","lastName"},viewNames={"people_surname","people_name","people_lastname"})
  private People people;
  
  public WorkerImpl() throws RemoteException {
    super();
  }
  
  @Override
  public People getPeople() throws RemoteException {
    return people;
  }
  
  @Override
  public void setPeople(People people) throws RemoteException {
    this.people = people;
  }

  @Override
  public CFC getCfc() throws RemoteException {
    return this.cfc;
  }

  @Override
  public void setCfc(CFC cfc) throws RemoteException {
    this.cfc = cfc;
  }
}