package division.editors.tables;

import bum.editors.EditorGui;
import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Company;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Deal;
import bum.interfaces.DealComment;
import division.ClientMain;
import division.ClientMainListener;
import division.editors.objects.company.nCompanyEditor;
import division.fx.PropertyMap;
import division.scale.ObjectPeriod;
import division.scale.ObjectPeriodScaleListener;
import division.swing.*;
import division.swing.guimessanger.Messanger;
import division.swing.multilinetable.MyCellEditor;
import division.swing.multilinetable.MyCellRenderer;
import division.util.FileLoader;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.*;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;

public class DealCommentTableEditor extends JPanel implements ObjectPeriodScaleListener, ClientMainListener {
  private final JToolBar toolBar = new JToolBar();
  private final DivisionToolButton addToolButton     = new DivisionToolButton(FileLoader.getIcon("Add16.gif"),"Добавить");
  private final DivisionToolButton removeToolButton  = new DivisionToolButton(FileLoader.getIcon("Delete16.gif"),"Удалить");
  
  private final JSplitPane split = new JSplitPane();
  private final JPanel contactPanel  = new JPanel(new GridBagLayout());
  private final JPanel commentsPanel = new JPanel(new GridBagLayout());
  
  private final DivisionTable table = new DivisionTable();
  private final JScrollPane scroll = new JScrollPane(table);
  private final MyCellRenderer renderer = new MyCellRenderer(new int[]{0/*,1,3,4*/});
  private final MyCellEditor   editor   = new MyCellEditor(renderer);
  
  private final JButton editContacts = new JButton("редактировать");
  private final DivisionTextArea contactInfo = new DivisionTextArea("Контактная информация...");
  private final JScrollPane contactInfoScroll = new JScrollPane(contactInfo);
  
  private boolean active = true;
  private Integer deal;
  
  private DivisionTarget divisionTargetComment;
  private DivisionTarget divisionTargetCompany;
  
  public DealCommentTableEditor() {
    super(new BorderLayout());
    initComponents();
    initEvents();
  }

  private void initComponents() {
    add(split, BorderLayout.CENTER);
    split.add(contactPanel,  JSplitPane.LEFT);
    split.add(commentsPanel, JSplitPane.RIGHT);
    
    toolBar.setFloatable(false);
    toolBar.add(addToolButton);
    toolBar.add(removeToolButton);
    
    
    contactInfo.setWrapStyleWord(true);
    contactInfo.setLineWrap(true);
    contactInfo.setEditable(false);
    
    contactPanel.add(editContacts,      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5,5,0,5), 0, 0));
    contactPanel.add(contactInfoScroll, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,GridBagConstraints.CENTER,    GridBagConstraints.BOTH, new Insets(5,5,5,5), 0, 0));
    
    commentsPanel.add(toolBar, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
    commentsPanel.add(scroll,  new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    
    table.setColumns("id","Дата","Комментарий","Контроль","Автор");
    table.setColumnWidthZero(0);
    
    table.findTableColumn(1).setCellRenderer(renderer);
    table.findTableColumn(2).setCellRenderer(renderer);
    table.findTableColumn(3).setCellRenderer(renderer);
    table.findTableColumn(4).setCellRenderer(renderer);

    table.findTableColumn(2).setCellEditor(editor);
    table.findTableColumn(3).setCellEditor(editor);
    
    table.getSorter().setSortKeys(Arrays.asList(new RowSorter.SortKey[]{new RowSorter.SortKey(1, SortOrder.DESCENDING)}));
    table.setColumnEditable(2, true);
    table.setColumnEditable(3, true);
  }
  
  private void addComment() {
    RemoteSession session = null;
    try {
      Integer workerId = ObjectLoader.getClient().getWorkerId();
      if(getDeal() != null && workerId != null) {
        session = ObjectLoader.createSession();
        session.executeUpdate("INSERT INTO [DealComment]([DealComment(deal)], [DealComment(worker)]) VALUES(?,?)", new Object[]{getDeal(),workerId});
        List<List> data = session.executeQuery("SELECT MAX([DealComment(id)]) FROM [DealComment]");
        if(!data.isEmpty()) {
          session.addEvent(Deal.class, "UPDATE", getDeal());
          session.addEvent(DealComment.class, "CREATE", (Integer) data.get(0).get(0));
        }
        session.commit();
      }
    }catch(Exception ex) {
      ObjectLoader.rollBackSession(session);
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void removeComment() {
    try {
      int[] rows = table.getSelectedRows();
      if(getDeal() != null && rows.length > 0) {
        Integer[] ids = new Integer[0];
        for(int row:rows)
          ids = (Integer[]) ArrayUtils.add(ids, table.getValueAt(row, 0));
        if(ObjectLoader.executeUpdate("DELETE FROM [DealComment] WHERE [DealComment(id)]=ANY(?)", true, new Object[]{ids}) > 0) {
          ObjectLoader.sendMessage(Deal.class, "UPDATE", getDeal());
          ObjectLoader.sendMessage(DealComment.class, "REMOVE", ids);
        }
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void saveComment() {
    if(isActive() && isEnabled() && getDeal() != null && table.getSelectedRowCount() > 0) {
      try {
        Integer dealCommentId = (Integer) table.getValueAt(table.getSelectedRow(), 0);
        if(dealCommentId != null) {
          editor.stopCellEditing();
          List<List> data = ObjectLoader.executeQuery("SELECT [DealComment(comment)],[DealComment(stopTime)] FROM [DealComment] WHERE [DealComment(id)]="+dealCommentId, true);
          String commentText = (String) table.getValueAt(table.getSelectedRow(), 2);
          java.sql.Date stopDate = new java.sql.Date(((Date) table.getValueAt(table.getSelectedRow(), 3)).getTime());
          if(!commentText.equals(data.get(0).get(0)) || !stopDate.equals(new Date(((java.sql.Date)data.get(0).get(1)).getTime()))) {
            if(ObjectLoader.executeUpdate("UPDATE [DealComment] SET "
                    + "[DealComment(comment)]=?, "
                    + "[DealComment(date)]=CURRENT_TIMESTAMP, "
                    + "[DealComment(stopTime)]=? "
                    + "WHERE [DealComment(id)]=?", 
                    new Object[]{commentText,stopDate,dealCommentId}) > 0) {
              ObjectLoader.sendMessage(DealComment.class, "UPDATE", dealCommentId);
              ObjectLoader.sendMessage(Deal.class, "UPDATE", getDeal());
            }
          }
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    EditorGui.setComponentEnable(split, enabled);
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    if(!active && isActive())
      saveComment();
    setEnabled(active);
    this.active = active;
  }

  private Integer getDeal() {
    return deal;
  }

  private void setDeal(Integer deal) {
    this.deal = deal;
    
    if(divisionTargetCompany != null)
      divisionTargetCompany.dispose();
    
    contactInfo.setText("");
    if(deal != null) {
      setContactData(deal);
      Object customerCompanyPartition = ObjectLoader.getData(Deal.class, deal, "customerCompanyPartition");
      if(customerCompanyPartition != null) {
        divisionTargetCompany = new DivisionTarget(CompanyPartition.class, (Integer) customerCompanyPartition) {
          @Override
          public void messageReceived(String type, Integer[] ids, PropertyMap objectEventProperty) {
            if(isActive() && isEnabled()) {
              if(ids.length > 0) {
                if(type.equals("UPDATE"))
                  setContactData(deal);
              }
            }
          }
        };
      }
    }
  }
  
  public void initData() {
    table.clear();
    if(isActive() && isEnabled()) {
      try {
        List<List> commentsData = ObjectLoader.executeQuery("SELECT "
                + "[DealComment(id)], "
                + "[DealComment(date)], "
                + "[DealComment(comment)], "
                + "[DealComment(stopTime)], "
                + "[DealComment(avtor)] "
                + "FROM [DealComment] WHERE tmp=false AND type='CURRENT' AND [DealComment(deal)]="+getDeal(), true);
        for(List d:commentsData)
          table.getTableModel().addRow(d.toArray());
      }catch(Exception ex) {
        setEnabled(false);
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  private void setContactData(Integer deal) {
    contactInfo.setText("");
    List<List> contactData = ObjectLoader.executeQuery("SELECT "
                + "[CompanyPartition(telefon)], "
                + "[CompanyPartition(email)], "
                + "[CompanyPartition(contactFio)], "
                + "[CompanyPartition(contactInfo)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=(SELECT [Deal(customerCompanyPartition)] FROM [Deal] WHERE [Deal(id)]="+deal+")", true);
    if(contactData != null && !contactData.isEmpty()) {
      String contactText = "Контактная информация:\n";
      contactText += "\nТелефон: "+(contactData.get(0).get(0)==null?"":contactData.get(0).get(0));
      contactText += "\nEmail: "+(contactData.get(0).get(1)==null?"":contactData.get(0).get(1));
      contactText += "\nКонтактное лицо: "+(contactData.get(0).get(2)==null?"":contactData.get(0).get(2));
      contactText += "\n\n---------прочее-----------\n"+(contactData.get(0).get(3)==null?"":contactData.get(0).get(3));
      contactInfo.setText(contactText);
    }
  }

  private void initEvents() {
    editContacts.addActionListener(e -> {
      if(getDeal() != null) {
        try {
          EditorGui.waitCursor(DealCommentTableEditor.this);
          List<List> data = ObjectLoader.executeQuery("SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE id=(SELECT [Deal(customerCompanyPartition)] FROM [Deal] WHERE id="+getDeal()+")", true);
          if(data != null && !data.isEmpty()) {
            nCompanyEditor companyEditor = new nCompanyEditor();
            companyEditor.setEditorObject(ObjectLoader.getObject(Company.class, (Integer) data.get(0).get(0)));
            companyEditor.setAutoLoadAndStore(true);
            companyEditor.createDialog().setVisible(true);
          }
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }finally {
          EditorGui.defaultCursor(DealCommentTableEditor.this);
        }
      }
    });
    
    addToolButton.addActionListener(e -> addComment());
    removeToolButton.addActionListener(e -> removeComment());
    ClientMain.addClientMainListener(this);
    
    table.getTableModel().addTableModelListener((TableModelEvent e) -> {
      SwingUtilities.invokeLater(() -> {
        if(e.getType() == TableModelEvent.UPDATE)
          saveComment();
      });
    });

    divisionTargetComment = new DivisionTarget(DealComment.class) {
      @Override
      public void messageReceived(String type, Integer[] ids, PropertyMap objectEventProperty) {
        if(isActive() && isEnabled()) {
          if(ids.length > 0) {
            if(type.equals("UPDATE") || type.equals("CREATE")) {
              final List<List> data = ObjectLoader.executeQuery("SELECT DISTINCT "
                      + "[DealComment(id)], "
                      + "[DealComment(date)], "
                      + "[DealComment(comment)], "
                      + "[DealComment(stopTime)], "
                      + "[DealComment(avtor)] "
                      + "FROM [DealComment] "
                      + "WHERE [DealComment(deal)]="+getDeal()+" AND [DealComment(id)]=ANY(?)", new Object[]{ids});
              if(!data.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                  data.stream().forEach(d -> {
                    boolean update = false;
                    for(Vector row:(Vector<Vector>)table.getTableModel().getDataVector()) {
                      if(row.get(0).equals(d.get(0))) {
                        update = true;
                        row.set(1, d.get(1));
                        row.set(2, d.get(2));
                        row.set(3, d.get(3));
                        row.set(4, d.get(4));
                      }
                    }
                    if (!update) {
                      table.getTableModel().addRow(d.toArray());
                    }
                  });
                  table.getTableModel().fireTableDataChanged();
                });
              }
            }else if(type.equals("REMOVE")) {
              final Integer[] fids = ids;
              SwingUtilities.invokeLater(() -> {
                Vector<Vector> dataVector = table.getTableModel().getDataVector();
                for(int i=dataVector.size()-1;i>=0;i--)
                  if(ArrayUtils.contains(fids, dataVector.get(i).get(0)))
                    dataVector.remove(i);
                table.getTableModel().fireTableDataChanged();
              });
            }
          }
        }
      }
    };
  }
  
  public void dispose() {
    editor.stopCellEditing();
    divisionTargetComment.dispose();
    if(divisionTargetCompany != null)
      divisionTargetCompany.dispose();
  }

  public Collection getComponentsToStore() {
    ArrayList components = new ArrayList();
    table.setName(getClass().getName()+"_commentTable");
    split.setName(getClass().getName()+"_commentSplit");
    components.add(table);
    components.add(split);
    return components;
  }

  @Override
  public void objectPeriodDoubleClicked(ObjectPeriod period) {
  }

  @Override
  public void objectPeriodsSelected(List<ObjectPeriod> periods) {
    setEnabled(periods.size() == 1);
    if(periods.size() == 1 && !periods.get(0).getId().equals(getDeal())) {
      saveComment();
      setDeal(periods.get(0).getId());
      initData();
    }
  }

  @Override
  public void dayDoubleClicked(int rowIndex, Date day) {
  }

  @Override
  public void daysSelected(TreeMap<Integer, List<Date>> days) {
    saveComment();
    setEnabled(false);
    table.clear();
    setDeal(null);
  }

  @Override
  public void dayWidthChanged(int dayWidth) {
  }

  @Override
  public void changedCFC(Integer[] id) {
    daysSelected(null);
  }

  @Override
  public void changedCompany(Integer[] id) {
    daysSelected(null);
  }
}