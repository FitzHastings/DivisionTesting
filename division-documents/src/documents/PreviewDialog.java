package documents;

import division.swing.guimessanger.Messanger;
import division.swing.ScriptPanel;
import division.util.FileLoader;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.transform.stream.StreamSource;
import org.apache.batik.util.gui.resource.JToolbarButton;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.render.awt.AWTRenderer;
import org.apache.fop.render.awt.viewer.PreviewPanel;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public class PreviewDialog extends JDialog {
  private final JTabbedPane tabb = new JTabbedPane();
  
  private final JPanel viewPanel = new JPanel(new GridBagLayout());
  private final ScriptPanel scriptPanel = new ScriptPanel(SyntaxConstants.SYNTAX_STYLE_XML);
  
  private final JSlider slider = new JSlider(10, 300);
  private PreviewPanel previewPanel;
  private AWTRenderer renderer;
  private FOUserAgent agent;

  private final JToolBar tool       = new JToolBar();
  private final JLabel  page        = new JLabel();
  private final JButton nextPage    = new JButton(">>");
  private final JButton previosPage = new JButton("<<");

  private final JToolbarButton print = new JToolbarButton();
  private final JToolbarButton pdf   = new JToolbarButton();
  private final JToolbarButton fo    = new JToolbarButton();
  private final JToolbarButton rtf   = new JToolbarButton();
  private final JToolbarButton png   = new JToolbarButton();
  private final JToolbarButton ps    = new JToolbarButton();
  private final JToolbarButton email = new JToolbarButton();
  
  private final ExecutorService pool = Executors.newSingleThreadExecutor();
  
  private final FOP fop = new FOP();
  
  private String xml;

  public PreviewDialog(String xml, Window owner, String title, ModalityType modalityType, GraphicsConfiguration gc) throws Exception {
    super(owner, title, modalityType, gc);
    this.xml = xml;
    init();
  }

  public PreviewDialog(String xml, Window owner, String title, ModalityType modalityType) throws Exception {
    super(owner, title, modalityType);
    this.xml = xml;
    init();
  }

  public PreviewDialog(String xml, Window owner, String title) throws Exception {
    super(owner, title);
    this.xml = xml;
    init();
  }

  public PreviewDialog(String xml, Window owner, ModalityType modalityType) throws Exception {
    super(owner, modalityType);
    this.xml = xml;
    init();
  }

  public PreviewDialog(String xml, Window owner) throws Exception {
    super(owner);
    this.xml = xml;
    init();
  }

  public PreviewDialog(String xml, Dialog owner, String title, boolean modal, GraphicsConfiguration gc) throws Exception {
    super(owner, title, modal, gc);
    this.xml = xml;
    init();
  }

  public PreviewDialog(String xml, Dialog owner, String title, boolean modal) throws Exception {
    super(owner, title, modal);
    this.xml = xml;
    init();
  }

  public PreviewDialog(String xml, Dialog owner, String title) throws Exception {
    super(owner, title);
    this.xml = xml;
    init();
  }

  public PreviewDialog(String xml, Dialog owner, boolean modal) throws Exception {
    super(owner, modal);
    this.xml = xml;
    init();
  }

  public PreviewDialog(String xml, Dialog owner) throws Exception {
    super(owner);
    this.xml = xml;
    init();
  }

  public PreviewDialog(String xml, Frame owner, String title, boolean modal, GraphicsConfiguration gc) throws Exception {
    super(owner, title, modal, gc);
    this.xml = xml;
    init();
  }

  public PreviewDialog(String xml, Frame owner, String title, boolean modal) throws Exception {
    super(owner, title, modal);
    this.xml = xml;
    init();
  }

  public PreviewDialog(String xml, Frame owner, String title) throws Exception {
    super(owner, title);
    this.xml = xml;
    init();
  }

  public PreviewDialog(String xml, Frame owner, boolean modal) throws Exception {
    super(owner, modal);
    this.xml = xml;
    init();
  }

  public PreviewDialog(String xml, Frame owner) throws Exception {
    super(owner);
    this.xml = xml;
    init();
  }

   public PreviewDialog(String xml) throws Exception {
    this(xml, (JFrame)null);
  }
  
  @Override
  public void setModal(boolean modal) {
    super.setModal(modal);
    setAlwaysOnTop(modal);
  }
  
  public final void init() throws Exception {
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    scriptPanel.setText(xml);
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    setSize(screenSize.width-200,screenSize.height-200);
    centerLocation();
    agent = FOP.getFopFactory().newFOUserAgent();
    renderer = new AWTRenderer(agent);
    agent.setRendererOverride(renderer);
    previewPanel = new PreviewPanel(agent, null, renderer);
    preview();
    initComponents();
    initEvents();
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    setAlwaysOnTop(isModal());
  }
  
  private void preview() {
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    try {
      renderer.clearViewportList();
      FOP.transform(FOP.createXSLStream(), new StreamSource(new StringReader(scriptPanel.getText())) , MimeConstants.MIME_FOP_AWT_PREVIEW, agent, null);
      previewPanel.setDisplayMode(PreviewPanel.CONTINUOUS);
      previewPanel.reload();
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }finally {
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
  }
  
  public static void setCursor(JComponent component, Cursor cursor) {
    component.setCursor(cursor);
    for(Component comp:component.getComponents())
      if(comp instanceof JComponent)
        setCursor((JComponent)comp, cursor);
  }

  private void initComponents() {
    setLayout(new BorderLayout());
    print.setIcon(FileLoader.getIcon("print20.png"));
    print.setToolTipText("Печать");
    pdf.setIcon(FileLoader.getIcon("pdf_ico.png"));
    pdf.setToolTipText("Экспорт в PDF");
    fo.setIcon(FileLoader.getIcon("xml_ico.gif"));
    fo.setToolTipText("Экспорт в XSL-FO");
    rtf.setIcon(FileLoader.getIcon("rtf_ico2.jpg"));
    rtf.setToolTipText("Экспорт в RTF");
    ps.setIcon(FileLoader.getIcon("postscript_ico.png"));
    ps.setToolTipText("Экспорт в POSTSCRIPT");
    png.setIcon(FileLoader.getIcon("png_ico.png"));
    png.setToolTipText("Экспорт в PNG");
    email.setIcon(FileLoader.getIcon("email-send.png"));
    email.setToolTipText("Отправить по электронной почте");

    setNextPreviosButtonsEnable();

    slider.setMajorTickSpacing(100);
    slider.setMinorTickSpacing(10);

    slider.setPaintLabels(true);
    slider.setPaintTicks(true);
    slider.setPaintTrack(true);
    slider.setValue(100);

    tool.setFloatable(false);
    tool.add(print);
    tool.addSeparator();
    tool.add(pdf);
    tool.add(fo);
    tool.add(rtf);
    tool.add(png);
    tool.add(ps);
    tool.addSeparator();
    tool.add(email);
    tool.addSeparator();
    tool.add(page);
    
    email.setVisible(false);

    if(renderer.getNumberOfPages() > 1) {
      tool.addSeparator();
      tool.add(previosPage);
      tool.add(nextPage);
    }

    slider.setOrientation(JSlider.VERTICAL);
    
    for(Component com:previewPanel.getComponents())
      if(com instanceof JScrollPane)
        ((JScrollPane)com).getVerticalScrollBar().setUnitIncrement(50);
    
    add(tabb, BorderLayout.CENTER);
    
    tabb.add("Просмотр документа", viewPanel);
    tabb.add("Просмотр кода", scriptPanel);
    
    viewPanel.add(tool,         new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    viewPanel.add(slider,       new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.VERTICAL, new Insets(5, 5, 5, 5), 0, 0));
    viewPanel.add(previewPanel, new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
  }

  private void setNextPreviosButtonsEnable() {
    previosPage.setEnabled(previewPanel.getPage() > 0);
    nextPage.setEnabled((previewPanel.getPage()+1) < renderer.getNumberOfPages());
    page.setText("страница: "+(previewPanel.getPage()+1)+"/"+renderer.getNumberOfPages());
  }

  private void export(String type) {
    String end = null;

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setAcceptAllFileFilterUsed(false);
    
    switch(type) {
      case MimeConstants.MIME_PDF:
        fileChooser.setFileFilter(new FileNameExtensionFilter("файлы "+type, "pdf"));
        end = "pdf";
        break;
      case MimeConstants.MIME_XSL_FO:
        fileChooser.setFileFilter(new FileNameExtensionFilter("файлы xsl-fo", "fo"));
        end = "fo";
        break;
      case MimeConstants.MIME_RTF:
        fileChooser.setFileFilter(new FileNameExtensionFilter("файлы RTF", "rtf"));
        end = "rtf";
        break;
      case MimeConstants.MIME_RTF_ALT1:
        fileChooser.setFileFilter(new FileNameExtensionFilter("файлы RTF", "rtf"));
        end = "rtf";
        break;
      case MimeConstants.MIME_RTF_ALT2:
        fileChooser.setFileFilter(new FileNameExtensionFilter("файлы RTF", "rtf"));
        end = "rtf";
        break;
      case MimeConstants.MIME_POSTSCRIPT:
        fileChooser.setFileFilter(new FileNameExtensionFilter("файлы POSTSCRIPT", "ps"));
      end = "ps";
        break;
      case MimeConstants.MIME_PNG:
        fileChooser.setFileFilter(new FileNameExtensionFilter("файлы png", "png"));
        end = "png";
        break;
    }

    if(end != null) {
      fileChooser.setDialogTitle("Укажите путь");
      fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooser.setSelectedFile(new File("export_document_"+System.currentTimeMillis()));
      fileChooser.setApproveButtonText("Экспортировать");
      fileChooser.setApproveButtonToolTipText("Экспортировать");
      if(fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        if(file != null) {
          try {
            file = new File(file.getAbsolutePath()+"."+end);
            if(type.equals(MimeConstants.MIME_XSL_FO)) {
              String xslfo = fop.get_XSLFO_string_from_XML(scriptPanel.getText(), true);
              FileLoader.createFileIfNotExists(file.getAbsolutePath());
              FileOutputStream out = new FileOutputStream(file);
              out.write(xslfo.getBytes("UTF8"));
              out.flush();
              out.close();
            }else
              FOP.export_from_XML(file.getAbsolutePath(), scriptPanel.getText(), type);
          }catch(Exception ex) {
            ex.printStackTrace(System.out);
          }
        }
      }
    }
  }

  private void initEvents() {
    tabb.addChangeListener((ChangeEvent e) -> {
      if(tabb.getSelectedIndex() == 0) {
        preview();
      }
    });
    
    fo.addActionListener((ActionEvent e) -> {
      export(MimeConstants.MIME_XSL_FO);
    });
    
    png.addActionListener((ActionEvent e) -> {
      export(MimeConstants.MIME_PNG);
    });
    
    rtf.addActionListener((ActionEvent e) -> {
      export(MimeConstants.MIME_RTF_ALT2);
    });

    pdf.addActionListener((ActionEvent e) -> {
      export(MimeConstants.MIME_PDF);
    });
    
    ps.addActionListener((ActionEvent e) -> {
      export(MimeConstants.MIME_POSTSCRIPT);
    });

    print.addActionListener((ActionEvent e) -> {
      startPrinterJob(true);
    });

    nextPage.addActionListener((ActionEvent e) -> {
      previewPanel.setPage(previewPanel.getPage()+1);
      setNextPreviosButtonsEnable();
    });

    previosPage.addActionListener((ActionEvent e) -> {
      previewPanel.setPage(previewPanel.getPage()-1);
      setNextPreviosButtonsEnable();
    });

    slider.addChangeListener((ChangeEvent e) -> {
      double scale = (double)slider.getValue()/100;
      if(scale > 0) {
        previewPanel.setScaleFactor(scale);
        previewPanel.reload();
      }
    });
    
    for(Component com:previewPanel.getComponents()) {
      com.addMouseWheelListener((MouseWheelEvent e) -> {
        boolean isCtrl  = (e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == MouseEvent.CTRL_DOWN_MASK;
        if(isCtrl) {
          int scale = slider.getValue() + (int)e.getPreciseWheelRotation()*10;
          if(scale > slider.getMinimum() && scale <= slider.getMaximum())
            slider.setValue(scale);
        }
      });
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

  public void centerLocation() {
    try {
      Point location = getParent().getLocationOnScreen();
      int x = (int)(getParent().getSize().width - getSize().width)/2;
      int y = (int)(getParent().getSize().height - getSize().height)/2;
      this.setLocation(location.x+x, location.y+y);
    }catch(Exception ex) {
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int x = (int)(screenSize.width - getSize().width)/2;
      int y = (int)(screenSize.height - getSize().height)/2;
      setLocation(x, y);
    }
  }
  
  public void setEmailAction(ActionListener listener) {
    for(ActionListener l:email.getActionListeners())
      email.removeActionListener(l);
    email.addActionListener(listener);
    email.setVisible(true);
  }
}