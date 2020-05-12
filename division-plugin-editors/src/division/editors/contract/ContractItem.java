package division.editors.contract;

import bum.interfaces.Contract;
import division.swing.DivisionItem;
import division.util.Utility;
import java.rmi.RemoteException;
import mapping.MappingObject;

public class ContractItem extends DivisionItem {
  public ContractItem() {
    super();
  }

  @Override
  public void setObject(MappingObject object) throws RemoteException {
    super.setObject(object);
    name = "Договор "+((Contract)object).getTemplate().getName()+" № "+((Contract)object).getNumber()+" от "+Utility.format(((Contract)object).getDate());
  }
}