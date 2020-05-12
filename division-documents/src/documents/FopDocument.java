package documents;

import java.awt.Dialog;
import java.awt.print.PrinterJob;
import java.io.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.print.PrintService;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JOptionPane;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.render.print.PrintRenderer;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.tools.generic.*;
import org.xml.sax.SAXException;

public class FopDocument {
  public static String XHTML_TO_XSLFO = "fo.xsl";
  private File templateFile;
  
  private VelocityContext     context = new VelocityContext();
  private ScriptEngine        engine  = new ScriptEngineManager().getEngineByName("JavaScript");

  private static FopFactory fopFactory = createFopFactory();

  public FopDocument() throws Exception {
    context.put("convert",    new ConversionTool());
    context.put("dateTool",   new DateTool());
    context.put("mathTool",   new MathTool());
    context.put("numberTool", new NumberTool());
    context.put("listTool",   new ListTool());

  }

  public void setTemplateFile(File templateFile) throws Exception {
    this.templateFile = templateFile.getAbsoluteFile();
  }
  
  public void setTemplateFile(String templateFileName) throws Exception {
    templateFile = new File(templateFileName).getAbsoluteFile();
  }

  public void setTemplateText(String XMLText) throws Exception {
    templateFile = createTempFile(XMLText);
  }
  
  public void preview() throws Exception {
    preview(true,true);
  }
  
  public void preview(Dialog owner) throws Exception {
    PreviewDialog dialog = new PreviewDialog(get_XML_From_XMLTemplate(), owner);
    dialog.setVisible(true);
  }
  
  public void preview(boolean modul, boolean onTop) throws Exception {
    PreviewDialog dialog = new PreviewDialog(get_XML_From_XMLTemplate());
    dialog.setModal(modul);
    dialog.setAlwaysOnTop(onTop);
    dialog.setVisible(true);
  }
  
  public static void preview(String xslfo) throws Exception {
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer = factory.newTransformer();
    Fop fop = fopFactory.newFop(MimeConstants.MIME_FOP_AWT_PREVIEW);
    Source src = new StreamSource(new StringReader(xslfo));
    transformer.transform(src, new SAXResult(fop.getDefaultHandler()));
  }

  public static void print(String xslfo, PrintRenderer printRenderer) throws Exception {
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer = factory.newTransformer();
    Fop fop;
    if(printRenderer != null) {
      FOUserAgent userAgent = fopFactory.newFOUserAgent();
      userAgent.setRendererOverride(printRenderer);
      //printRenderer.setUserAgent(userAgent);
      userAgent.setRendererOverride(printRenderer);
      fop = fopFactory.newFop(MimeConstants.MIME_FOP_PRINT,userAgent);
    }else fop = fopFactory.newFop(MimeConstants.MIME_FOP_PRINT);
    Source src = new StreamSource(new StringReader(xslfo));
    transformer.transform(src, new SAXResult(fop.getDefaultHandler()));
  }

  public synchronized File getDocument(String fileName, String type) throws Exception {
    File file = new File(new File(fileName).getAbsolutePath());
    File dir = new File(file.getParent());
    dir.mkdirs();
    Transformer transformer = TransformerFactory.newInstance().newTemplates(createXSLStream()).newTransformer();
    FileOutputStream out = new FileOutputStream(file);
    FOUserAgent userAgent = fopFactory.newFOUserAgent();
    Fop fop = fopFactory.newFop(type,userAgent,out);
    transformer.transform(createJSVelocityStream(), new SAXResult(fop.getDefaultHandler()));
    out.flush();
    out.close();
    out = null;
    return file;
  }
  
  public static PrintService getPrintService() {
    PrinterJob printerJob = PrinterJob.getPrinterJob();
    if(printerJob.printDialog())
      return printerJob.getPrintService();
    return null;
  }

  public static PrintRenderer getPrintRenderer() {
    PrintRenderer printRenderer = null;
    final PrinterJob printerJob = PrinterJob.getPrinterJob();
    //if(printerJob.printDialog())
      //printRenderer = new PrintRenderer(printerJob);
    printRenderer = new PrintRenderer(fopFactory.newFOUserAgent());
    return printRenderer;
  }
  
  public void print(PrintRenderer printRenderer) throws Exception {
    Transformer transformer = TransformerFactory.newInstance().newTemplates(createXSLStream()).newTransformer();
		Fop fop;
		if(printRenderer != null) {
			FOUserAgent userAgent = fopFactory.newFOUserAgent();
                        userAgent.setRendererOverride(printRenderer);
			//printRenderer.setUserAgent(userAgent);
			userAgent.setRendererOverride(printRenderer);
			fop = fopFactory.newFop(MimeConstants.MIME_FOP_PRINT,userAgent);
		}else fop = fopFactory.newFop(MimeConstants.MIME_FOP_PRINT);
    transformer.transform(createJSVelocityStream(), new SAXResult(fop.getDefaultHandler()));
  }

  public void print() throws Exception {
    print(null);
  }

  public synchronized String getXSLFOString() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer = factory.newTransformer(createXSLStream());
    Source src = createJSVelocityStream();
    Result res = new StreamResult(out);
    transformer.transform(src, res);
    String xslfo = out.toString();
    out.reset();
    out.flush();
    out.close();
    out = null;
    return xslfo;
  }

  public File getXSLFOFile(String FoName)throws Exception {
    File file = new File(FoName);
    OutputStream out = new FileOutputStream(file);
    try {
      //Setup XSLT
      TransformerFactory factory = TransformerFactory.newInstance();
      Transformer transformer = factory.newTransformer(createXSLStream());

      //Setup input for XSLT transformation
      Source src = createJSVelocityStream();

      //Resulting SAX events (the generated FO) must be piped through to FOP
      Result res = new StreamResult(out);

      //Start XSLT transformation and FOP processing
      transformer.transform(src, res);
    }finally {
      out.flush();
      out.close();
      out = null;
    }
    return file;
  }

  public void addVariable(String name, Object value) {
    engine.put(name, value);
    context.put(name, value);
  }
  
  public void addJSModul(String jsmodul) throws ScriptException {
    engine.eval(jsmodul);
  }
  
  public void addJSModul(File jsfile) throws FileNotFoundException, ScriptException {
    engine.eval(new FileReader(jsfile));
  }
  
  private String get_XML_From_XMLTemplate() throws Exception {
    FileInputStream in = new FileInputStream(templateFile);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int b;
    while((b = in.read()) != -1)
      baos.write(b);
    in.close();
    in = null;
    String temp = baos.toString("UTF8");
    baos.reset();
    baos.flush();
    baos.close();
    baos = null;

    Pattern p = Pattern.compile("(<js>([\\s\\S]*?)</js>)");
    Matcher m = p.matcher(temp);
    while(m.find()) {
      JSDocument document = new JSDocument();
      engine.put("document", document);
      engine.put("vcontext", context);
      engine.eval(m.group(2));
      temp = temp.replace(m.group(1), document.read());
    }

    File tempFile = createTempFile(temp);

    Velocity.setProperty("file.resource.loader.path", tempFile.getParent());
    Velocity.setProperty("file.resource.loader.cache", false);
    Velocity.init();
    Template template = Velocity.getTemplate(tempFile.getName(),"UTF-8");
    StringWriter writer = new StringWriter();
    template.merge(context, writer);
    String xml = writer.toString();
    writer.flush();
    writer.close();
    writer = null;
    return xml;
  }

  private StreamSource createJSVelocityStream() throws Exception {
    FileInputStream in = new FileInputStream(templateFile);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int b;
    while((b = in.read()) != -1)
      baos.write(b);
    in.close();
    in = null;
    String temp = baos.toString("UTF8");
    
    baos.reset();
    baos.flush();
    baos.close();
    baos = null;

    Pattern p = Pattern.compile("(<js>([\\s\\S]*?)</js>)");
    Matcher m = p.matcher(temp);
    while(m.find()) {
      JSDocument document = new JSDocument();
      engine.put("document", document);
      engine.put("vcontext", context);
      engine.eval(m.group(2));
      temp = temp.replace(m.group(1), document.read());
    }

    File tempFile = createTempFile(temp);

    Velocity.setProperty("file.resource.loader.path", tempFile.getParent());
    Velocity.setProperty("file.resource.loader.cache", false);
    Velocity.init();
    Template template = Velocity.getTemplate(tempFile.getName(),"UTF-8");
    StringWriter writer = new StringWriter();
    template.merge(context, writer);
    writer.flush();
    writer.close();
    return new StreamSource(new StringReader(writer.toString()));
  }
  
  private StreamSource createXSLStream() throws FileNotFoundException {
    InputStream in = FopDocument.class.getResourceAsStream(XHTML_TO_XSLFO);
    if(in == null)
      in = new FileInputStream(XHTML_TO_XSLFO);
    StreamSource stream = new StreamSource(in);
    return stream;
  }
  
  private File createTempFile(String text) throws IOException {
    File temp_xml = new File(new Date().getTime()+".tmp").getAbsoluteFile();
    temp_xml.deleteOnExit();
    FileOutputStream writer = new FileOutputStream(temp_xml);
    writer.write(text.getBytes("UTF8"));
    writer.flush();
    writer.close();
    writer = null;
    return temp_xml;
  }





  //new File(new URI(FopDocument.class.getResource("fop.xconf").getPath()));
  /*private static FopFactory createFopFactory() {
    try {
      FopFactory factory = FopFactory.newInstance();
      factory.setUserConfig("fop.xml");
      return factory;
    }catch(SAXException | IOException ex) {
      JOptionPane.showMessageDialog(null, ex);
    }
    return null;
  }*/
  
  public static FopFactory createFopFactory() {
    FopFactory factory = null;
    //DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
    try {
      factory = FopFactory.newInstance(new File("fop.xml"));
      //Configuration cfg = cfgBuilder.buildFromFile(new File("fop.xml"));
      //factory.setUserConfig(cfg);
    }catch(SAXException | IOException ex){}
    return factory;
  }

  public static File getDocumentFromXslFo(String fileName, String type, String xslfo) throws Exception {
    File file = new File(new File(fileName).getAbsolutePath());
    File dir = new File(file.getParent());
    dir.mkdirs();
    FileOutputStream out = new FileOutputStream(file);
    Fop fop = fopFactory.newFop(type,out);
    Source src = new StreamSource(new StringReader(xslfo));
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer = factory.newTransformer();
    transformer.transform(src, new SAXResult(fop.getDefaultHandler()));
    out.flush();
    out.close();
    out = null;
    return file;
  }

  /*class PreviewDialog extends JDialog {
    private JSlider slider = new JSlider(0, 300);
    private PreviewPanel previewPanel;
    private AWTRenderer renderer;

    private JToolBar tool = new JToolBar();
    private JLabel page = new JLabel();
    private JButton nextPage = new JButton(">>");
    private JButton previosPage = new JButton("<<");

    private JToolbarButton print = new JToolbarButton();
    private JToolbarButton pdf = new JToolbarButton();
    private JToolbarButton fo  = new JToolbarButton();

    public PreviewDialog(PreviewPanel previewPanel, AWTRenderer renderer) {
      this.previewPanel = previewPanel;
      this.renderer = renderer;
      setLayout(new GridBagLayout());
      initComponents();
      initEvents();
    }

    private void initComponents() {
      print.setIcon(FileLoader.getIcon("print20.png"));
      pdf.setIcon(FileLoader.getIcon("pdf_ico.png"));
      fo.setIcon(FileLoader.getIcon("rtf_ico.png"));

      setNextPreviosButtonsEnable();

      slider.setMajorTickSpacing(100);
      slider.setMinorTickSpacing(10);

      slider.setPaintLabels(true);
      slider.setPaintTicks(true);
      slider.setPaintTrack(true);
      slider.setValue(100);

      tool.setFloatable(false);
      tool.add(print);
      tool.add(pdf);
      tool.add(fo);
      tool.addSeparator();
      tool.add(page);

      if(renderer.getNumberOfPages() > 1) {
        tool.addSeparator();
        tool.add(previosPage);
        tool.add(nextPage);
      }

      slider.setOrientation(JSlider.VERTICAL);
      
      add(tool,         new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
      add(slider,       new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.VERTICAL, new Insets(5, 5, 5, 5), 0, 0));
      add(previewPanel, new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
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

      if(type.equals(MimeConstants.MIME_PDF)) {
        fileChooser.setFileFilter(new FileNameExtensionFilter("файлы "+type, "pdf"));
        end = "pdf";
      }

      if(type.equals(MimeConstants.MIME_XSL_FO)) {
        fileChooser.setFileFilter(new FileNameExtensionFilter("файлы xsl-fo", "fo"));
        end = "fo";
      }

      if(end != null) {
        fileChooser.setDialogTitle("Укажите путь");
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File("export_document_"+new Date().getTime()+"."+end));
        if(fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
          final File file = fileChooser.getSelectedFile();
          if(file != null) {
            try {
              if(type.equals(MimeConstants.MIME_XSL_FO))
                getXSLFOFile(file.getAbsolutePath());
              else getDocument(file.getAbsolutePath(), type);
            }catch(Exception ex) {
              ex.printStackTrace();
            }
          }
        }
      }
    }

    private void initEvents() {
      fo.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          export(MimeConstants.MIME_XSL_FO);
        }
      });

      pdf.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          export(MimeConstants.MIME_PDF);
        }
      });

      print.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          startPrinterJob(true);
        }
      });
      
      nextPage.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          previewPanel.setPage(previewPanel.getPage()+1);
          setNextPreviosButtonsEnable();
        }
      });

      previosPage.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          previewPanel.setPage(previewPanel.getPage()-1);
          setNextPreviosButtonsEnable();
        }
      });

      slider.addChangeListener(new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
          double scale = (double)slider.getValue()/100;
          if(scale > 0) {
            previewPanel.setScaleFactor(scale);
            previewPanel.reload();
          }
        }
      });
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
  }*/
}