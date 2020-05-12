package division.fx.desktop;

import division.fx.FXUtility;
import division.fx.util.MsgTrash;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class FXInternalFrame extends StackPane {
  private final Label  closeButton        = new Label();
  private final Label  minimizeButton     = new Label();
  private final Label  maximizeButton     = new Label();
  private final HBox   buttonBar          = new HBox(5, minimizeButton, maximizeButton, closeButton);
  
  private final BorderPane windowPane = new BorderPane();
  private final BorderPane titleBar   = new BorderPane();
  private final Label      title      = new Label();
  
  private final BooleanProperty minimizeProperty = new SimpleBooleanProperty(false);
  private final BooleanProperty maximazeProperty = new SimpleBooleanProperty(false);
  private final BooleanProperty closedProperty    = new SimpleBooleanProperty(false);
  
  private boolean RESIZE_W;
  private boolean RESIZE_N;
  private boolean RESIZE_E;
  private boolean RESIZE_S;
  
  private final Delta resizeDelta = new Delta();
  private double minX,maxX,minY,maxY;
  
  public FXInternalFrame() {
    this("");
  }
  
  public FXInternalFrame(String titleText) {
    this(titleText, 0, 0);
  }
  
  public FXInternalFrame(String titleText, double width, double height) {
    this(null, titleText, width, height);
  }
  
  public FXInternalFrame(Node contentPane, String titleText, double width, double height) {
    FXUtility.initCss(this);
    
    if(width > 0)
      setPrefWidth(width);
    if(height > 0)
      setPrefHeight(height);
    
    title.setText(titleText);
    titleBar.setRight(buttonBar);
    titleBar.setCenter(title);
    windowPane.setTop(titleBar);
    getChildren().add(windowPane);
    initEvents();
    
    if(contentPane != null)
      setContentPane(contentPane);
  }

  public BooleanProperty minimizeProperty() {
    return minimizeProperty;
  }

  public BooleanProperty maximazeProperty() {
    return maximazeProperty;
  }

  public BooleanProperty closedProperty() {
    return closedProperty;
  }
  
  public void setTitle(String titleText) {
    title.setText(titleText);
  }
  
  public String getTitle() {
    return title.getText();
  }
  
  public StringProperty titleProperty() {
    return title.textProperty();
  }

  public void setAllwaysOnTop(boolean allwaysOnTop) {
    if(allwaysOnTop)
      ((Pane)getParent()).getChildren().addListener((ListChangeListener.Change<? extends Node> c) -> toFront());
  }
  
  public void setContentPane(Node pane) {
    pane.getStyleClass().add("contentPane");
    windowPane.setCenter(pane);
  }
  
  public void dispose() {
    prefWidthProperty().unbind();
    prefHeightProperty().unbind();
    minimizeProperty.unbind();
    maximazeProperty.unbind();
    closedProperty().unbind();
  }
  
  private void initEvents() {
    maximazeProperty.addListener(new ChangeListener<Boolean>() {
      Bounds lastBounds = null;
      
      @Override
      public void changed(ObservableValue<? extends Boolean> ob, Boolean ol, Boolean nw) {
        prefWidthProperty().unbind();
        prefHeightProperty().unbind();
        
        toFront();
        Timeline timeline = new Timeline();
        KeyValue kvx = null, kvy = null, kvw = null, kvh = null;

        if(nw) {
          setPrefWidth(getWidth());
          setPrefHeight(getHeight());
          
          lastBounds = localToParent(getBoundsInLocal());
          kvx = new KeyValue(layoutXProperty(), 0.0);
          kvy = new KeyValue(layoutYProperty(), 0.0);
          
          kvw = new KeyValue(prefWidthProperty(), ((Pane)getParent()).widthProperty().get());
          kvh = new KeyValue(prefHeightProperty(), ((Pane)getParent()).heightProperty().get());

          timeline.setOnFinished(event -> {
            prefWidthProperty().bind(((Pane)getParent()).widthProperty());
            prefHeightProperty().bind(((Pane)getParent()).heightProperty());
          });
        }else {
          kvx = new KeyValue(layoutXProperty(), lastBounds.getMinX());
          kvy = new KeyValue(layoutYProperty(), lastBounds.getMinY());

          kvw = new KeyValue(prefWidthProperty(), lastBounds.getWidth());
          kvh = new KeyValue(prefHeightProperty(), lastBounds.getHeight());

          lastBounds = null;
        }

        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(200), kvw, kvh, kvx, kvy));
        timeline.play();
      }
    });
    
    minimizeProperty.addListener(new ChangeListener<Boolean>() {
      //double lastHeight = -1;
      double lastX, lastY;
      
      @Override
      public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        prefWidthProperty().unbind();
        prefHeightProperty().unbind();
        Timeline timeline = new Timeline();
        KeyValue kvx = null, kvy = null, kvlx = null, kvly = null;
        if(newValue) {
          lastX = getLayoutX();
          lastY = getLayoutY();
          
          kvx = new KeyValue(scaleXProperty(), 0.0);
          kvy = new KeyValue(scaleYProperty(), 0.0);
          
          kvlx = new KeyValue(layoutXProperty(), (((Pane)getParent()).getWidth()-getWidth())/2);
          kvly = new KeyValue(layoutYProperty(), ((Pane)getParent()).getHeight());
        }else {
          toFront();
          kvx = new KeyValue(scaleXProperty(), 1);
          kvy = new KeyValue(scaleYProperty(), 1);
          
          kvlx = new KeyValue(layoutXProperty(), lastX);
          kvly = new KeyValue(layoutYProperty(), lastY);
        }
        
        final KeyFrame kf = new KeyFrame(Duration.millis(200), kvx, kvy, kvlx, kvly);
        timeline.getKeyFrames().add(kf);
        timeline.play();
      }
    });
    
    closedProperty.addListener(new ChangeListener<Boolean>() {
      @Override
      public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        toFront();
        prefWidthProperty().unbind();
        prefHeightProperty().unbind();
        if(newValue) {
          Timeline timeline = new Timeline();
          KeyValue kvx = new KeyValue(scaleXProperty(), 0.0);
          KeyValue kvy = new KeyValue(scaleYProperty(), 0.0);
          KeyFrame kf = new KeyFrame(Duration.millis(200), kvx, kvy);
          timeline.getKeyFrames().add(kf);
          timeline.setOnFinished((ActionEvent event1) -> ((Pane)getParent()).getChildren().remove(this));
          timeline.play();
        }
      }
    });
    
    makeDragable(titleBar);
    makeResizable(3);
    setOnMouseClicked(mouseEvent -> toFront());  
    closeButton.setOnMouseClicked((MouseEvent event) -> {
      if(closing()) {
        try {
          finalize();
        }catch(Throwable ex){MsgTrash.out(ex);}
        closedProperty.set(true);
      }
    });
    minimizeButton.setOnMouseClicked((MouseEvent event) -> minimizeProperty.set(!minimizeProperty.get()));
    maximizeButton.setOnMouseClicked((MouseEvent event) -> maximazeProperty.set(!maximazeProperty.get()));
    titleBar.setOnMouseClicked((MouseEvent event) -> {
      if(event.getClickCount() == 2)
        maximazeProperty.set(!maximazeProperty.get());
    });
  }
  
  protected boolean closing() {
    return true;
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
  }
  
  private Delta dragDelta = null;
  
  public void makeDragable(Node what) {
    what.setOnMouseMoved(mouseEvent -> {
      dragDelta = new Delta();
      dragDelta.x = getLayoutX() - mouseEvent.getScreenX();
      dragDelta.y = getLayoutY() - mouseEvent.getScreenY();
    });
    
    what.setOnMouseDragged(mouseEvent -> {
      if(dragDelta != null) {
        toFront();
        setLayoutX(mouseEvent.getScreenX() + dragDelta.x);
        setLayoutY(mouseEvent.getScreenY() + dragDelta.y);
      }
    });
  }

  public void makeResizable(double mouseBorderWidth) {
    addEventFilter(MouseEvent.MOUSE_MOVED, mouseEvent -> {
      if(!maximazeProperty.get()) {
        setCursor(Cursor.DEFAULT);

        double mouseX = mouseEvent.getX();
        double mouseY = mouseEvent.getY();

        double width  = this.boundsInLocalProperty().get().getWidth();
        double height = this.boundsInLocalProperty().get().getHeight();

        RESIZE_N = mouseY < mouseBorderWidth;
        RESIZE_S = mouseY > height-mouseBorderWidth;
        RESIZE_W = mouseX < mouseBorderWidth;
        RESIZE_E = mouseX > width-mouseBorderWidth;
        
        this.setCursor(
                 RESIZE_N  && !RESIZE_S && !RESIZE_W && !RESIZE_E ? Cursor.N_RESIZE  : 
                 RESIZE_N  && !RESIZE_S && !RESIZE_W &&  RESIZE_E ? Cursor.NE_RESIZE :
                !RESIZE_N  && !RESIZE_S && !RESIZE_W &&  RESIZE_E ? Cursor.E_RESIZE  :
                !RESIZE_N &&   RESIZE_S && !RESIZE_W &&  RESIZE_E ? Cursor.SE_RESIZE :
                !RESIZE_N &&   RESIZE_S && !RESIZE_W && !RESIZE_E ? Cursor.S_RESIZE  :
                !RESIZE_N &&   RESIZE_S &&  RESIZE_W && !RESIZE_E ? Cursor.SW_RESIZE :
                !RESIZE_N &&  !RESIZE_S &&  RESIZE_W && !RESIZE_E ? Cursor.W_RESIZE  :
                 RESIZE_N &&  !RESIZE_S &&  RESIZE_W && !RESIZE_E ? Cursor.NW_RESIZE :Cursor.DEFAULT);
      }
    });
    
    setOnMousePressed((MouseEvent event) -> {
      resizeDelta.x = getLayoutX() - event.getScreenX();
      resizeDelta.y = getLayoutY() - event.getScreenY();
      minX = getLayoutX();
      maxX = minX + getWidth();
      minY = getLayoutY();
      maxY = minY + getHeight();
    });
    
    setOnMouseReleased((MouseEvent event) -> {
      RESIZE_N = RESIZE_S = RESIZE_W = RESIZE_E = false;
    });
    
    setOnMouseDragged(mouseEvent -> {
      if(!maximazeProperty.get()) {
        toFront();

        double mouseX = mouseEvent.getX();
        double mouseY = mouseEvent.getY();
        if(RESIZE_W) {
          setLayoutX(mouseEvent.getScreenX() + resizeDelta.x);
          setPrefWidth(maxX - getLayoutX());
        }
        if(RESIZE_N) {
          setLayoutY(mouseEvent.getScreenY() + resizeDelta.y);
          setPrefHeight(maxY-getLayoutY());
        }
        if(RESIZE_E)
          setPrefWidth(mouseX);
        if(RESIZE_S)
          setPrefHeight(mouseY);
      }
    });
  }
  
  private static class Delta {
    double x, y;
  }
}