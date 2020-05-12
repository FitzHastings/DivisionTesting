package bum.editors.exportImport;

import division.swing.guimessanger.Messanger;
import bum.editors.EditorController;
import bum.editors.util.ObjectLoader;
import division.border.LinkBorder;
import division.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.*;
import mapping.MappingObject;
import org.apache.commons.lang3.ArrayUtils;
import util.RemoteSession;

public class ImportDialog extends DivisionDialog {
  private Class<? extends MappingObject> objectclass;
  private JPanel         root            = new JPanel(new GridBagLayout());
  private JPanel         filePanel       = new JPanel(new BorderLayout());
  private LinkBorder     filePanelBorder = new LinkBorder("Файл импорта");
  private JTextField     fileName        = new JTextField();

  private JPanel           fieldsPanel    = new JPanel(new GridBagLayout());
  private DefaultListModel model          = new DefaultListModel();
  private JList            fieldList      = new JList(model);
  private DivisionScrollPane   fildListScroll = new DivisionScrollPane(fieldList);

  private JButton left = new JButton("<");
  private JButton right = new JButton(">");
  private JButton leftAll = new JButton("<<");
  private JButton rightAll = new JButton(">>");

  private DefaultListModel realModel = new DefaultListModel();
  private JList          realFieldList = new JList(realModel);
  private DivisionScrollPane realFildListScroll = new DivisionScrollPane(realFieldList);

  private DivisionComboBox splitCombo = new DivisionComboBox("табуляция","пробел",";",":","|","\"","\'","\\","/","!","#");
  private JButton importButton = new JButton("Импорт");

  private File importFile = null;
  //private TreeMap<String,Object[]> descriptionMethods;
  private Map<String,Map<String,Object>> fieldsInfo;

  public ImportDialog(Class<? extends MappingObject> objectclass) {
    super(EditorController.getFrame());
    try {
      this.objectclass = objectclass;
      fieldsInfo = ObjectLoader.getFieldsInfo(objectclass);

      for(String field:fieldsInfo.keySet())
        model.addElement(field);
      initComponents();
      initEvents();
      setSize(new Dimension(500, 500));
      centerLocation();
    }catch(Exception ex){Messanger.showErrorMessage(ex);this.dispose();}
  }

  private void initComponents() {
    realFieldList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    realFieldList.setDragEnabled(true);
    realFieldList.setDropTarget(new DropTarget(realFieldList, new listDropTarget()));

    setContentPane(root);
    fileName.setEditable(false);
    filePanel.setBorder(filePanelBorder);
    filePanel.add(fileName, BorderLayout.CENTER);
    root.add(filePanel,          new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
    root.add(new JLabel("Выберите разделить"),      new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    root.add(splitCombo,                            new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));

    fieldsPanel.setBorder(BorderFactory.createTitledBorder("Выберите поля"));
    root.add(fieldsPanel,         new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 0, 5), 0, 0));
    root.add(importButton,        new GridBagConstraints(0, 3, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    
    fieldsPanel.add(fildListScroll,     new GridBagConstraints(0, 0, 1, 4, 0.5, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 0, 5), 0, 0));
    fieldsPanel.add(right,              new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    fieldsPanel.add(rightAll,           new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    fieldsPanel.add(left,               new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    fieldsPanel.add(leftAll,            new GridBagConstraints(1, 3, 1, 1, 0.0, 1.0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    fieldsPanel.add(realFildListScroll, new GridBagConstraints(2, 0, 1, 4, 0.5, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 0, 5), 0, 0));
  }

  private void initEvents() {
    filePanelBorder.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Укажите файл импорта");
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
        if(importFile != null)
          fileChooser.setCurrentDirectory(importFile);
        fileChooser.showOpenDialog(null);
        importFile = fileChooser.getSelectedFile();
        if(importFile != null)
          fileName.setText(importFile.getAbsolutePath());
      }
    });

    importButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(importFile != null) {
          try {
            final ProgressBarDialog pg = new ProgressBarDialog(ImportDialog.this);
            pg.setTitle("Импорт");
            int max = 0;

            BufferedReader buff = new BufferedReader(new FileReader(importFile));
            while(buff.readLine() != null)
              max++;
            buff.close();
            pg.getProgressBar().setStringPainted(true);
            pg.getProgressBar().setMinimum(0);
            pg.getProgressBar().setMaximum(max);
            pg.setVisible(true);
            pg.submit(() -> {
              try {
                Map<String,Object>[] realFieldsInfo = new TreeMap[0];
                for(int i=0;i<realModel.size();i++)
                  realFieldsInfo = (Map<String, Object>[])ArrayUtils.add(realFieldsInfo, fieldsInfo.get(realModel.get(i)));
                RemoteSession session = ObjectLoader.createSession(true);
                String split = splitCombo.getSelectedItem().toString();
                if(split.equals("табуляция"))
                  split = "\t";
                if(split.equals("пробел"))
                  split = " ";
                BufferedReader buff1 = new BufferedReader(new FileReader(importFile));
                String line = "";
                String query;
                query = "INSERT INTO ["+objectclass.getName()+"](";
                for(Map<String,Object> map:realFieldsInfo) {
                  query += map.get("DBNAME")+",";
                  line += ",?";
                }
                query = query.substring(0, query.length()-1)+") VALUES ("+line.substring(1)+")";
                while ((line = buff1.readLine()) != null) {
                  pg.getProgressBar().setValue(pg.getProgressBar().getValue()+1);
                  Object[] params = line.split(split);
                  if(params.length < realFieldsInfo.length)
                    params = Arrays.copyOf(params, 10);
                  session.executeUpdate(query,params);
                }
                buff1.close();
                pg.dispose();
              }catch(Exception ex) {
                Messanger.showErrorMessage(ex);
              }
            });
          }catch(Exception ex){Messanger.showErrorMessage(ex);}
        }else JOptionPane.showMessageDialog(root, "Укажите файл импорта", "Ошибка!", JOptionPane.ERROR_MESSAGE);
      }
    });

    right.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for(Object o:fieldList.getSelectedValues())
          realModel.addElement(o);
        for(Object o:fieldList.getSelectedValues())
          model.removeElement(o);
      }
    });

    rightAll.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for(int i=0;i<model.size();i++)
          realModel.addElement(model.getElementAt(i));
        model.removeAllElements();
      }
    });

    left.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for(Object o:realFieldList.getSelectedValues())
          model.addElement(o);
        for(Object o:realFieldList.getSelectedValues())
          realModel.removeElement(o);
      }
    });

    leftAll.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for(int i=0;i<realModel.size();i++)
          model.addElement(realModel.getElementAt(i));
        realModel.removeAllElements();
      }
    });
  }

  class listDropTarget implements DropTargetListener {
    private int dragIndex;
    private Object dragValue;

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
      dragIndex = realFieldList.getSelectedIndex();
      dragValue = realFieldList.getSelectedValue();
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
      realModel.removeElementAt(dragIndex);
      dragIndex = realFieldList.locationToIndex(dtde.getLocation());
      realModel.insertElementAt(dragValue, dragIndex);
      realFieldList.setSelectedIndex(dragIndex);
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
    }
  }
}