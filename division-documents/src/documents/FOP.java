package documents;

import division.swing.guimessanger.Messanger;
import division.util.FileLoader;
import division.util.GzipUtil;
import division.util.ScriptUtil;
import division.util.Utility;
import java.awt.Dialog;
import java.awt.print.PrinterJob;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.fop.apps.*;
import org.apache.fop.render.awt.AWTRenderer;
import org.apache.fop.render.print.PrintRenderer;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.tools.generic.*;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.xml.sax.SAXException;

public class FOP {
  private final static String     XHTML_TO_XSLFO = "conf"+File.separator+"fo.xsl";
  private final static FopFactory FOP_FACTORY    = createFopFactory();
  
  private final List<String> jsModules = new ArrayList<>();
  private final Map<String,Object> variables = new HashMap<>();
  
  public static FopFactory createFopFactory() {
    FopFactory factory = null;
    //DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
    try {
      factory = FopFactory.newInstance(new File("fop.xml"));
      //Configuration cfg = cfgBuilder.buildFromFile(new File("fop.xml"));
      //factory.setUserConfig(cfg);
    }catch(SAXException | IOException ex) {
      ex.printStackTrace();
    }
    return factory;
  }
  
  public static FopFactory getFopFactory() {
    return FOP_FACTORY;
  }
  
  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  ///////////////////XSL-FO/////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  public static void print_from_XSLFO(String xslfo) throws Exception {
    print_XSLFO(new StreamSource(new StringReader(xslfo)), null);
  }
  
  public static void print_from_XSLFO_file(String xslfoFile) throws Exception {
    print_XSLFO(new StreamSource(new FileReader(xslfoFile)), null);
  }
  
  public static void print_from_XSLFO(String xslfo, PrintRenderer printRenderer) throws Exception {
    print_XSLFO(new StreamSource(new StringReader(xslfo)), printRenderer);
  }
  
  public static void print_from_XSLFO_file(String xslfoFile, PrintRenderer printRenderer) throws Exception {
    print_XSLFO(new StreamSource(new FileReader(xslfoFile)), printRenderer);
  }
  
  public static void preview_from_XSLFO(String xslfo) throws Exception {
    previw_XSLFO(new StreamSource(new StringReader(xslfo)));
  }
  
  public static void preview_from_XSLFO_file(String xslfoFile) throws Exception {
    previw_XSLFO(new StreamSource(new FileReader(xslfoFile)));
  }
  
  private static void previw_XSLFO(Source src) 
          throws TransformerConfigurationException, FOPException, TransformerException, SAXException, IOException, ConfigurationException {
    transform(null, src, MimeConstants.MIME_FOP_AWT_PREVIEW, null, null);
  }
  
  private static void print_XSLFO(Source src, PrintRenderer printRenderer) 
          throws TransformerConfigurationException, FOPException, TransformerException {
    FOUserAgent userAgent = FOP_FACTORY.newFOUserAgent();
    if(printRenderer != null) {
      userAgent.setRendererOverride(printRenderer);
      userAgent.setRendererOverride(printRenderer);
    }
    transform(null, src, MimeConstants.MIME_FOP_PRINT, userAgent, null);
  }
  
  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  ///////////////////XMLTemplate////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  
  public static void print_from_XMLTemplate(String xmlTemplate) throws Exception {
    print_from_XMLTemplate(xmlTemplate, null);
  }
  
  public static void print_from_XMLTemplate(String xmlTemplate, Map<String,Object> valiables) throws Exception {
    print_from_XML(get_XML_From_XMLTemplate(xmlTemplate, valiables));
  }
  
  public static void preview_from_XMLTemplate(String xmlTemplate) throws Exception {
    preview_from_XMLTemplate(xmlTemplate, null);
  }
  
  public static void preview_from_XMLTemplate(String xmlTemplate, Map<String,Object> valiables) throws Exception {
    preview_from_XML(get_XML_From_XMLTemplate(xmlTemplate, valiables));
  }
  
  
  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  ///////////////////XML////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  
  public static void print_from_XML(String XML) throws Exception {
    print_from_XML(XML, true, null);
  }
  
  public static void print_from_XML(String XML, boolean printerSelectable) throws Exception {
    print_from_XML(XML, printerSelectable, null);
  }
  
  public static void print_from_XML(String XML, boolean printerSelectable, PrinterJob printerJob) throws Exception {
    Source xsltSource = createXSLStream();
    Source xmlSource = new StreamSource(new StringReader(XML));
    
    if(printerSelectable || !printerSelectable && printerJob != null) {
      PrinterJob pj = printerSelectable ? PrinterJob.getPrinterJob() : printerJob;
      if(printerSelectable && pj.printDialog() || printerJob != null) {
        try {
          FOUserAgent userAgent = FOP_FACTORY.newFOUserAgent();
          AWTRenderer renderer = new AWTRenderer(userAgent);
          userAgent.setRendererOverride(renderer);

          transform(
                  xsltSource, 
                  xmlSource, 
                  MimeConstants.MIME_FOP_PRINT, userAgent, null);

          pj.setPageable(renderer);
          pj.print();
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    }else
      transform(
              xsltSource, 
              xmlSource, 
              MimeConstants.MIME_FOP_PRINT, null, null);
  }
  
  public static void preview_from_XML(String XML) throws Exception {
    PreviewDialog dialog = new PreviewDialog(XML);
    dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
    SwingUtilities.invokeLater(() -> dialog.setVisible(true));
  }
  
  public static void preview_from_groovy(String groovyText, Map<String,Object> valiables) throws Exception {
    PreviewDialog dialog = new PreviewDialog(get_XML_From_GS_XMLTemplate(groovyText, valiables));
    dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
    SwingUtilities.invokeLater(() -> dialog.setVisible(true));
  }
  
  
  
  
  
  
  
  
  
  
  
  public void export_from_XSLFO(String fileName, String xslfo, String type) throws Exception {
    FileLoader.createFileIfNotExists(fileName);
    transform(null, new StreamSource(new StringReader(xslfo)), type, null, new FileOutputStream(fileName));
  }
  
  public void export_from_XSLFO_file(String fileName, String xslfoFile, String type) throws Exception {
    FileLoader.createFileIfNotExists(fileName);
    transform(null, new StreamSource(new FileReader(xslfoFile)), type, null, new FileOutputStream(fileName));
  }
  
  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  ///////////////////XMLTemplate////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  
  public void export_from_XMLTemplate(String fileName, String xml, String type) throws Exception {
    export_from_XMLTemplate_file(fileName, createTempFile(xml).getAbsolutePath(), type);
  }
  
  public void export_from_XMLTemplate_file(String fileName, String xmlFileName, String type) throws Exception {
    FileLoader.createFileIfNotExists(fileName);
    transform(createXSLStream(), createJSVelocityStreamFromXmTemplatelFile(xmlFileName), type, null, new FileOutputStream(fileName));
  }
  
  public static String get_XSLFO_string_from_XML(String xml) 
          throws FileNotFoundException, TransformerConfigurationException, TransformerException, IOException {
    Source src;
    src = new StreamSource(new StringReader(xml));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer = factory.newTransformer(createXSLStream());
    Result res = new StreamResult(out);
    transformer.transform(src, res);
    String xslfo = out.toString();
    out.reset();
    out.flush();
    out.close();
    out = null;
    return xslfo;
  }
  
  public String get_XSLFO_string_from_XML(String xml, boolean pure) throws Exception {
    Source src;
    if(pure)
      src = new StreamSource(new StringReader(xml));
    else src = new StreamSource(new StringReader(get_XML_From_XMLTemplate(xml)));
    
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer = factory.newTransformer(createXSLStream());
    Result res = new StreamResult(out);
    transformer.transform(src, res);
    String xslfo = out.toString();
    out.reset();
    out.flush();
    out.close();
    out = null;
    return xslfo;
  }
  
  public String get_XSLFO_string_from_XML_file(String xmlFileName, boolean pure) throws Exception {
    return get_XSLFO_string_from_XML(getStringFromFile(xmlFileName), pure);
  }
  
  public static void export_from_XML(String fileName, String xml, String type) throws Exception {
    transform(createXSLStream(), new StreamSource(new StringReader(xml)), type, null, new FileOutputStream(FileLoader.createFileIfNotExists(fileName)));
  }
  
  public static void export_from_XML(OutputStream out, String xml, String type) throws Exception {
    transform(createXSLStream(), new StreamSource(new StringReader(xml)), type, null, out);
  }
  
  public static InputStream export_from_XML(String xml, String type) throws Exception {
    return transformAndReturnInputStream(createXSLStream(), new StreamSource(new StringReader(xml)), type, null);
  }
  
  public static byte[] export_from_XML_to_bytea(String xml, String type) throws Exception {
    byte[] arr = new byte[0];
    InputStream in = transformAndReturnInputStream(createXSLStream(), new StreamSource(new StringReader(xml)), type, null);
    if(in != null) {
      arr = new byte[in.available()];
      in.read(arr);
      in.close();
      in = null;
    }
    return arr;
  }
  
  public static void transform(Source xslSource, Source src, String type, FOUserAgent userAgent, OutputStream out) 
          throws TransformerConfigurationException, FOPException, TransformerException {
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer;
    if(xslSource != null)
      transformer = factory.newTransformer(xslSource);
    else transformer = factory.newTransformer();
    Fop fop;
    if(out != null)
      fop = FOP_FACTORY.newFop(type, userAgent==null?FOP_FACTORY.newFOUserAgent():userAgent, out);
    else fop = FOP_FACTORY.newFop(type, userAgent==null?FOP_FACTORY.newFOUserAgent():userAgent);
    transformer.transform(src, new SAXResult(fop.getDefaultHandler()));
    if(out != null) {
      try {
        out.flush();
        out.close();
        out = null;
      }catch(Exception ex) {
        Logger.getLogger(FOP.class.getName()).warning(ex.getMessage());
      }
    }
  }
  
  public static InputStream transformAndReturnInputStream(Source xslSource, Source src, String type, FOUserAgent userAgent) 
          throws TransformerConfigurationException, FOPException, TransformerException {
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer;
    if(xslSource != null)
      transformer = factory.newTransformer(xslSource);
    else transformer = factory.newTransformer();
    ByteArrayOutputStream out = null;
    ByteArrayInputStream in = null;
    try {
      out = new ByteArrayOutputStream();
      Fop fop = FOP_FACTORY.newFop(type, userAgent==null?FOP_FACTORY.newFOUserAgent():userAgent, out);
      transformer.transform(src, new SAXResult(fop.getDefaultHandler()));
      in = new ByteArrayInputStream(out.toByteArray());
    }catch(Exception ex) {
      Logger.getLogger(FOP.class.getName()).warning(ex.getMessage());
    }finally {
      try {
        out.reset();
        out.flush();
        out.close();
        out = null;
      }catch(Exception ex) {
        Logger.getLogger(FOP.class.getName()).warning(ex.getMessage());
      }
    }
    return in;
  }
  
  public static StreamSource createXSLStream() throws FileNotFoundException {
    InputStream in = new FileInputStream(XHTML_TO_XSLFO);
    StreamSource stream = new StreamSource(in);
    return stream;
  }
  
  private static VelocityContext createVelocityContext(Map<String,Object> variables) {
    VelocityContext context = new VelocityContext();
    context.put("convert",    new ConversionTool());
    context.put("dateTool",   new DateTool());
    context.put("mathTool",   new MathTool());
    context.put("numberTool", new NumberTool());
    context.put("listTool",   new ListTool());
    context.put("utility",    Utility.class);
    context.put("GzipUtil",   new GzipUtil());
    
    if(variables != null)
      for(String variable:variables.keySet())
        context.put(variable, variables.get(variable));
    return context;
  }
  
  public static String get_XML_From_XMLTemplate(String XML) throws Exception {
    return get_XML_From_XMLTemplate(XML, null, null);
  }
  
  public static String get_XML_From_XMLTemplate(String XML, List<String> jsModules) throws Exception {
    return get_XML_From_XMLTemplate(XML, jsModules, null);
  }
  
  public static String get_XML_From_XMLTemplate(String XML, Map<String,Object> variables) throws Exception {
    return get_XML_From_XMLTemplate(XML, null, variables);
  }
  
  public static String get_XML_From_XMLTemplate(String XML, List<String> jsModules, Map<String,Object> variables) throws Exception {
    return scriptValidator(XML, variables);
    /*ScriptEngine engine = createScriptEngine(jsModules,variables);
    VelocityContext context = createVelocityContext(variables);
    
    Pattern p = Pattern.compile("(<js>([\\s\\S]*?)</js>)");
    Matcher m = p.matcher(XML);
    while(m.find()) {
      JSDocument document = new JSDocument();
      engine.put("document", document);
      engine.put("vcontext", context);
      engine.eval(m.group(2));
      XML = XML.replace(m.group(1), document.read());
    }

    File tempFile = createTempFile(XML);
    Velocity.setProperty("file.resource.loader.path", tempFile.getParent());
    Velocity.setProperty("file.resource.loader.cache", false);
    Velocity.setProperty(Velocity.INPUT_ENCODING, "UTF8");
    Velocity.setProperty(Velocity.OUTPUT_ENCODING, "UTF8");
    Velocity.init();
    Template template = Velocity.getTemplate(tempFile.getName(),"UTF8");
    StringWriter writer = new StringWriter();
    template.merge(context, writer);
    writer.flush();
    writer.close();
    tempFile.delete();
    return writer.toString();*/
  }
  
  public static String get_XML_From_GS_XMLTemplate(String xml, Map<String,Object> variables) throws Exception {
    xml = "{text}"+xml.replaceAll("\n", "{next-line}")+"{text}";
    
    Pattern p = Pattern.compile("(<gs:([\\s\\S]*?)>)");
    Matcher m = p.matcher(xml);
    while(m.find()) {
      xml = xml.replaceAll(Pattern.quote(m.group(1)), "'+("+m.group(2).replaceAll("'", "\"")+")+'");
    }
    
    p = Pattern.compile("(<gs>([\\s\\S]*?)</gs>)");
    m = p.matcher(xml);
    while(m.find()) {
      xml = xml.replaceAll(Pattern.quote(m.group(1)), "{text}"+m.group(2).replaceAll("\\{next-line\\}", "\n")+"{text}");
    }
    
    p = Pattern.compile("(\\{text\\}([\\s\\S]*?)\\{text\\})");
    m = p.matcher(xml);
    while(m.find()) {
      xml = xml.replaceAll(Pattern.quote(m.group(1)), "out('"+m.group(2)+"');");
    }
    
    xml = "def out(var) {\nret += var\n};\n"+xml+"ret;";
    variables.put("ret", "");
    
    String r = (String) ScriptUtil.runScript("dfsd", xml, SyntaxConstants.SYNTAX_STYLE_GROOVY, variables);
    
    return r.replaceAll("\\{next-line\\}", "\n");
  }
  
  public static String getStringFromFile(String fileName) throws FileNotFoundException, IOException {
    ByteArrayOutputStream baos;
    try(FileInputStream in = new FileInputStream(fileName)) {
      baos = new ByteArrayOutputStream();
      int b;
      while((b = in.read()) != -1)
        baos.write(b);
      in.close();
    }
    new File(fileName).delete();
    String str = baos.toString("UTF8");
    baos.reset();
    baos.flush();
    baos.close();
    baos = null;
    return str;
  }
  
  public static StreamSource createJSVelocityStreamFromXmlTemplateString(String xml, List<String> jsModules, Map<String,Object> variables) throws Exception {
    return new StreamSource(new StringReader(get_XML_From_XMLTemplate(xml,jsModules,variables)));
  }
  
  public StreamSource createJSVelocityStreamFromXmlTemplateString(String xml) throws Exception {
    return new StreamSource(new StringReader(get_XML_From_XMLTemplate(xml)));
  }
  
  public StreamSource createJSVelocityStreamFromXmTemplatelFile(String xmlFileName) throws Exception {
    return createJSVelocityStreamFromXmlTemplateString(getStringFromFile(xmlFileName), jsModules, variables);
  }
  
  public static File createTempFile(String text) throws FileNotFoundException, IOException {
    File temp_xml = new File(System.currentTimeMillis()+".tmp").getAbsoluteFile();
    temp_xml.deleteOnExit();
    FileOutputStream writer = null;
    try {
      writer = new FileOutputStream(temp_xml);
      writer.write(text.getBytes("UTF8"));
      writer.flush();
      writer.close();
    }catch(Exception ex){}
    finally {
      if(writer != null) {
        writer.flush();
        writer.close();
      }
      writer = null;
    }
    return temp_xml;
  }
  
  /*public static PrintService getPrintService() {
    PrinterJob printerJob = PrinterJob.getPrinterJob();
    if(printerJob.printDialog())
      return printerJob.getPrintService();
    return null;
  }*/
  
  
  
  public static String scriptValidator(String source, Map<String,Object> variables) throws Exception {
    variables.put("scriptContext", new TreeMap<>());
    source = runGroovyScript(source, variables);
    source = runJavaScript(source, variables);
    source = runVelocityScript(source, variables);
    return source;
  }
  
  /*
  public static String runGroovyScript(String source, Map<String,Object> variables) throws Exception {
    // Маркируем исходные данные, чтобы отделить их от новых
    source = "{text}"+source.replaceAll("\n", "{next-line}").replaceAll("\\$", "{velocity}")+"{text}";
    
    Pattern p = Pattern.compile("(<gs>([\\s\\S]*?)</gs>)");
    Matcher m = p.matcher(source);
    while(m.find()) {
      System.out.println("Нашёл Groovy:\n"+m.group(1)+"\n#############################################");
      String r = "{text}"+m.group(2).replaceAll("\\{next-line\\}", "\n").replaceAll("\\\\", "\\\\\\\\")+"{text}";
      
      int index = r.indexOf("out(");
      if(index >= 0) {
        char[] s = r.substring(index+4).toCharArray();
        String g = "out(";
        int count = 1;
        while(count > 0) {
          g += s[0];
          if(s[0] == '(')
            count++;
          if(s[0] == ')')
            count--;
          s = ArrayUtils.remove(s, 0);
        }
        
        System.out.println("     Нашёл out:\n     "+g+"\n     #############################################");
        r = r.replaceFirst(Pattern.quote(g), g.replaceAll("\\n", "").replaceAll("\\t", ""));
      }
      
      source = source.replaceAll(Pattern.quote(m.group(1)), r);
    }
    
    p = Pattern.compile("(\\{text\\}([\\s\\S]*?)\\{text\\})");
    m = p.matcher(source);
    while(m.find())
      source = source.replaceAll(Pattern.quote(m.group(1)), "out('"+m.group(2).replaceAll("'", "{one-quote}")+"');");
    
    p = Pattern.compile("(<gs:([\\s\\S]*?)>)");
    m = p.matcher(source);
    while(m.find())
      source = source.replaceAll(Pattern.quote(m.group(1)), "'+("+m.group(2).replaceAll("'", "\"")+")+'");
    
    source = "def out(variable) {\nret += variable\n};\n"+source+"\nret;";
    variables.put("ret", "");
    
    System.out.println();
    System.out.println();
    System.out.println();
    System.out.println(source);
    
    String r = (String) ScriptUtil.runScript("dfsd", source.replaceAll("\\{velocity\\}", "\\$").replaceAll("\n", ""), SyntaxConstants.SYNTAX_STYLE_GROOVY, variables);
    
    source = r.replaceAll("\\{next-line\\}", "\n").replaceAll("\\{velocity\\}", "\\$").replaceAll("\\{one-quote\\}", "'");
    
    return source;
  }
  */
  
  public static String runGroovyScript(String source, Map<String,Object> variables) throws Exception {
    // Маркируем исходные данные, чтобы отделить их от новых
    source = "{text}"+source.replaceAll("\n", "{next-line}").replaceAll("\\$", "{velocity}")+"{text}";
    
    Pattern p = Pattern.compile("(<gs>([\\s\\S]*?)</gs>)");
    Matcher m = p.matcher(source);
    while(m.find()) {
      //source = source.replaceAll(Pattern.quote(m.group(1)), "{text}"+m.group(2).replaceAll("\\{next-line\\}", "\n").replaceAll("\\\\", "\\\\\\\\")+"{text}");
      System.out.println("Нашёл Groovy:\n"+m.group(1)+"\n#############################################");
      String r = "{text}"+m.group(2).replaceAll("\\{next-line\\}", "\n").replaceAll("\\\\", "\\\\\\\\")+"{text}";
      
      String findString = r;
      int index;
      while((index = findString.indexOf("out(")) >= 0) {
        char[] s = findString.substring(index+4).toCharArray();
        String g = "out(";
        int count = 1;
        while(count > 0) {
          g += s[0];
          if(s[0] == '(')
            count++;
          if(s[0] == ')')
            count--;
          s = ArrayUtils.remove(s, 0);
        }

        //System.out.println("     Нашёл out:\n     "+g+"\n     #############################################");
        String gg = g.replaceAll("\\n", "").replaceAll("\\t", "");
        r = r.replaceFirst(Pattern.quote(g), gg);
        findString = findString.substring(index+gg.length());
      }
      
      source = source.replaceAll(Pattern.quote(m.group(1)), r);
    }
    
    p = Pattern.compile("(\\{text\\}([\\s\\S]*?)\\{text\\})");
    m = p.matcher(source);
    while(m.find()) {
      source = source.replaceAll(Pattern.quote(m.group(1)), "out('"+m.group(2).replaceAll("'", "{one-quote}")+"');");
    }
    
    p = Pattern.compile("(<\\?([\\s\\S]*?)\\?>)");
    m = p.matcher(source);
    while(m.find()) {
      source = source.replaceAll(Pattern.quote(m.group(1)), "'+("+m.group(2).replaceAll("'", "\"")+")+'");
    }
    
    p = Pattern.compile("(<gs:([\\s\\S]*?)>)");
    m = p.matcher(source);
    while(m.find()) {
      source = source.replaceAll(Pattern.quote(m.group(1)), "'+("+m.group(2).replaceAll("'", "\"")+")+'");
    }
    
    source = "def out(variable) {\nret += variable\n};\n"+source+"\nret;";
    variables.put("ret", "");
    
    String r = (String) ScriptUtil.runScript("dfsd", source.replaceAll("\\{velocity\\}", "\\$"), SyntaxConstants.SYNTAX_STYLE_GROOVY, variables);
    
    source = r.replaceAll("\\{next-line\\}", "\n").replaceAll("\\{velocity\\}", "\\$").replaceAll("\\{one-quote\\}", "'");
    
    return source;
  }
  
  public static String runJavaScript(String source, Map<String,Object> variables) throws Exception {
    // Маркируем исходные данные, чтобы отделить их от новых
    source = "{text}"+source.replaceAll("\n", "{next-line}").replaceAll("\\$", "{velocity}")+"{text}";
    
    Pattern p = Pattern.compile("(<js>([\\s\\S]*?)</js>)");
    Matcher m = p.matcher(source);
    while(m.find()) {
      source = source.replaceAll(Pattern.quote(m.group(1)), "{text}"+m.group(2).replaceAll("\\{next-line\\}", "\n").replaceAll("\\\\", "\\\\\\\\")+"{text}");
    }
    
    p = Pattern.compile("(\\{text\\}([\\s\\S]*?)\\{text\\})");
    m = p.matcher(source);
    while(m.find()) {
      source = source.replaceAll(Pattern.quote(m.group(1)), "out('"+m.group(2).replaceAll("'", "{one-quote}")+"');");
    }
    
    p = Pattern.compile("(<js:([\\s\\S]*?)>)");
    m = p.matcher(source);
    while(m.find()) {
      source = source.replaceAll(Pattern.quote(m.group(1)), "'+("+m.group(2).replaceAll("'", "\"")+")+'");
    }
    
    source = "function out(variable) {\nret += variable\n};\n"+source+"\nret;";
    variables.put("ret", "");
    
    String r = (String) ScriptUtil.runScript("dfsd", source.replaceAll("\\{velocity\\}", "\\$"), SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT, variables);
    
    source = r.replaceAll("\\{next-line\\}", "\n").replaceAll("\\{velocity\\}", "\\$").replaceAll("\\{one-quote\\}", "'");
    
    return source;
  }
  
  public static String runVelocityScript(String source, Map<String,Object> variables) throws Exception {
    Map<String,Object> gjscontext = (Map<String,Object>) variables.remove("scriptContext");
    variables.putAll(gjscontext);
    VelocityContext context = createVelocityContext(variables);
    File tempFile = createTempFile(source);
    Velocity.setProperty("file.resource.loader.path", tempFile.getParent());
    Velocity.setProperty("file.resource.loader.cache", false);
    Velocity.setProperty(Velocity.INPUT_ENCODING, "UTF8");
   // Velocity.setProperty(Velocity.OUTPUT_ENCODING, "UTF8");
    Velocity.init();
    Template template = Velocity.getTemplate(tempFile.getName(),"UTF8");
    StringWriter writer = new StringWriter();
    template.merge(context, writer);
    writer.flush();
    writer.close();
    tempFile.delete();
    return writer.toString();
  }
}