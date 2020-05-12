package division.editors.objects.company;

import bum.editors.EditorGui;
import bum.editors.TableEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.CompanyPartitionDocument;
import bum.interfaces.Document;
import division.border.ComponentTitleBorder;
import division.editors.objects.DocumentEditor;
import division.exportimport.ExportImportUtil;
import division.swing.DivisionCalendarComboBox;
import division.swing.DivisionComboBox;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionTextField;
import division.swing.DivisionToolButton;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import mapping.MappingObject;
import division.swing.table.groupheader.ColumnGroup;
import division.swing.table.groupheader.ColumnGroupHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.sql.Date;
import java.time.Period;
import java.util.List;
import util.RemoteSession;

public class CompanyPartitionDocumentTableEditor extends TableEditor {
  private JTabbedPane tabb = new JTabbedPane();
  
  private Integer companyPartitionId = null;
  private final DivisionTextField  exportPath = new DivisionTextField("Файл экспорта...");
  private final DivisionToolButton selectPath = new DivisionToolButton("...", "Указать файл");
  private final ColumnGroupHeader header = new ColumnGroupHeader(getTable());
  private final CompanyPartitionDocTemplateTable companyPartitionDocTemplateTable = new CompanyPartitionDocTemplateTable();
  
  private final String[] formats = new String[]{
    " ",
    "гг","гггг",
    "ггмм","гг/мм","гг.мм","гг-мм",
    "ммгг","мм/гг","мм.гг","мм-гг",
    "ггггмм","гггг/мм","гггг.мм","гггг-мм",
    "ммгггг","мм/гггг","мм.гггг","мм-гггг",
    "ггммдд","гг/мм/дд","гг.мм.дд","гг-мм-дд",
    "ммггдд","мм/гг/дд","мм.гг.дд","мм-гг-дд",
    "ггггммдд","гггг/мм/дд","гггг.мм.дд","гггг-мм-дд",
    "ммггггдд","мм/гггг/дд","мм.гггг.дд","мм-гггг-дд",
    "ггддмм","гг/дд/мм","гг.дд.мм","гг-дд-мм",
    "ммддгг","мм/дд/гг","мм.дд.гг","мм-дд-гг",
    "ггггддмм","гггг/дд/мм","гггг.дд.мм","гггг-дд-мм",
    "ммддгггг","мм/дд/гггг","мм.дд.гггг","мм-дд-гггг",
    "ддггмм","дд/гг/мм","дд.гг.мм","дд-гг-мм",
    "ддммгг","дд/мм/гг","дд.мм.гг","дд-мм-гг",
    "ддггггмм","дд/гггг/мм","дд.гггг.мм","дд-гггг-мм",
    "ддммгггг","дд/мм/гггг","дд.мм.гггг","дд-мм-гггг"
  };
  
  
  private final JPanel contractNumbering = new JPanel(new GridBagLayout());
  private final JTextField       contractPrefix           = new JTextField();
  private final DivisionComboBox contractPrefixTypeFormat = new DivisionComboBox(formats);
  private final JTextField       contractPrefixSeparator  = new JTextField();
  
  private final JTextField       contractSuffixSeparator  = new JTextField();
  private final DivisionComboBox contractsuffixTypeFormat = new DivisionComboBox(formats);
  private final JTextField       contractSuffix           = new JTextField();
  
  private final JPanel documentNumbering = new JPanel(new GridBagLayout());
  private final DivisionTextField    contractStartNumber      = new DivisionTextField(DivisionTextField.Type.INTEGER);
  
  private JPanel                     stopPanel  = new JPanel(new GridBagLayout());
  private JCheckBox                  stopCheck  = new JCheckBox("Дата запрета отгрузки", false);
  private ComponentTitleBorder       stopBorder = new ComponentTitleBorder(stopCheck, stopPanel, BorderFactory.createLineBorder(Color.GRAY));
  private DivisionCalendarComboBox   stopDate   = new DivisionCalendarComboBox();
  
  private final JPanel numberingPanel = new JPanel(new GridBagLayout());
  private final JCheckBox individualNumberingCheck = new JCheckBox("Индивидуальная нумерация");
  private final JPanel individualnumberingpane = new JPanel(new BorderLayout());
  
  public CompanyPartitionDocumentTableEditor() {
    super(
            new String[]{
              "id",              //0
              "документ",        //1
              "префикс",         //2
              "дата",            //3
              "разделитель",     //4
              "разделитель",     //5
              "дата",            //6
              "суффикс",         //7
              "обнуление",       //8
              "<html>начальный<br/>номер</html>", //9
              "<html>занимать пропущенный<br/>(удалённый) номер</html>"  //10
            },
            new String[]{"id","document_name","prefix","prefixTypeFormat","prefixSplit",
              "suffixSplit","suffixTypeFormat","suffix","periodForZero","startNumber","grabFreeNumber"}, 
            CompanyPartitionDocument.class, 
            null, 
            "", 
            null, 
            MappingObject.Type.CURRENT);
    setRemoveActionType(MappingObject.RemoveAction.MARK_FOR_DELETE);
    initComponents();
    initEvents();
  }

  public JCheckBox getIndividualNumberingCheck() {
    return individualNumberingCheck;
  }

  public JPanel getNumberingPanel() {
    return numberingPanel;
  }

  @Override
  protected void insertData(List<List> data, int startIndex) {
    for(List d:data) {
      d.set(3,  createdTypeFormat((String)d.get(3), 3, (Integer)d.get(0)));
      d.set(6,  createdTypeFormat((String)d.get(6), 6, (Integer)d.get(0)));
      d.set(8,  createdPeriodComboBox(convert(d.get(8)), (Integer)d.get(0)));
    }
    super.insertData(data, startIndex);
  }
  
  public CompanyPartitionDocument.Period convert(Object p) {
    if(p == null)
      return null;
    if(p instanceof CompanyPartitionDocument.Period)
      return (CompanyPartitionDocument.Period)p;
    if(p instanceof Period)
      return ((Period)p).getDays() == 1 ? CompanyPartitionDocument.Period.день : ((Period)p).getMonths() == 1 ? CompanyPartitionDocument.Period.месяц : ((Period)p).getYears() == 1 ? CompanyPartitionDocument.Period.год : null;
    if(p instanceof String) {
      try {
        return convert(Period.parse((String)p));
      }catch(Exception ex) { }
      try {
        return CompanyPartitionDocument.Period.valueOf((String)p);
      }catch(Exception ex) { }
    }
    return null;
  }

  public Integer getCompanyPartitionId() {
    return companyPartitionId;
  }

  public void setCompanyPartitionId(Integer companyPartitionId) {
    this.companyPartitionId = companyPartitionId;
    companyPartitionDocTemplateTable.setCompanyPartitionId(companyPartitionId);
    getClientFilter().clear().AND_EQUAL("partition", companyPartitionId);
  }
  
  private DivisionComboBox createdTypeFormat(String value, final int column, final Integer id) {
    final DivisionComboBox formatField = new DivisionComboBox();
    formatField.addItems(formats);
    if(value == null)
      formatField.setSelectedIndex(-1);
    else formatField.setSelectedItem(value);
    
    formatField.addItemListener((ItemEvent e) -> {
      if(e.getStateChange() == ItemEvent.SELECTED) {
        try {
          CompanyPartitionDocument companyPartitionDocument = (CompanyPartitionDocument) ObjectLoader.getObject(CompanyPartitionDocument.class, id);
          String data = (String)formatField.getSelectedItem();
          if(data == null || data.equals(" "))
            data = null;
          if(column == 3)
            companyPartitionDocument.setPrefixTypeFormat(data);
          else if(column == 6)
            companyPartitionDocument.setSuffixTypeFormat(data);
          ObjectLoader.saveObject(companyPartitionDocument, true);
        } catch (Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    return formatField;
  }
  
  private DivisionComboBox createdPeriodComboBox(CompanyPartitionDocument.Period period, final Integer id) {
    DivisionComboBox comboBox = new DivisionComboBox(
            new Object[]{CompanyPartitionDocument.Period.день,CompanyPartitionDocument.Period.месяц,CompanyPartitionDocument.Period.год}
    );
    comboBox.setSelectedItem(period);
    comboBox.addItemListener((ItemEvent e) -> {
      if(e.getStateChange() == ItemEvent.SELECTED) {
        try {
          CompanyPartitionDocument companyPartitionDocument = (CompanyPartitionDocument) ObjectLoader.getObject(CompanyPartitionDocument.class, id);
          CompanyPartitionDocument.Period p = (CompanyPartitionDocument.Period) e.getItem();
          companyPartitionDocument.setPeriodForZero(p);
          ObjectLoader.saveObject(companyPartitionDocument, true);
        } catch (Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    return comboBox;
  }

  private void initComponents() {
    addSubEditorToStore(companyPartitionDocTemplateTable, "companyPartitionDocTemplateForCompanyPartitionDocumentTableEditor");
    
    stopPanel.setBorder(stopBorder);
    stopPanel.add(stopDate, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(3, 5, 3, 5), 0, 0));
    stopPanel.add(new JLabel("Парамметр задаётся для запрета редактирования, удаления или редактирования документов до выбранной даты."), 
                            new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 5, 3, 5), 0, 0));
    
    getStatusBar().setVisible(false);
    
    
    contractNumbering.setBorder(BorderFactory.createTitledBorder("Нумерация договоров"));
    contractNumbering.add(new JLabel("Префикс"),    new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    contractNumbering.add(new JLabel("Дата"),       new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    contractNumbering.add(new JLabel("Разделитель префикса"),       new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    contractNumbering.add(new JLabel("Разделитель суффикса"),       new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    contractNumbering.add(new JLabel("Дата"),       new GridBagConstraints(4, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    contractNumbering.add(new JLabel("Суффикс"),    new GridBagConstraints(5, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    contractNumbering.add(new JLabel("Начальный номер"),    new GridBagConstraints(6, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    
    contractNumbering.add(contractPrefix,           new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    contractNumbering.add(contractPrefixTypeFormat, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    contractNumbering.add(contractPrefixSeparator,  new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    contractNumbering.add(contractSuffixSeparator,  new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    contractNumbering.add(contractsuffixTypeFormat, new GridBagConstraints(4, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    contractNumbering.add(contractSuffix,           new GridBagConstraints(5, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    contractNumbering.add(contractStartNumber,      new GridBagConstraints(6, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    exportPath.setEditable(false);
    exportPath.setEnabled(false);
    
    JPanel exportPanel = new JPanel(new GridBagLayout());
    exportPanel.add(exportPath,            new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    exportPanel.add(selectPath,            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    
    getTopPanel().setLayout(new GridBagLayout());
    getTopPanel().add(exportPanel,       new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
                                                                                                                                                                                                
    companyPartitionDocTemplateTable.setVisibleOkButton(false);
    
    numberingPanel.add(contractNumbering, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    numberingPanel.add(getToolPanel(),    new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    numberingPanel.add(getRootPanel(),    new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    
    documentNumbering.add(stopPanel,         new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    documentNumbering.add(individualnumberingpane,            new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    
    individualnumberingpane.setBorder(new ComponentTitleBorder(individualNumberingCheck, individualnumberingpane, BorderFactory.createLineBorder(Color.DARK_GRAY)));
    individualnumberingpane.add(numberingPanel, BorderLayout.CENTER);
    
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(tabb, BorderLayout.CENTER);
    tabb.add("Нумерация документов", documentNumbering);
    tabb.add("Шаблоны документов",   companyPartitionDocTemplateTable.getGUI());
    setRootPanel(panel);
    
    getTable().setFindable(false);
    getTable().getTableModel().setColumnClass(9, Integer.class);
    getTable().setColumnEditable(2, true);
    getTable().setColumnEditable(3, true);
    getTable().setColumnEditable(4, true);
    getTable().setColumnEditable(5, true);
    getTable().setColumnEditable(6, true);
    getTable().setColumnEditable(7, true);
    getTable().setColumnEditable(8, true);
    getTable().setColumnEditable(9, true);
    getTable().setColumnEditable(10, true);
    ColumnGroup prefix = new ColumnGroup("Форимрование префикса");
    ColumnGroup suffix = new ColumnGroup("Форимрование суффикса");
    
    prefix.add(getTable().findTableColumn(2));
    prefix.add(getTable().findTableColumn(3));
    prefix.add(getTable().findTableColumn(4));
    
    
    suffix.add(getTable().findTableColumn(5));
    suffix.add(getTable().findTableColumn(6));
    suffix.add(getTable().findTableColumn(7));
    
    header.addColumnGroup(prefix);
    header.addColumnGroup(suffix);
    
    header.revalidate();
    getTable().setTableHeader(header);
    setAddFunction(true);
  }

  private void initEvents() {
    individualNumberingCheck.addItemListener((ItemEvent e) -> {
      EditorGui.setComponentEnable(getNumberingPanel(), e.getStateChange() == ItemEvent.SELECTED);
      //if(e.getStateChange() == ItemEvent.SELECTED)
        //initData();
    });
    
    stopCheck.addItemListener((ItemEvent e) -> {
      stopDate.setEnabled(e.getStateChange()==ItemEvent.SELECTED);
    });
    
    selectPath.addActionListener((ActionEvent e) -> {
      String fileName = exportPath.getText();
      JFileChooser chooser = new JFileChooser(fileName==null||fileName.equals("")?".":fileName);
      chooser.setDialogType(JFileChooser.OPEN_DIALOG);
      chooser.setDialogTitle("Задать файл экспорта");
      chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      chooser.setMultiSelectionEnabled(false);
      if(fileName != null && !fileName.equals(""))
        chooser.setSelectedFile(new File(fileName));
      chooser.showDialog(null, "Ok");
      File file = chooser.getSelectedFile();
      if(file != null) {
        try {
          fileName = file.getAbsolutePath();
          exportPath.setText(fileName);
          ExportImportUtil.setExportPath(companyPartitionId, fileName);
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    setAddAction((ActionEvent e) -> {
      TableEditor documentTableEditor = new TableEditor(
              new String[]{"id","Документ"},
              new String[]{"id","name"},
              Document.class,
              DocumentEditor.class,
              "Документы",
              MappingObject.Type.CURRENT);
      documentTableEditor.setName("DocumentTable");
      documentTableEditor.setAutoLoad(true);
      documentTableEditor.setAutoStore(true);
      documentTableEditor.initData();
      MappingObject[] documents = documentTableEditor.getObjects();
      RemoteSession session = null;
      try {
        session = ObjectLoader.createSession();
        for(MappingObject document:documents) {
          CompanyPartitionDocument companyPartitionDocument = (CompanyPartitionDocument) session.createEmptyObject(CompanyPartitionDocument.class);
          companyPartitionDocument.setDocument((Document)document);
          session.toEstablishes(getRootFilter(), companyPartitionDocument);
          session.saveObject(companyPartitionDocument);
        }
        session.commit();
      } catch (Exception ex) {
        ObjectLoader.rollBackSession(session);
        Messanger.showErrorMessage(ex);
      }
    });
    
    getTable().getTableModel().addTableModelListener((TableModelEvent e) -> {
      int row    = e.getLastRow();
      int column = e.getColumn();
      if (row >= 0 && column > 1 && column != 3 && column != 6 && column != 8) {
        Integer id1 = (Integer) getTable().getValueAt(row, 0);
        Object value = getTable().getValueAt(row, column);
        try {
          CompanyPartitionDocument companyPartitionDocument = (CompanyPartitionDocument) ObjectLoader.getObject(CompanyPartitionDocument.class, id1, true);
          switch(column) {
            case 2:
              companyPartitionDocument.setPrefix((String)value);
              break;
            case 4:
              companyPartitionDocument.setPrefixSplit((String)value);
              break;
            case 5:
              companyPartitionDocument.setSuffixSplit((String)value);
              break;
            case 7:
              companyPartitionDocument.setSuffix((String)value);
              break;
            case 9:
              companyPartitionDocument.setStartNumber((Integer)value);
              break;
            case 10:
              companyPartitionDocument.setGrabFreeNumber((boolean)value);
              break;
          }
          ObjectLoader.saveObject(companyPartitionDocument, true);
        }catch (Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
  }

  @Override
  public void initData() {
    super.initData();
    companyPartitionDocTemplateTable.initData();
    String path = ExportImportUtil.getExportPath(companyPartitionId);
    exportPath.setText(path==null?"":path);
  }
  
  public void setStopDate(Date date) {
    stopCheck.setSelected(date != null);
    stopDate.setEnabled(date != null);
    if(date != null)
      stopDate.setDateInCalendar(date);
  }
  
  public Date getStopDate() {
    return stopCheck.isSelected() ? new Date(stopDate.getDate().getTime()) : null;
  }

  public String getContractPrefix() {
    return contractPrefix.getText();
  }

  public void setContractPrefix(String contractPrefix) {
    this.contractPrefix.setText(contractPrefix);
  }

  public String getContractPrefixSeparator() {
    return contractPrefixSeparator.getText();
  }

  public void setContractPrefixSeparator(String contractPrefixSeparator) {
    this.contractPrefixSeparator.setText(contractPrefixSeparator);
  }

  public String getContractPrefixTypeFormat() {
    return contractPrefixTypeFormat.getSelectedItem()==null||contractPrefixTypeFormat.getSelectedItem().equals(" ")?null:String.valueOf(contractPrefixTypeFormat.getSelectedItem());
  }

  public void setContractPrefixTypeFormat(String contractPrefixTypeFormat) {
    this.contractPrefixTypeFormat.setSelectedItem(contractPrefixTypeFormat);
  }

  public Integer getContractStartNumber() {
    return Integer.valueOf(contractStartNumber.getText()==null||contractStartNumber.getText().equals("")?"1":contractStartNumber.getText());
  }

  public void setContractStartNumber(Integer contractStartNumber) {
    this.contractStartNumber.setText(String.valueOf(contractStartNumber));
  }

  public String getContractSuffix() {
    return contractSuffix.getText();
  }

  public void setContractSuffix(String contractSuffix) {
    this.contractSuffix.setText(contractSuffix);
  }

  public String getContractSuffixSeparator() {
    return contractSuffixSeparator.getText();
  }

  public void setContractSuffixSeparator(String contractSuffixSeparator) {
    this.contractSuffixSeparator.setText(contractSuffixSeparator);
  }

  public String getContractsuffixTypeFormat() {
    return contractsuffixTypeFormat.getSelectedItem()==null||contractsuffixTypeFormat.getSelectedItem().equals(" ")?null:String.valueOf(contractsuffixTypeFormat.getSelectedItem());
  }

  public void setContractsuffixTypeFormat(String contractsuffixTypeFormat) {
    this.contractsuffixTypeFormat.setSelectedItem(contractsuffixTypeFormat);
  }
}