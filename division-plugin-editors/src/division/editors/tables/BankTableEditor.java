package division.editors.tables;

import bum.editors.EditorGui;
import bum.editors.EditorListener;
import bum.editors.TableEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Bank;
import bum.interfaces.Region;
import bum.util.SimpleProcessingImpl;
import division.editors.objects.BankEditor;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionToolButton;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import util.RemoteSession;

public class BankTableEditor extends TableEditor {
  private JSplitPane split = new JSplitPane();
  private TableEditor groupBankTableEditor = new TableEditor(new String[]{"id","Код","Наименование","Город"}, new String[]{"id","regionCode","name","town"}, Region.class, null, "Группы банков");

  private DivisionToolButton importBanks = new DivisionToolButton("Иморт банков", "Иморт банков");
  private SimpleProcessingImpl processing;
  
  public BankTableEditor() {
    super(new String[]{"id","Наименование","Город","БИК","Кор. счёт"},new String[]{"id","name","town","bik","corrAccount"},Bank.class, BankEditor.class, "Банки");
    getTable().getTableFilters().addTextFilter(1);
    initCompanyTableEditorComponents();
    initEvents();
    try {
      processing = new SimpleProcessingImpl();
    }catch(RemoteException ex) {
      Messanger.showErrorMessage(ex);
    }
    addComponentToStore(split, "split");
    setName("BankTable");
  }

  @Override
  public void initData() {
    groupBankTableEditor.setSortFields(new String[]{"name"});
    groupBankTableEditor.initData();
  }

  private void initEvents() {
    importBanks.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Укажите путь к хранилищу банков");
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.showOpenDialog(getRootPanel());
        final String path  = fileChooser.getSelectedFile().getAbsolutePath();
        final File regions = new File(path+File.separator+"reg.txt");
        final File banks   = new File(path+File.separator+"bnkseek.txt");
        
        BufferedReader reader = null;
        try {
          reader = new BufferedReader(new InputStreamReader(new FileInputStream(regions), "Cp1251"));
          int max = 0;
          while(reader.readLine() != null)
            max++;
          reader = new BufferedReader(new InputStreamReader(new FileInputStream(banks), "Cp1251"));
          while(reader.readLine() != null)
            max++;
          processing.setMinMax(0, max);
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }finally {
          try {
            reader.close();;
            reader = null;
          }catch(Exception ex){}
        }

        processing.submit(() -> {
          RemoteSession session = null;
          BufferedReader reader1 = null;
          try {
            reader1 = new BufferedReader(new InputStreamReader(new FileInputStream(regions), "Cp1251"));
            session = ObjectLoader.createSession();
            String line;
            while ((line = reader1.readLine()) != null) {
              processing.setValue(processing.getProgressBar().getValue()+1);
              processing.setText("Осталось импортировать "+(processing.getProgressBar().getMaximum()-processing.getProgressBar().getValue())+" объектов");
              String[] row = line.split("\t");
              List<List> rez = session.executeQuery("SELECT id FROM [Region] "+
                      "WHERE [Region(regionCode)]='"+row[0]+"'");
              
              if(rez.isEmpty())
                session.executeUpdate("INSERT INTO [Region]"+
                        "([Region(name)],[Region(regionCode)]"+(row.length>2?",[Region(town)]":"")+") " +
                        "VALUES('"+row[1]+"','"+row[0]+"'"+(row.length>2?",'"+row[2]+"'":"")+")");
              else session.executeUpdate("UPDATE [Region] SET " +
                      "[Region(name)]='"+row[1]+(row.length>2?",[Region(town)]='"+row[2]+"'":"'")+
                      " WHERE [Region(regionCode)]='"+row[0]+"'");
            }
            reader1 = new BufferedReader(new InputStreamReader(new FileInputStream(banks), "Cp1251"));
            while ((line = reader1.readLine()) != null) {
              processing.setValue(processing.getProgressBar().getValue()+1);
              processing.setText("Осталось импортировать "+(processing.getProgressBar().getMaximum()-processing.getProgressBar().getValue())+" объектов");
              String[] row = line.split("\t");
              List<List> rez = session.executeQuery("SELECT id FROM [Bank]"+
                      " WHERE [Bank(bik)]='"+row[5]+"'");
              if(rez.isEmpty())
                session.executeUpdate("INSERT INTO [Bank] " +
                        "([Bank(address)],[Bank(bik)],"+
                        (row.length>6?"[Bank(corrAccount)],":"")+
                        "[Bank(region)]," +
                        "name) " +
                        "VALUES('"+row[1]+"','"+
                        row[5]+"',"+
                        (row.length>6?"'"+row[6]+"',":"")+
                        "(SELECT id FROM [Region]"+
                        " WHERE [Region(regionCode)]='"+row[5].substring(2, 4)+"'), '"+
                        row[3]+"')");
              else session.executeUpdate("UPDATE [Bank] SET " +
                      "[Bank(name)]='"+row[3]+"', [Bank(address)]='"+row[1]+
                      (row.length>6?", [Bank(corrAccount)]='"+row[6]+"'":"'")+
                      " WHERE [Bank(bik)]='"+row[5]+"'");
            }
            session.commit();
            groupBankTableEditor.initData();
          }catch(Exception ex) {
            ObjectLoader.rollBackSession(session);
            Messanger.showErrorMessage(ex);
          } finally {
            try {
              reader1.close();
              reader1 = null;
            }catch(Exception ex){}
          }
        });
      }
    });

    groupBankTableEditor.addEditorListener(new EditorListener() {
      @Override
      public void changeSelection(EditorGui editor, Integer[] ids) {
        getClientFilter().clear();
        getClientFilter().AND_IN("region", ids);
        BankTableEditor.super.initData();
      }
    });
  }

  private void initCompanyTableEditorComponents() {
    getRootPanel().removeAll();
    getRootPanel().add(split, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 0, 5), 0, 0));
    JPanel panel = new JPanel(new GridBagLayout());
    
    getToolBar().getComponent(0).setEnabled(false);
    getToolBar().add(importBanks);
    
    panel.add(getToolBar(), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    panel.add(progressBar, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    panel.add(scroll, new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    
    split.add(groupBankTableEditor.getRootPanel(),JSplitPane.LEFT);
    split.add(panel,JSplitPane.RIGHT);
  }
  
  @Override
  public void dispose() {
    groupBankTableEditor.dispose();
    super.dispose();
  }

  @Override
  public String getEmptyObjectTitle() {
    return "[банки]";
  }
}