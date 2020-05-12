package division.editors.products;

import bum.editors.EditorGui;
import division.swing.DivisionSplitPane;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

public class CatalogEditor extends EditorGui {
  private final JTabbedPane tabProduct = new JTabbedPane();
  private final ProductTree currentProductTree = new ProductTree();
  private final ProductTree archiveProductTree = new ProductTree(true);
  
  private final DivisionSplitPane splitBottom = new DivisionSplitPane(JSplitPane.VERTICAL_SPLIT);
  
  private final JTabbedPane tab = new JTabbedPane();
  private final TechPassport         techPassport             = new TechPassport();
  private final ProductCalculator    productFactorTableEditor = new ProductCalculator();
  private final ProductDocumentPanel productDocumentPanel     = new ProductDocumentPanel();

  public CatalogEditor() {
    super("Каталог", null);
    initComponents();
    initEvents();
  }

  private void initComponents() {
    currentProductTree.setVisibleOkButton(false);
    archiveProductTree.setVisibleOkButton(false);
    
    productDocumentPanel.setVisibleOkButton(false);
    techPassport.setVisibleOkButton(false);
    productFactorTableEditor.setVisibleOkButton(false);
    
    tab.add("Документооборот",     productDocumentPanel.getGUI());
    tab.add("Тех. паспорт",        techPassport.getGUI());
    tab.add("Калькулятор",         productFactorTableEditor.getGUI());
    
    tabProduct.add("Текущий",  currentProductTree.getGUI());
    tabProduct.add("Архивный", archiveProductTree.getGUI());
    
    splitBottom.add(tabProduct, JSplitPane.TOP);
    splitBottom.add(tab,        JSplitPane.BOTTOM);

    getRootPanel().setLayout(new BorderLayout());
    getRootPanel().add(splitBottom, BorderLayout.CENTER);
    
    addSubEditorToStore(currentProductTree, "currentProductTreeForCatalogEditor");
    addSubEditorToStore(archiveProductTree, "archiveProductTreeForCatalogEditor");
    addSubEditorToStore(techPassport);
    getComponentsToStore().addAll(productFactorTableEditor.getComponentsToStore());
    getComponentsToStore().addAll(productDocumentPanel.getComponentsToStore());
    
    addComponentToStore(splitBottom, "splitBottom");
    
    splitBottom.setBorder(BorderFactory.createEmptyBorder());
  }

  @Override
  public void setActive(boolean active) {
    super.setActive(active);
    if(!isActive()) {
      currentProductTree.setActive(false);
      archiveProductTree.setActive(false);
      productDocumentPanel.setActive(false);
      techPassport.setActive(false);
      productFactorTableEditor.setActive(false);
    }else {
      changeTab();
      changeProductTree();
    }
  }

  private void initEvents() {
    currentProductTree.addEditorListener(techPassport);
    currentProductTree.addEditorListener(productDocumentPanel);
    currentProductTree.addEditorListener(productFactorTableEditor);
    
    archiveProductTree.addEditorListener(techPassport);
    archiveProductTree.addEditorListener(productDocumentPanel);
    archiveProductTree.addEditorListener(productFactorTableEditor);
    
    tab.addChangeListener(e -> changeTab());
    tabProduct.addChangeListener(e -> changeProductTree());
  }
  
  private void changeProductTree() {
    currentProductTree.setActive(false);
    archiveProductTree.setActive(false);
    
    switch(tabProduct.getSelectedIndex()) {
      case 0 :
        currentProductTree.setActive(true);
        currentProductTree.initData();
        break;
      case 1 :
        archiveProductTree.setActive(true);
        archiveProductTree.initData();
        break;
    }
  }
  
  private void changeTab() {
    techPassport.setActive(false);
    productFactorTableEditor.setActive(false);
    productDocumentPanel.setActive(false);
    
    switch(tab.getSelectedIndex()) {
      case 0 :
        productDocumentPanel.setActive(true);
        productDocumentPanel.initData();
        break;
      case 1 :
        techPassport.setActive(true);
        techPassport.initData();
        break;
      case 2 :
        productFactorTableEditor.setActive(true);
        productFactorTableEditor.initData();
        break;
    }
  }
  
  @Override
  public synchronized void initData() {
  }

  @Override
  public Boolean okButtonAction() {
    return true;
  }

  @Override
  public void dispose() {
    productFactorTableEditor.dispose();
    productDocumentPanel.dispose();
    techPassport.dispose();
    currentProductTree.dispose();
    archiveProductTree.dispose();
    super.dispose();
  }

  @Override
  public void initTargets() {
  }
}