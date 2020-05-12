package division.editors.products;

import bum.editors.util.ObjectLoader;
import bum.interfaces.Product;
import division.fx.PropertyMap;
import division.swing.TreeTable.SwingNode;
import division.swing.guimessanger.Messanger;
import java.math.BigDecimal;
import java.time.Period;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import javafx.collections.ObservableList;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
import mapping.MappingObject;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class ProductNode extends SwingNode {
  public enum Type{Process,Group,Product}
  private Type type;
  
  private boolean archive = false;
  
  private Integer company;
  
  private Integer groupId;
  
  private Integer id;
  private Integer globId;
  
  private BigDecimal cost;
  private BigDecimal globCost;

  private Period duration;
  private Period globDuration;
  
  private Period recurrence;
  private Period globRecurrence;
  
  private BigDecimal nds;
  
  private JTree tree;

  public ProductNode(JTree tree, Integer company, Integer id, String name, Type type) {
    super(name);
    this.id = id;
    this.type = type;
    this.company = company;
    this.tree = tree;
  }
  
  public ProductNode(JTree tree, Integer company, Integer globId, String name) {
    super(name);
    this.globId = globId;
    this.type = Type.Product;
    this.company = company;
    this.tree = tree;
  }

  public Integer getGroupId() {
    return groupId;
  }

  public void setGroupId(Integer groupId) {
    this.groupId = groupId;
  }

  public boolean isArchive() {
    return archive;
  }

  public void setArchive(boolean archive) {
    this.archive = archive;
  }
  
  public void setDuration(Period duration) {
    setDuration(duration, true);
  }
  
  public void setDuration(Period duration, boolean updateDataBase) {
    if(updateDataBase) {
      Integer[] ids = getProductIds(false);
      Integer[] toCreateIds = getToCreateProductIds();
      if(ids.length+toCreateIds.length == 1 || ids.length+toCreateIds.length > 1 && 
              JOptionPane.showConfirmDialog(null, "Изменить для всех продуктов?", "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == 0) {
        if(duration != null) {
          ids = (Integer[]) ArrayUtils.addAll(ids, createProducts(toCreateIds));
          if(updateProduct("duration", duration, ids)) {
            if(getType() == Type.Product)
              this.duration = duration;
          }
        }else {
          removeProducts(ids, isArchive());
        }
      }
    }else 
      if(getType() == Type.Product)
        this.duration = duration;
  }

  public Period getDuration() {
    if(getType() != Type.Product && getChildCount() > 0 && tree != null) {
      if(tree.isCollapsed(new TreePath(getPath()))) {
        Period val = null;
        boolean ndsEempty = true;
        Enumeration<ProductNode> em = preorderEnumeration();
        while(em.hasMoreElements()) {
          ProductNode node = em.nextElement();
          if(node.getType() == Type.Product) {
            if(ndsEempty) {
              val = node.getDuration();
              ndsEempty = false;
            }else {
              if(val == null ? node.getDuration() != null : !val.equals(node.getDuration())) {
                val = null;
                break;
              }
            }
          }
        }
        return val;
      }
    }
    return duration;
  }
  
  public void setGlobDuration(Period globDuration) {
    setGlobDuration(globDuration, true);
  }
  
  public void setGlobDuration(Period globDuration, boolean updateDataBase) {
    if(updateDataBase) {
      Integer[] ids = getProductIds(true);
      if(ids.length == 1 || ids.length > 1 && 
              JOptionPane.showConfirmDialog(null, "Изменить для всех продуктов?", "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == 0) {
        if(updateProduct("duration", globDuration, ids) && getType() == Type.Product)
          this.globDuration = globDuration;
      }
    }else if(getType() == Type.Product)
      this.globDuration = globDuration;
  }
  
  public Period getGlobDuration() {
    if(getType() != Type.Product && getChildCount() > 0 && tree != null) {
      if(tree.isCollapsed(new TreePath(getPath()))) {
        Period val = null;
        boolean ndsEempty = true;
        Enumeration<ProductNode> em = preorderEnumeration();
        while(em.hasMoreElements()) {
          ProductNode node = em.nextElement();
          if(node.getType() == Type.Product) {
            if(ndsEempty) {
              val = node.getGlobDuration();
              ndsEempty = false;
            }else {
              if(val == null ? node.getGlobDuration() != null : !val.equals(node.getGlobDuration())) {
                val = null;
                break;
              }
            }
          }
        }
        return val;
      }
    }
    return globDuration;
  }
  
  public void setRecurrence(Period recurrence) {
    setRecurrence(recurrence, true);
  }
  
  public void setRecurrence(Period recurrence, boolean updateDataBase) {
    if(updateDataBase) {
      Integer[] ids = getProductIds(false);
      Integer[] toCreateIds = getToCreateProductIds();
      if(ids.length+toCreateIds.length == 1 || ids.length+toCreateIds.length > 1 && 
              JOptionPane.showConfirmDialog(null, "Изменить для всех продуктов?", "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == 0) {
        ids = (Integer[]) ArrayUtils.addAll(ids, createProducts(toCreateIds));
        if(updateProduct("recurrence", recurrence, ids)) {
          if(getType() == Type.Product)
            this.recurrence = recurrence;
        }
      }
    }else 
      if(getType() == Type.Product)
        this.recurrence = recurrence;
  }

  public Period getRecurrence() {
    if(getType() != Type.Product && getChildCount() > 0 && tree != null) {
      if(tree.isCollapsed(new TreePath(getPath()))) {
        Period val = null;
        boolean ndsEempty = true;
        Enumeration<ProductNode> em = preorderEnumeration();
        while(em.hasMoreElements()) {
          ProductNode node = em.nextElement();
          if(node.getType() == Type.Product) {
            if(ndsEempty) {
              val = node.getRecurrence();
              ndsEempty = false;
            }else {
              if(val == null ? node.getRecurrence() != null : !val.equals(node.getRecurrence())) {
                val = null;
                break;
              }
            }
          }
        }
        return val;
      }
    }
    return recurrence;
  }
  
  public void setGlobRecurrence(Period globRecurrence) {
    setGlobRecurrence(globRecurrence, true);
  }
  
  public void setGlobRecurrence(Period globRecurrence, boolean updateDataBase) {
    if(updateDataBase) {
      Integer[] ids = getProductIds(true);
      if(ids.length == 1 || ids.length > 1 && 
              JOptionPane.showConfirmDialog(null, "Изменить для всех продуктов?", "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == 0) {
        if(updateProduct("recurrence", globRecurrence, ids) && getType() == Type.Product)
          this.globRecurrence = globRecurrence;
      }
    }else if(getType() == Type.Product)
      this.globRecurrence = globRecurrence;
  }

  public Period getGlobRecurrence() {
    if(getType() != Type.Product && getChildCount() > 0 && tree != null) {
      if(tree.isCollapsed(new TreePath(getPath()))) {
        Period val = null;
        boolean ndsEempty = true;
        Enumeration<ProductNode> em = preorderEnumeration();
        while(em.hasMoreElements()) {
          ProductNode node = em.nextElement();
          if(node.getType() == Type.Product) {
            if(ndsEempty) {
              val = node.getGlobRecurrence();
              ndsEempty = false;
            }else {
              if(val == null ? node.getGlobRecurrence() != null : !val.equals(node.getGlobRecurrence())) {
                val = null;
                break;
              }
            }
          }
        }
        return val;
      }
    }
    return globRecurrence;
  }
  
  public void setCost(BigDecimal cost) {
    setCost(cost, true);
  }
  
  public void setCost(BigDecimal cost, boolean updateDataBase) {
    if(updateDataBase) {
      Integer[] ids = getProductIds(false);
      if(cost != null) {
        ids = (Integer[]) ArrayUtils.addAll(ids, createProducts(getToCreateProductIds()));
        if(updateProduct("cost", cost, ids)) {
          if(getType() == Type.Product)
            this.cost = cost;
        }
      }else {
        removeProducts(ids, isArchive());
      }
    }else 
      if(getType() == Type.Product)
        this.cost = cost;
  }

  public BigDecimal getCost() {
    if(getType() != Type.Product && getChildCount() > 0 && tree != null) {
      if(tree.isCollapsed(new TreePath(getPath()))) {
        BigDecimal val = null;
        boolean ndsEempty = true;
        Enumeration<ProductNode> em = preorderEnumeration();
        while(em.hasMoreElements()) {
          ProductNode node = em.nextElement();
          if(node.getType() == Type.Product) {
            if(ndsEempty) {
              val = node.getCost();
              ndsEempty = false;
            }else {
              if(val == null ? node.getCost() != null : !val.equals(node.getCost())) {
                val = null;
                break;
              }
            }
          }
        }
        return val;
      }
    }
    return cost;
  }
  
  public void setGlobCost(BigDecimal globCost) {
    setGlobCost(globCost, true);
  }
  
  public void setGlobCost(BigDecimal globCost, boolean updateDataBase) {
    if(!updateDataBase || updateProduct("cost", globCost, getProductIds(true)))
      if(getType() == Type.Product)
        this.globCost = globCost;
  }

  public BigDecimal getGlobCost() {
    if(getType() != Type.Product && getChildCount() > 0 && tree != null) {
      if(tree.isCollapsed(new TreePath(getPath()))) {
        BigDecimal val = null;
        boolean ndsEempty = true;
        Enumeration<ProductNode> em = preorderEnumeration();
        while(em.hasMoreElements()) {
          ProductNode node = em.nextElement();
          if(node.getType() == Type.Product) {
            if(ndsEempty) {
              val = node.getGlobCost();
              ndsEempty = false;
            }else {
              if(val == null ? node.getGlobCost() != null : !val.equals(node.getGlobCost())) {
                val = null;
                break;
              }
            }
          }
        }
        return val;
      }
    }
    return globCost;
  }
  
  public void setNds(BigDecimal nds) {
    setNds(nds, true);
  }
  
  public void setNds(BigDecimal nds, boolean updateDataBase) {
    if(!updateDataBase || updateProduct("nds", nds, getProductIds(true)))
      if(getType() == Type.Product)
        this.nds = nds;
  }

  public BigDecimal getNds() {
    if(getType() != Type.Product && getChildCount() > 0 && tree != null) {
      if(tree.isCollapsed(new TreePath(getPath()))) {
        BigDecimal val = null;
        boolean ndsEempty = true;
        Enumeration<ProductNode> em = preorderEnumeration();
        while(em.hasMoreElements()) {
          ProductNode node = em.nextElement();
          if(node.getType() == Type.Product) {
            if(ndsEempty) {
              val = node.getNds();
              ndsEempty = false;
            }else {
              if(val == null ? node.getNds() != null : !val.equals(node.getNds())) {
                val = null;
                break;
              }
            }
          }
        }
        return val;
      }
    }
    return nds;
  }
  
  public static void removeProducts(Integer[] ids, boolean archive) {
    if(archive)
      ObjectLoader.removeObjects(Product.class, ids);
    else {
      try {
        for(List d:ObjectLoader.executeQuery("SELECT id FROM [Product] WHERE [Product(globalProduct)]=ANY(?)", true, new Object[]{ids}))
          ids = (Integer[]) ArrayUtils.add(ids, d.get(0));
        if(ObjectLoader.executeUpdate("UPDATE [Product] SET type='ARCHIVE' WHERE id=ANY(?)", new Object[]{ids}) > 0)
          ObjectLoader.sendMessage(Product.class, "UPDATE", ids);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  private Integer[] createProducts(Integer[] ids) {
    Integer[] createdId = new Integer[0];
    if(ids.length > 0 && company != null) {
      ObservableList<PropertyMap> list = ObjectLoader.getList(Product.class, ids, "id", "service", "group","cost","duration","recurrence","techPassport","nds");
      try {
        for(PropertyMap gp:list) {
          ObservableList<PropertyMap> products = ObjectLoader.getList(DBFilter.create(Product.class)
                  .AND_EQUAL("company", company)
                  .AND_EQUAL("service", gp.getInteger("service"))
                  .AND_EQUAL("group", gp.getInteger("group")), "id","type","tmp");
          if(products.isEmpty()) {
            createdId = (Integer[]) ArrayUtils.add(createdId, ObjectLoader.createObject(Product.class, gp.setValue("company", company).setValue("globalProduct", gp.getInteger("id")).getSimpleMapWithoutKeys("id")));
          }else {
            ObservableList<PropertyMap> archiveProducts =  products.filtered(p -> p.getValue("type", Product.Type.class) == MappingObject.Type.ARCHIVE && !p.is("tmp"));
            if(!archiveProducts.isEmpty()) {
              if(ObjectLoader.saveObject(Product.class, archiveProducts.get(0).setValue("type", Product.Type.CURRENT)))
                createdId = (Integer[]) ArrayUtils.add(createdId, archiveProducts.get(0).getInteger("id"));
            }else {
              ObservableList<PropertyMap> tmpProducts =  products.filtered(p -> p.is("tmp"));
              if(!tmpProducts.isEmpty()) {
                if(ObjectLoader.saveObject(Product.class, tmpProducts.get(0).setValue("type", Product.Type.CURRENT).setValue("tmp", false)))
                  createdId = (Integer[]) ArrayUtils.add(createdId, tmpProducts.get(0).getInteger("id"));
              }else createdId = (Integer[]) ArrayUtils.add(createdId, products.get(0).getInteger("id"));
            }
          }
        }
      }catch(Exception ex) {
        ex.printStackTrace();
      }
    }
    
    /*if(ids.length > 0 && company != null) {
      RemoteSession session = null;
      try {
        Integer startId = 0;
        
        
        ObservableList<PropertyMap> list = ObjectLoader.getList(Product.class, ids, "id", "service", "group");
        TreeSet<Integer> groups = new TreeSet<>(PropertyMap.getListFromList(list, "group", Integer.TYPE));
        
        ids = new Integer[0];
        for(Integer g:groups) {
          ObservableList<PropertyMap> l = list.filtered(p -> g.equals(p.getInteger("group")));
          ids = (Integer[]) ArrayUtils.add(ids, l.get(0).getInteger("id"));
        }

        
        if(ids.length > 0) {
          session = ObjectLoader.createSession();
          List<List> data = session.executeQuery("SELECT MAX(id) FROM [Product]");
          if(!data.isEmpty())
            startId = Integer.valueOf(data.get(0).get(0).toString());
          String query = "INSERT INTO [Product] ([Product(globalProduct)], [Product(service)], [Product(group)], "
                  + "[Product(company)], [Product(cost)], [Product(duration)], [Product(recurrence)], [Product(techPassport)], [Product(nds)]) VALUES ";
          for(Integer id:ids) {
            query += "("+id+"," 
                    +"(SELECT [Product(service)] FROM [Product] WHERE id="+id+"),"
                    + "(SELECT [Product(group)] FROM [Product] WHERE id="+id+"),"
                    + company+","
                    + "(SELECT [Product(cost)] FROM [Product] WHERE id="+id+"),"
                    + "(SELECT [Product(duration)] FROM [Product] WHERE id="+id+"),"
                    + "(SELECT [Product(recurrence)] FROM [Product] WHERE id="+id+"),"
                    + "(SELECT [Product(techPassport)] FROM [Product] WHERE id="+id+"),"
                    + "(SELECT [Product(nds)] FROM [Product] WHERE id="+id+")),";
          }
          query = query.substring(0, query.length()-1);
          session.executeUpdate(query);
          ids = new Integer[0];
          for(List d:session.executeQuery("SELECT id FROM [Product] WHERE id>"+startId))
            ids = (Integer[]) ArrayUtils.add(ids, d.get(0));
          session.addEvent(Product.class, "CREATE", ids);
          ObjectLoader.commitSession(session);
        }
        return ids;//(Integer[]) ArrayUtils.addAll(createdIds, ids);
      }catch(Exception ex) {
        ObjectLoader.rollBackSession(session);
        Messanger.showErrorMessage(ex);
      }
    }*/
    return createdId;
  }
  
  private boolean updateProduct(String field, Object value, Integer[] ids) {
    try {
      ObjectLoader.executeUpdate("UPDATE [Product] SET [Product("+field+")]=? WHERE id=ANY(?)", true, new Object[]{value, ids});
      ObjectLoader.sendMessage(Product.class, "UPDATE", ids);
      return true;
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
      return false;
    }
  }
  
  public Integer getGlobId() {
    return globId;
  }

  public void setGlobId(Integer globId) {
    this.globId = globId;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }
  
  public Integer[] getProductIds(boolean global) {
    Integer[] ids = new Integer[0];
    Enumeration em = preorderEnumeration();
    while(em.hasMoreElements()) {
      ProductNode node = (ProductNode) em.nextElement();
      if(node.getType() == Type.Product) {
        if(global)
          ids = (Integer[]) ArrayUtils.add(ids, node.getGlobId());
        else if(node.getId() != null) 
          ids = (Integer[]) ArrayUtils.add(ids, node.getId());
      }
    }
    return ids;
  }
  
  private Integer[] getToCreateProductIds() {
    Integer[] ids = new Integer[0];
    Enumeration em = preorderEnumeration();
    while(em.hasMoreElements()) {
      ProductNode node = (ProductNode) em.nextElement();
      if(node.getType() == Type.Product && node.getId() == null)
        ids = (Integer[]) ArrayUtils.add(ids, node.getGlobId());
    }
    return ids;
  }
}