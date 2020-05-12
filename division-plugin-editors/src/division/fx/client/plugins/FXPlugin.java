package division.fx.client.plugins;

import division.ClientMain;
import division.fx.client.ClientFilter;
import division.fx.desktop.FXDesktopPane;
import division.fx.desktop.FXInternalFrame;
import division.fx.FXUtility;
import division.fx.dialog.FXD;
import division.fx.gui.FXDisposable;
import division.fx.gui.FXStorable;
import division.fx.util.MsgTrash;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.WindowEvent;

public abstract class FXPlugin implements FXStorable, Initializable, FXDisposable {
  private StringProperty title = new SimpleStringProperty();
  private FXDesktopPane desktopPane;
  
  private final ChangeListener<? super Integer[]> cfcListener = (ob, ol, nw) -> changeCFC(nw, true);
  private final ChangeListener<? super Integer[]> companyListener = (ob, ol, nw) -> changeCompany(nw, true);
  private final ChangeListener<? super Integer[]> partitionListener = (ob, ol, nw) -> changePartition(nw, true);
  
  private Parent content;
  private String fxml;
  
  private BooleanProperty activeProperty = new SimpleBooleanProperty(false);
  private BooleanProperty pluginProperty = new SimpleBooleanProperty(true);

  public FXDesktopPane getDesktopPane() {
    return desktopPane;
  }

  public BooleanProperty pluginProperty() {
    return pluginProperty;
  }
  
  public BooleanProperty activeProperty() {
    return activeProperty;
  }

  public void setDesktopPane(FXDesktopPane desktopPane) {
    this.desktopPane = desktopPane;
  }

  public String getFxml() {
    return fxml;
  }

  public void setFxml(String fxml) {
    this.fxml = fxml;
  }
  
  public void initPlugin() {
    try {
      FXMLLoader loader;
      if(getFxml() != null) {
        loader = FXUtility.getLoader(getFxml());
        loader.setController(this);
        content = loader.load();
      }else initialize(new URL("http://localhost"), null);
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }
    
    activeProperty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(newValue) {
        ClientFilter.CFCProperty().addListener(cfcListener);
        ClientFilter.companyProperty().addListener(companyListener);
        ClientFilter.partitionProperty().addListener(partitionListener);
        changeCompany(ClientMain.getCurrentCompanys(), true);
        changeCFC(ClientMain.getCurrentCfc(), true);
      }else {
        ClientFilter.CFCProperty().removeListener(cfcListener);
        ClientFilter.companyProperty().removeListener(companyListener);
        ClientFilter.partitionProperty().removeListener(partitionListener);
      }
    });
  }

  public String getTitle() {
    return titlepProperty().getValue();
  }

  public void setTitle(String title) {
    titlepProperty().setValue(title);
  }
  
  public StringProperty titlepProperty() {
    return title;
  }
  
  public FXInternalFrame show(String title) {
    setTitle(title);
    return show();
  }
  
  public FXInternalFrame show() {
    if(getDesktopPane() == null) {
      showWindow();
      return null;
    }else {
      FXInternalFrame frame = new FXInternalFrame() {
        @Override
        protected boolean closing() {
          return FXPlugin.this.closing();
        }
      };
      frame.setContentPane(getContent());
      frame.titleProperty().bind(titlepProperty());
      load(getTitle());
      frame.closedProperty().addListener((ob, ol, nw) -> {
        if(nw)
          dispose();
      });
      getDesktopPane().add(frame);
      
      if(pluginProperty().getValue()) {
        activeProperty().unbind();
        activeProperty().bind(frame.visibleProperty());
      }
      return frame;
    }
  }
  
  protected boolean closing() {
    return true;
  }
  
  public FXD showWindow() {
    return showWindow(null);
  }
  
  private boolean dispose = false;
  
  public FXD showWindow(Node parent) {
    FXD fxd = FXD.create(titlepProperty().getValue(), parent, getContent());
    
    fxd.addEventHandler(WindowEvent.WINDOW_SHOWING, e -> {
      load(getTitle());
    });
    
    fxd.addEventHandler(WindowEvent.WINDOW_HIDING, e -> {
      store(getTitle());
    });
    
    fxd.setOnHiding(e -> {
      if(dispose) {
        dispose = false;
        dispose();
      }
    });
    fxd.setOnCloseRequest(e -> {
      if(closing())
        dispose = true;
    });
    
    if(pluginProperty().getValue()) {
      activeProperty().unbind();
      activeProperty().bind(fxd.showingProperty());
    }
    
    fxd.setAlwaysOnTop(true);
    fxd.show();
    
    return fxd;
  }
  
  public Node getContent() {
    return content;
  }
  
  public abstract void start();

  public void changeCFC(Integer[] cfc, boolean forceInit) {}
  public void changeCompany(Integer[] company, boolean forceInit) {}
  public void changePartition(Integer[] partition, boolean forceInit) {}
  
  @Override
  public void finaly() {
  }
  
  private ObservableList<Node> storeControls = FXCollections.observableArrayList();
  
  @Override
  public ObservableList<Node> storeControls() {
    return storeControls;
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
  }
  
  ObservableList<FXDisposable> disposablelist = FXCollections.observableArrayList();
  @Override
  public final ObservableList<FXDisposable> disposeList() {
    return disposablelist;
  }
}