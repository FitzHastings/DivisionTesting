package bum.realizations;

import bum.annotations.ManyToOne;
import bum.annotations.OneToMany;
import bum.annotations.Table;
import bum.interfaces.*;
import java.rmi.RemoteException;
import java.util.*;
import mapping.MappingObjectImpl;

@Table
public class PriceListImpl extends MappingObjectImpl implements PriceList {
  @OneToMany(mappedBy="priceList")
  private List<Product> products = new ArrayList<>();

  @ManyToOne
  private Company company;

  public PriceListImpl() throws RemoteException {
    super();
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
  public Company getCompany() throws RemoteException {
    return company;
  }

  @Override
  public void setCompany(Company company) throws RemoteException {
    this.company = company;
  }
}