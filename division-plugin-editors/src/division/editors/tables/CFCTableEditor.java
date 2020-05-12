package division.editors.tables;

import bum.editors.EditorController;
import bum.editors.TreeEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.CFC;
import bum.interfaces.Company;
import bum.interfaces.Contract;
import bum.interfaces.Worker;
import division.editors.objects.CFCEditor;
import division.swing.guimessanger.Messanger;
import division.swing.tree.Node;
import division.swing.tree.bum_Node;
import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreePath;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;

public class CFCTableEditor extends TreeEditor {
  private final JPopupMenu pop = new JPopupMenu();
  
  private final JMenuItem moveTo = new JMenuItem("Переместить");
  private final JMenuItem copyTo = new JMenuItem("Копировать");
  
  private final JPopupMenu companyPop = new JPopupMenu();
  private final JMenuItem companyMoveTo = new JMenuItem("Переместить");
  private final JMenuItem companyCopyTo = new JMenuItem("Копировать");
  private final companyCopyToListener actionCompanyCopy = new companyCopyToListener();
  private final companyMoveToListener actionCompanyMove = new companyMoveToListener();
  
  public CFCTableEditor() {
    super("Все группы", CFC.class, CFCEditor.class, "Все группы");
    pop.add(copyTo);
    pop.add(moveTo);
    //copyTo.addActionListener(actionCopy);
    //moveTo.addActionListener(actionMove);
    
    companyPop.add(companyMoveTo);
    companyPop.add(companyCopyTo);
    companyCopyTo.addActionListener(actionCompanyCopy);
    companyMoveTo.addActionListener(actionCompanyMove);
  }
  
  class companyCopyToListener implements ActionListener {
    private List<Integer> companys;
    private Integer cfc;

    public void setCompanys(List<Integer> companys) {
      this.companys = companys;
    }

    public void setCFC(Integer cfc) {
      this.cfc = cfc;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      RemoteSession session = null;
      try {
        session = ObjectLoader.createSession();
        for(Integer id:companys)
          session.executeUpdate("INSERT INTO [CFC(companys):table]([CFC(companys):object], [CFC(companys):target]) VALUES("+cfc+","+id+")");
        session.addEvent(Company.class, "UPDATE", companys.toArray(new Integer[0]));
        session.commit();
      }catch(Exception ex) {
        ObjectLoader.rollBackSession(session);
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  class companyMoveToListener implements ActionListener {
    private List<Integer> companys;
    private Integer cfc;
    private Integer[] oldCfc;

    public void setCompanys(List<Integer> companys) {
      this.companys = companys;
    }

    public void setCFC(Integer cfc) {
      this.cfc = cfc;
    }

    public void setOldCfc(Integer[] oldCfc) {
      this.oldCfc = oldCfc;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      RemoteSession session = null;
      try {
        List<List> customerContracts = ObjectLoader.executeQuery("SELECT [Contract(id)] FROM [Contract] WHERE [Contract(customerCompany)]=ANY(?)", true, new Object[]{companys.toArray(new Integer[0])});
        List<List> sellerContracts   = ObjectLoader.executeQuery("SELECT [Contract(id)] FROM [Contract] WHERE [Contract(sellerCompany)]=ANY(?)", true, new Object[]{companys.toArray(new Integer[0])});
        if((customerContracts.isEmpty() && sellerContracts.isEmpty()) || ((!customerContracts.isEmpty() || !sellerContracts.isEmpty()) && 
                JOptionPane.showConfirmDialog(null, "Данное действие повлечёт за собой смену группы договора.\nПродолжить?", "Внимание...", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == 0)) {
          session = ObjectLoader.createSession();
          session.executeUpdate("DELETE FROM [CFC(companys):table] WHERE [CFC(companys):object]=ANY(?) AND [CFC(companys):target]=ANY(?)", new Object[]{oldCfc, companys.toArray(new Integer[0])});
          List<List> data;
          for(Integer id:companys) {
            data = session.executeQuery("SELECT * FROM [CFC(companys):table] WHERE [CFC(companys):object]="+cfc+" AND [CFC(companys):target]="+id);
            if(data.isEmpty())
              session.executeUpdate("INSERT INTO [CFC(companys):table]([CFC(companys):object], [CFC(companys):target]) VALUES("+cfc+","+id+")");
          }
          
          Integer[] contractIds = new Integer[0];
          for(List d:customerContracts)
            contractIds = (Integer[]) ArrayUtils.add(contractIds, d.get(0));
          if(contractIds.length > 0) {
            session.executeUpdate("UPDATE [Contract] SET [Contract(customerCfc)]="+cfc+" WHERE [Contract(id)]=ANY(?)", new Object[]{contractIds});
            session.addEvent(Contract.class, "UPDATE", contractIds);
          }
          
          contractIds = new Integer[0];
          for(List d:sellerContracts)
            contractIds = (Integer[]) ArrayUtils.add(contractIds, d.get(0));
          if(contractIds.length > 0) {
            session.executeUpdate("UPDATE [Contract] SET [Contract(sellerCfc)]="+cfc+" WHERE [Contract(id)]=ANY(?)", new Object[]{contractIds});
            session.addEvent(Contract.class, "UPDATE", contractIds);
          }
          session.addEvent(Company.class, "UPDATE", companys.toArray(new Integer[0]));
          session.commit();
        }
      }catch(Exception ex) {
        ObjectLoader.rollBackSession(session);
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  /*class copyToListener implements ActionListener {
    private List<MappingObject> peoples;
    private MappingObject cfc;

    public void setPeoples(List<MappingObject> peoples) {
      this.peoples = peoples;
    }

    public void setCFC(MappingObject cfc) {
      this.cfc = cfc;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      try {
        ((CFC)cfc).addUsers(Arrays.asList(peoples.toArray(new Worker[peoples.size()])));
      }catch(RemoteException ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  class moveToListener implements ActionListener {
    private List<MappingObject> workers;
    private MappingObject cfc;

    public void setPeoples(List<MappingObject> workers) {
      this.workers = workers;
    }

    public void setCFC(MappingObject cfc) {
      this.cfc = cfc;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      try {
        for(MappingObject worker:workers)
          ((Worker)worker).setCfc((CFC)cfc);
      }catch(RemoteException ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }*/
  
  @Override
  protected void drop(Point point, List objects, Class interfaceClass) {
    super.drop(point, objects, interfaceClass);
    TreePath path = tree.getPathForLocation(point.x, point.y);
    if(path != null) {
      Node targetNode = (Node)path.getLastPathComponent();
      if(!targetNode.isRoot()) {
        /*if(interfaceClass == Worker.class) {
          actionCopy.setPeoples(objects);
          actionCopy.setCFC(((bum_Node)targetNode).getObject());

          actionMove.setPeoples(objects);
          actionMove.setCFC(((bum_Node)targetNode).getObject());

          pop.show(getTree(), point.x, point.y);
        }else */if(interfaceClass == Company.class) {
          actionCompanyCopy.setCompanys(objects);
          actionCompanyCopy.setCFC(((bum_Node)targetNode).getId());

          actionCompanyMove.setCompanys(objects);
          actionCompanyMove.setCFC(((bum_Node)targetNode).getId());
          actionCompanyMove.setOldCfc(getSelectedId());

          companyPop.show(getTree(), point.x, point.y);
        }else if(interfaceClass == Contract.class) {
          try {
            Integer cfcId = ((bum_Node)targetNode).getId();
            int r = JOptionPane.showOptionDialog(
                    EditorController.getFrame(), 
                    "Какую группу сменить в договорах на \""+targetNode.toString()+"\"?", 
                    "Сменить группу.", 
                    JOptionPane.DEFAULT_OPTION, 
                    JOptionPane.QUESTION_MESSAGE, 
                    null, 
                    new String[]{"Группу продавца","Группу покупателя","Отмена"}, "Группу продавца");
            if(r < 2) {
              String company = "sellerCompany";
              String cfc     = "sellerCfc";
              if(r == 1) {
                company = "customerCompany";
                cfc     = "customerCfc";
              }
              
              Integer[] contracts = (Integer[]) objects.toArray(new Integer[0]);
              try {
                List<List> data = ObjectLoader.executeQuery("SELECT "
                        + "[Contract(id)],"
                        + "[Contract(number)],"
                        + "(SELECT COUNT([Company(cfcs):target]) "
                        + "FROM [Company(cfcs):table] WHERE [Company(cfcs):target]=? AND [Company(cfcs):object]=[Contract("+company+")]) "
                        + "FROM [Contract] WHERE [Contract(id)]=ANY(?)", true, new Object[]{cfcId,contracts});
                contracts = new Integer[0];
                String notUpdate = "";
                for(List d:data) {
                  if(!d.get(2).equals(0l))
                    contracts = (Integer[]) ArrayUtils.add(contracts, d.get(0));
                  else notUpdate += ", № "+d.get(1)+(notUpdate.length()%50 == 49?"\n":"");
                }
                
                if(!notUpdate.equals("")) {
                    r = JOptionPane.showConfirmDialog(
                            EditorController.getFrame(), 
                            "Невозможно сменить группу "+(r==0?"продавца":"покупателя")+"\n для следующих договоров : "+notUpdate.substring(2)+"."
                            + (contracts.length>0?"\nПродолжить?":""), 
                            contracts.length>0?"Внимание, вопрос!":"Внимение!", 
                            contracts.length>0?JOptionPane.YES_NO_OPTION:JOptionPane.CLOSED_OPTION, 
                            contracts.length>0?JOptionPane.QUESTION_MESSAGE:JOptionPane.ERROR_MESSAGE);
                    if(r == 1 || contracts.length == 0) {
                      return;
                    }
                  }
                
                if(contracts.length > 0) {
                  if(ObjectLoader.executeUpdate("UPDATE [Contract] SET [Contract("+cfc+")]=? WHERE [Contract(id)]=ANY(?)", new Object[]{cfcId, contracts}, true) > 0)
                    ObjectLoader.sendMessage(Contract.class, "UPDATE", contracts);
                }
                
              }catch(Exception ex) {
                Messanger.showErrorMessage(ex);
              }
            }
            
            
            
            /*if(JOptionPane.showConfirmDialog(
              EditorController.getInstance().getFrame(),
              "Сменить группу продавца\nвыделенных договоров на \""+targetNode.getLabel()+"\"",
              "Смена исполнителя со стороны продавца",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE) == 0) {
              for(Object o:objects) {
                if(o instanceof Integer)
                  contract = (Contract)contractTable.getObject((Integer)o);
                else contract = (Contract)o;
                if(contract != null) {
                  sellerCompany = contract.getSellerCompany();
                  if(sellerCompany != null) {
                    if(cfc.getCompanys().contains(sellerCompany)) {
                      contract.setSellerCfc(cfc);
                      contract.saveObject();
                    }
                  }
                }
              }
            }*/
          }catch(Exception ex){Messanger.showErrorMessage(ex);}
        }
      }
    }
  }

  @Override
  protected Hashtable<Class,Integer> getDropSupportedClasses() {
    Hashtable<Class,Integer> hash = new Hashtable<Class,Integer>();
    hash.put(CFC.class,DnDConstants.ACTION_MOVE);
    hash.put(Worker.class,DnDConstants.ACTION_COPY);
    hash.put(Company.class,DnDConstants.ACTION_COPY);
    hash.put(Contract.class,DnDConstants.ACTION_MOVE);
    return hash;
  }
  
  @Override
  public String getEmptyObjectTitle() {
    return "[ЦФУ]";
  }
}