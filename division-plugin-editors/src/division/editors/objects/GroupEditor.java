package division.editors.objects;

import division.swing.guimessanger.Messanger;
import bum.editors.MainObjectEditor;
import bum.editors.TableEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Factor;
import bum.interfaces.Group;
import division.editors.tables.FactorTableEditor;
import division.swing.*;
import division.util.FileLoader;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import mapping.MappingObject.Type;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;

public class GroupEditor extends MainObjectEditor {
  Group.ObjectType type;
  private final DivisionTextField  groupName    = new DivisionTextField(DivisionTextField.Type.ALL);
  private final DivisionTextField  barcode      = new DivisionTextField(DivisionTextField.Type.ALL);
  private final DivisionComboBox   edizm        = new DivisionComboBox();
  private final DivisionToolButton addEdizm     = new DivisionToolButton("Добавить единицу измерения");

  private final TableEditor    factorTable   = new TableEditor(
          new String[]{"id", "Наименование","Тип","Ед. измерения","Значения","Уникальность"},
          new String[]{"id","name","factorType","unit","listValues","unique"},
          Factor.class,
          null,
          "", 
          null, 
          Type.CURRENT, 
          false) {
            @Override
            protected void insertData(List<List> data, int startIndex) {
              data.stream().forEach(d -> d.add(false));
              super.insertData(data, startIndex);
            }
          };
  
  private final JTextArea          description       = new JTextArea();
  private final DivisionScrollPane descriptionScroll = new DivisionScrollPane(description);

  private final DivisionToolButton addImage          = new DivisionToolButton(FileLoader.getIcon("Add16.gif"), "Загрузить изображение");
  private final DivisionToolButton removeImage       = new DivisionToolButton(FileLoader.getIcon("Delete16.gif"), "Удалить изображение");
  private final JLabel             imageBox          = new JLabel();
  private final DivisionScrollPane imageBoxScroll    = new DivisionScrollPane(imageBox);
  
  public GroupEditor() throws RemoteException {
    super();
    initComponents();
    initEvents();
  }

  private void initComponents() {
    //factorTable.getTable().setColumnEditable(4, true);
    factorTable.setAddFunction(true);
    
    factorTable.getTable().getSorter().setSortKeys(Arrays.asList(new RowSorter.SortKey[]{new RowSorter.SortKey(5, SortOrder.DESCENDING)}));

    JPanel panel = new JPanel(new GridBagLayout());
    panel.add(new JLabel("Наименование"),  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    panel.add(groupName,                   new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    panel.add(new JLabel("Штрих-код"),     new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    panel.add(barcode,                     new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    panel.add(new JLabel("Ед. измерения"), new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    panel.add(edizm,                       new GridBagConstraints(5, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    panel.add(addEdizm,                    new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

    factorTable.getGUI().setBorder(BorderFactory.createTitledBorder("Реквизиты групп экземпляров объекта"));
    factorTable.setVisibleOkButton(false);

    JPanel descriptionPanel  = new JPanel(new GridBagLayout());
    descriptionPanel.setBorder(BorderFactory.createTitledBorder("Функционал"));
    descriptionPanel.add(descriptionScroll, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

    JPanel imagePanel  = new JPanel(new GridBagLayout());
    imagePanel.setBorder(BorderFactory.createTitledBorder("Изображение"));
    imagePanel.add(addImage,       new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    imagePanel.add(removeImage,    new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    imagePanel.add(imageBoxScroll, new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

    JPanel podrobPanel = new JPanel(new GridBagLayout());
    podrobPanel.setBorder(BorderFactory.createTitledBorder("Подробности"));
    podrobPanel.add(descriptionPanel, new GridBagConstraints(0, 0, 1, 1, 0.5, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    podrobPanel.add(imagePanel,       new GridBagConstraints(1, 0, 1, 1, 0.5, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

    getRootPanel().add(panel,                     new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    getRootPanel().add(factorTable.getGUI(),      new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    getRootPanel().add(podrobPanel,               new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
  }

  private void initEvents() {
    addImage.addActionListener(e -> {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Выберите файл изображения");
      fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
      fileChooser.setFileFilter(new FileFilter() {
        @Override
        public boolean accept(File f) {
          if(f.isDirectory())
            return true;
          int index = f.getName().lastIndexOf(".")+1;
          String type = f.getName().substring(index).toLowerCase();
          return type.equals("jpg") || type.equals("jpeg") ||
                  type.equals("gif") || type.equals("png") || type.equals("bmp");
        }
        
        @Override
        public String getDescription() {
          return "Файл изображения (jpg,jpeg,gif,png,bmp)";
        }
      });
      
      fileChooser.showDialog(null, "выбрать");
      File file = fileChooser.getSelectedFile();
      if(file != null)
        imageBox.setIcon(new ImageIcon(file.getAbsolutePath()));
    });

    removeImage.addActionListener(e -> imageBox.setIcon(null));
    groupName.addActionListener(e -> okButtonAction());
    factorTable.setRemoveAction(e -> removeFactorFromGroup());
    factorTable.setAddAction(e -> createGroupFactor());
    
    groupName.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        setTitle(type+"::"+groupName.getText());
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        setTitle(type+"::"+groupName.getText());
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        setTitle(type+"::"+groupName.getText());
      }
    });
  }
  
  private void removeFactorFromGroup() {
    Integer[] factors = factorTable.getSelectedId();
    if(JOptionPane.showConfirmDialog(
            getRootPanel(),
            "<html>Вы уверены в том что хотите <b>удалить</b> выделенны"+(factors.length>1?"е":"й")+" объект"+(factors.length>1?"ы":"")+"?</html>",
            "Подтверждение удаления",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE) == 0) {
      try {
        Integer[] groups  = new Integer[]{((Group)getEditorObject()).getId()};
        List<List> data = ObjectLoader.executeQuery("SELECT getGroupChilds("+((Group)getEditorObject()).getId()+")", true);
        if(!data.isEmpty() && data.get(0).get(0) != null)
          groups = (Integer[]) ArrayUtils.addAll(groups, (Integer[]) data.get(0).get(0));

        if(ObjectLoader.executeUpdate("DELETE FROM [!Group(factors):table] WHERE [!Group(factors):object]=ANY(?) AND [!Group(factors):target]=ANY(?)", true,
                new Object[]{groups, factors}) > 0)
          ObjectLoader.sendMessage(Factor.class, "UPDATE", factors);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
  }
  }

  private void createGroupFactor() {
    try {
      FactorTableEditor factorTableEditor = new FactorTableEditor();
      factorTableEditor.getClientFilter()
              .AND_NOT_IN("groups", new Integer[]{((Group)getEditorObject()).getId()})
              .AND_EQUAL("productFactor", false);
      factorTableEditor.setAutoLoad(true);
      factorTableEditor.setAutoStore(true);
      factorTableEditor.initData();
      
      Integer[] groups  = new Integer[]{((Group)getEditorObject()).getId()};
      Integer[] factors = factorTableEditor.get();
      
      RemoteSession session = null;
      try {
        List<List> data = ObjectLoader.executeQuery("SELECT array_agg(a) FROM unnest(getGroupChilds("+((Group)getEditorObject()).getId()+")) a "
                + "WHERE a <> ALL(ARRAY(SELECT [Group(factors):object] FROM [Group(factors):table] WHERE [Group(factors):target]=ANY(?)))", true, new Object[]{factors});
        
        if(!data.isEmpty() && data.get(0).get(0) != null)
          groups = (Integer[]) ArrayUtils.addAll(groups, (Integer[]) data.get(0).get(0));
        
        String[] querys = new String[0];
        for(Integer factor:factors) {
          for(Integer group:groups) {
            querys = (String[]) ArrayUtils.add(querys, 
                    "INSERT INTO [!Group(factors):table] ([!Group(factors):object],[!Group(factors):target]) VALUES ("+group+","+factor+")");
          }
        }
        session = ObjectLoader.createSession();
        session.executeUpdate(querys);
        session.addEvent(Factor.class, "UPDATE", factors);
        session.commit();
      }catch(RemoteException | HeadlessException ex) {
        ObjectLoader.rollBackSession(session);
        Messanger.showErrorMessage(ex);
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  @Override
  public void initData() throws RemoteException {
    final Group group = (Group)this.getEditorObject();
    
    factorTable.getClientFilter().clear();
    factorTable.getClientFilter().AND_IN("groups", new Integer[]{group.getId()});
    factorTable.initData();

    barcode.setText(group.getBarcode());
    
    byte[] im = group.getImage();
    if(im != null && im.length > 0)
      imageBox.setIcon(new ImageIcon(im,""));

    description.setText(group.getDescription());
    
    String name = group.getName();
    
    type = group.getGroupType();
    
    setTitle(type+"::"+(name==null?"":name));
    groupName.setText(group.getName());
    groupName.grabFocus();
  }
  
  @Override
  public String commit() throws RemoteException {
    String msg = "";
    Group group = (Group)this.getEditorObject();
    
    if("".equals(groupName.getText()))
      msg+="незаполнено поле 'Наименование'";

    if(msg.equals("")) {
      group.setName(groupName.getText());
    }

    group.setDescription(description.getText());
    
    ImageIcon icon = (ImageIcon)imageBox.getIcon();
    if(icon != null) {
      if(!"".equals(icon.getDescription())) {
        try {
          FileInputStream in = new FileInputStream(icon.getDescription());
          byte[] bytes = new byte[in.available()];
          in.read(bytes);
          in.close();
          in = null;
          group.setImage(bytes);
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    }
    return msg;
  }
  
  @Override
  public void clearData() {
    groupName.setText("");
  }
  
  @Override
  public String getEmptyObjectTitle() {
    return "[группа/объект]";
  }
}