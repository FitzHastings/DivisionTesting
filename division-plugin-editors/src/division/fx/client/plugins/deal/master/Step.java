package division.fx.client.plugins.deal.master;

import division.fx.FXUtility;
import division.fx.gui.FXStorable;
import division.util.IDStore;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class Step extends BorderPane implements FXStorable {
  private MasterPane master;
  private final Long    longId = IDStore.createID();
  private final String  title;
  protected boolean init = false;
  
  public Step(String title) {
    this(title, null);
  }

  public Step(String title, Node node) {
    FXUtility.initMainCss(this);
    this.title = title;
    if(node != null)
      setCenter(node);
  }

  public MasterPane getMaster() {
    return master;
  }

  public void setMaster(MasterPane master) {
    this.master = master;
  }

  public String getTitle() {
    return title;
  }

  public Long getLongId() {
    return longId;
  }

  public boolean isInit() {
    return init;
  }
  
  public boolean start() {
    return true;
  }
  
  public boolean end() {
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Step && ((Step)obj).getLongId().equals(getLongId());
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 47 * hash + Objects.hashCode(this.longId);
    return hash;
  }

  private final ObservableList<Node> stories = FXCollections.observableArrayList();

  @Override
  public ObservableList<Node> storeControls() {
    return stories;
  }

  void startCurrent() {
  }
}