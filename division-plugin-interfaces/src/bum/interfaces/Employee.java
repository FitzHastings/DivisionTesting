package bum.interfaces;

import mapping.MappingObject;

public interface Employee extends MappingObject {
  public People getPeople();
  public void setPeople(People people);
  public CompanyPartition getPartition();
  public void setPartition(CompanyPartition partition);
}