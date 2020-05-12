package division.editors.products;

import bum.editors.EditorGui;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Product;
import division.border.ComponentTitleBorder;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.rmi.RemoteException;
import javax.swing.*;
import net.sf.json.JSONObject;

public class ProductEditor extends EditorGui {
  private final JCheckBox  check      = new JCheckBox("Политика цен");
  private final CostPolicy costPolicy = new CostPolicy();
  
  private JSONObject jsonProduct = null;

  private Integer[] products = new Integer[0];

  public ProductEditor() {
    super(null, null);
    initComponents();
    initEvents();
  }
  
  private void initComponents() {
    costPolicy.setVisibleOkButton(false);
    costPolicy.getGUI().setBorder(new ComponentTitleBorder(check, costPolicy.getGUI(), BorderFactory.createLineBorder(Color.LIGHT_GRAY)));
    getRootPanel().add(costPolicy.getGUI(), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
  }

  public void setProducts(Integer[] products) {
    this.products = products;
    jsonProduct = null;
    if(products.length == 1)
      jsonProduct = ObjectLoader.getJSON(Product.class, products[0]);
  }

  private void initEvents() {
  }
  
  @Override
  public void clear() {
  }
  
  @Override
  public void initData() throws RemoteException {
    clear();
    if(isActive() && products != null && products.length > 0)
      costPolicy.setProducts(products);
  }

  @Override
  public Boolean okButtonAction() {
    return true;
  }

  @Override
  public void initTargets() {
  }
}