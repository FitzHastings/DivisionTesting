package documents;

import division.fx.FXScriptPanel;
import division.fx.FXUtility;
import division.fx.PropertyMap;
import division.swing.guimessanger.Messanger;
import division.util.FileLoader;
import java.awt.Component;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.xml.transform.stream.StreamSource;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.render.awt.AWTRenderer;
import org.apache.fop.render.awt.viewer.PageChangeEvent;
import org.apache.fop.render.awt.viewer.PreviewPanel;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public class FXPreview extends Stage {
  private String docname;
  private final Label  page        = new Label();
  private final ToolButton nextPage    = new ToolButton("Следующая страница",  "next-page");
  private final ToolButton previosPage = new ToolButton("Предидущая страница", "prev-page");

  private final ToolButton print = new ToolButton("Печать документа",     "print-button");
  private final ToolButton pdf   = new ToolButton("Экспорт в PDF",        "export-pdf");
  private final ToolButton fo    = new ToolButton("Экспорт в XSL-FO",     "export-fo");
  private final ToolButton rtf   = new ToolButton("Экспорт в RTF",        "export-rtf");
  private final ToolButton png   = new ToolButton("Экспорт в PNG",        "export-png");
  
  private final Slider zoom = new Slider(0, 400, 100);
  
  private final ToolBar tools = new ToolBar( print, new Separator(), pdf,fo,rtf,png);
  private final ToolBar pagetools = new ToolBar(
          page,
          new Separator(),
          previosPage,
          nextPage,
          new Separator(),
          zoom);
  
  private final HBox toolPanel = new HBox(0, tools, pagetools);
  
  private final ToolBar custometool = new ToolBar();
  
  private final FOUserAgent agent          = FOP.getFopFactory().newFOUserAgent();
  private final AWTRenderer renderer       = new AWTRenderer(agent);
  
  private SwingNode  panel = new SwingNode();
  private final PreviewPanel documentpanel = new PreviewPanel(agent, null, renderer);
  private Button swipebButton = new Button("Показать редактор");
  private FXScriptPanel scriptpanel = new FXScriptPanel(SyntaxConstants.SYNTAX_STYLE_XML);
  private SplitPane split = new SplitPane(new BorderPane(panel, null, null, swipebButton, null), scriptpanel);
  private final BorderPane root  = new BorderPane(split, toolPanel, null, null, null);
  
  class ToolButton extends Button {
    public ToolButton(String toolTipText, String... classes) {
      setTooltip(new Tooltip(toolTipText));
      getStyleClass().add("tool-button");
      getStyleClass().addAll(classes);
    }
  }

  public FXPreview(String docname, Window owner, Modality modality) {
    this.docname = docname;
    panel.setContent(documentpanel);
    split.setOrientation(Orientation.VERTICAL);
    setScene(new Scene(root, 1000, 800));
    initOwner(owner);
    initModality(modality);
    FXUtility.initCss(this);
    FXUtility.copyStylesheets(owner, getScene());
    getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F5), () -> FXUtility.reloadCss(getScene()));
    
    tools.setMaxHeight(Double.MAX_VALUE);
    custometool.setMaxHeight(Double.MAX_VALUE);
    pagetools.setMaxHeight(Double.MAX_VALUE);
    HBox.setHgrow(pagetools, Priority.ALWAYS);
    
    documentpanel.addPageChangeListener((PageChangeEvent pce) -> {
      Platform.runLater(() -> {
        previosPage.setDisable(documentpanel.getPage() == 0);
        nextPage.setDisable(documentpanel.getPage() == renderer.getNumberOfPages()-1);
        page.setText("страница: "+(documentpanel.getPage()+1)+"/"+renderer.getNumberOfPages());
      });
    });
    
    SwingUtilities.invokeLater(() -> {
      for(Component com:documentpanel.getComponents())
        if(com instanceof JScrollPane)
          ((JScrollPane)com).getVerticalScrollBar().setUnitIncrement(50);
    });
    
    nextPage.setOnAction(e -> SwingUtilities.invokeLater(() -> documentpanel.setPage(documentpanel.getPage()+1)));
    previosPage.setOnAction(e -> SwingUtilities.invokeLater(() -> documentpanel.setPage(documentpanel.getPage()-1)));
    
    zoom.setPrefWidth(500);
    zoom.setBlockIncrement(20);
    zoom.setMajorTickUnit(100);
    zoom.setSnapToTicks(true);
    zoom.setShowTickLabels(true);
    zoom.setShowTickMarks(true);
    
    zoom.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
      SwingUtilities.invokeLater(() -> {
        double scale = newValue == null ? 0 : newValue.doubleValue()/100;
        if(scale > 0) {
          documentpanel.setScaleFactor(scale);
          documentpanel.reload();
        }
      });
    });
    
    fo.setOnAction(e -> export(MimeConstants.MIME_XSL_FO));
    png.setOnAction(e -> export(MimeConstants.MIME_PNG));
    rtf.setOnAction(e -> export(MimeConstants.MIME_RTF));
    pdf.setOnAction(e -> export(MimeConstants.MIME_PDF));
    print.setOnAction(e -> startPrinterJob(true));
    
    panel.addEventHandler(ScrollEvent.SCROLL, e -> {
      if(e.isControlDown()) {
        double scale = zoom.getValue() + (e.getDeltaY() < 0 ? -1 : 1)*20;
        if(scale > zoom.getMin() && scale <= zoom.getMax())
          zoom.setValue(scale);
      }
    });
    
    agent.setRendererOverride(renderer);
    scriptpanel.textProperty().addListener((ObservableValue<? extends String> observable, String oxml, String nxml) -> privew(nxml));
    
    swipebButton.setOnAction(e -> {
      if(split.getDividers().get(0).getPosition() >= 0.9) {
        new Timeline(new KeyFrame(Duration.millis(200), 
                new KeyValue(split.getDividers().get(0).positionProperty(), 0),
                new KeyValue(split.getDividers().get(0).positionProperty(), 0.5))).play();
      }else {
        new Timeline(new KeyFrame(Duration.millis(200), 
                new KeyValue(split.getDividers().get(0).positionProperty(), split.getDividers().get(0).getPosition()),
                new KeyValue(split.getDividers().get(0).positionProperty(), 1))).play();
      }
    });
    
    custometool.getItems().addListener((ListChangeListener.Change<? extends Node> c) -> {
      if(!custometool.getItems().isEmpty() && !toolPanel.getChildren().contains(custometool))
        toolPanel.getChildren().add(1, custometool);
      if(custometool.getItems().isEmpty())
        toolPanel.getChildren().remove(custometool);
    });
    
    swipebButton.textProperty().bind(Bindings.createStringBinding(() -> split.getDividers().get(0).getPosition() >= 0.9 ? "Показать редактор" : "Скрыть редактор", split.getDividers().get(0).positionProperty()));
    
    swipebButton.setMaxWidth(Integer.MAX_VALUE);
    scriptpanel.setMinHeight(0);
    setOnShown(e -> split.setDividerPositions(1));
  }
  
  public ToolBar getCustomTools() {
    return custometool;
  }
  
  public void setXML(String xml) {
    scriptpanel.setText(xml);
  }
  
  private int verticalscrollvalue   = 0;
  private int horizontalscrollvalue = 0;

  private void privew(String xml) {
    try {
      for(Component com:documentpanel.getComponents()) {
        if(com instanceof JScrollPane) {
          verticalscrollvalue = ((JScrollPane)com).getVerticalScrollBar().getValue();
          horizontalscrollvalue = ((JScrollPane)com).getHorizontalScrollBar().getValue();
        }
      }
      
      renderer.clearViewportList();
      FOP.transform(FOP.createXSLStream(), new StreamSource(new StringReader(xml)) , MimeConstants.MIME_FOP_AWT_PREVIEW, agent, null);
      documentpanel.setDisplayMode(PreviewPanel.CONTINUOUS);
      documentpanel.reload();
      previosPage.setDisable(renderer.getNumberOfPages() == 1);
      nextPage.setDisable(renderer.getNumberOfPages() == 1);
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  PropertyMap types = PropertyMap.create()
          .setValue(MimeConstants.MIME_PDF,    new ExtensionFilter("Экспорт в PDF",    "pdf", MimeConstants.MIME_PDF))
          .setValue(MimeConstants.MIME_RTF,    new ExtensionFilter("Экспорт в RTF",    "rtf", MimeConstants.MIME_RTF))
          .setValue(MimeConstants.MIME_PNG,    new ExtensionFilter("Экспорт в PNG",    "png", MimeConstants.MIME_PNG))
          .setValue(MimeConstants.MIME_XSL_FO, new ExtensionFilter("Экспорт в XSL-FO", "fo",  MimeConstants.MIME_XSL_FO));
  
  private void export(String type) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.getExtensionFilters().addAll(types.getSimpleMap().values().toArray(new ExtensionFilter[0]));
    fileChooser.setSelectedExtensionFilter(types.getValue(type, ExtensionFilter.class));
    fileChooser.setTitle("Укажите путь");
    fileChooser.setInitialFileName(docname == null ? "export_document_"+System.currentTimeMillis() : docname);
    fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
    File file = fileChooser.showSaveDialog(this);
    if(file != null) {
      try {
        file = new File(file.getAbsolutePath()+"."+fileChooser.getSelectedExtensionFilter().getExtensions().get(0));
        if(fileChooser.getSelectedExtensionFilter().equals(types.getValue(MimeConstants.MIME_XSL_FO))) {
          String xslfo = FOP.get_XSLFO_string_from_XML(scriptpanel.textProperty().getValue());
          FileLoader.createFileIfNotExists(file.getAbsolutePath());
          FileOutputStream out = new FileOutputStream(file);
          out.write(xslfo.getBytes("UTF8"));
          out.flush();
          out.close();
        }else {
          FOP.export_from_XML(file.getAbsolutePath(), scriptpanel.textProperty().getValue(), fileChooser.getSelectedExtensionFilter().getExtensions().get(1));
        }
      }catch(Exception ex) {
        ex.printStackTrace(System.out);
      }
    }
  }
  
  public void startPrinterJob(boolean showDialog) {
    PrinterJob pj = PrinterJob.getPrinterJob();
    pj.setPageable(renderer);
    if(!showDialog || pj.printDialog()) {
      try {
        pj.print();
      }catch (PrinterException e) {
        e.printStackTrace();
      }
    }
  }
}