package division.editors.objects.company;

import bum.editors.EditorGui;
import bum.editors.TableEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.CFC;
import bum.interfaces.Company;
import division.editors.tables.CFCTableEditor;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionSplitPane;
import division.util.FXArrays;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import mapping.MappingObject;
import util.filter.local.DBFilter;

public class CompanySelectionEditor extends TableEditor {
  private final DivisionSplitPane split = new DivisionSplitPane();
  private final CFCTableEditor cfcTableEditor = new CFCTableEditor();
  private DBFilter companyUnion;

  public CompanySelectionEditor() {
    super(new String[]{"id","Наименование","Инн"},
            new String[]{"id","name","inn"},
            Company.class, nCompanyEditor.class,
            "Выбор предприятия",
            MappingObject.Type.CURRENT);

    JPanel RPanel = new JPanel(new GridBagLayout());
    RPanel.add(getToolPanel(), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    RPanel.add(getRootPanel(),  new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    RPanel.add(getStatusBar(),  new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 0), 0, 0));

    addComponentToStore(split);
    cfcTableEditor.getTree().setHierarchy(true);
    cfcTableEditor.setVisibleOkButton(false);
    split.add(cfcTableEditor.getGUI(), JSplitPane.LEFT);
    split.add(RPanel, JSplitPane.RIGHT);
    split.setOneTouchExpandable(true);
    
    getTable().getTableFilters().addTextFilter(1);
    getTable().getTableFilters().addTextFilter(2);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(split, BorderLayout.CENTER);
    setRootPanel(panel);

    setDoubleClickSelectable(true);
    cfcTableEditor.addEditorListener(this);
    
    cfcTableEditor.setAdministration(!ObjectLoader.getClient().isPartner());
  }

  @Override
  public void changeSelection(EditorGui editor, Integer[] ids) {
    changeCfc(ids);
  }

  @Override
  public void initDataComplited(EditorGui editor) {
    try {
      Integer workerId = ObjectLoader.getClient().getWorkerId();
      try {
        List<List> data = ObjectLoader.executeQuery("SELECT [Worker(cfc)] FROM [Worker] WHERE id="+workerId, true);
        if(!data.isEmpty())
          cfcTableEditor.setSelectedObjects(new Integer[]{(Integer) data.get(0).get(0)});
      } catch (Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void changeCfc(Integer[] ids) {
    if(companyUnion == null)
      companyUnion = getClientFilter().AND_FILTER();
    companyUnion.clear();
    if(ObjectLoader.getClient().isPartner())
      ids = FXArrays.retainAll(ObjectLoader.getChilds(CFC.class, ObjectLoader.getClient().getCfcId()), ids);
    if(cfcTableEditor.getSelectedObjectsCount() > 0 || ids.length > 0)
      companyUnion.AND_IN("cfcs", ids);
    setSortFields(new String[]{"name"});
    super.initData();
  }

  public MappingObject[] getSelectedCFC() {
    return cfcTableEditor.getSelectedObjects();
  }
  
  public Integer[] getSelectedCFCIds() {
    return cfcTableEditor.getSelectedId();
  }

  @Override
  public void initData() {
    if(ObjectLoader.getClient().isPartner())
      cfcTableEditor.getClientFilter().clear().AND_IN("id", ObjectLoader.getChilds(CFC.class, ObjectLoader.getClient().getCfcId()));
    cfcTableEditor.initData();
  }
}