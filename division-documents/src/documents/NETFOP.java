package documents;

import division.util.GzipUtil;
import division.util.ScriptUtil;
import division.util.Utility;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.tools.generic.ConversionTool;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.ListTool;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.xml.sax.SAXException;

public class NETFOP {
  private final String     XHTML_TO_XSLFO = "conf"+File.separator+"fo.xsl";
  private final FopFactory FOP_FACTORY    = createFopFactory();
  
  /*public FopFactory createFopFactory() {
    FopFactory factory = FopFactory.newInstance();
    DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
    try {
      Configuration cfg = cfgBuilder.buildFromFile(new File("fop.xml"));
      factory.setUserConfig(cfg);
    }catch(SAXException | IOException | ConfigurationException ex){}
    return factory;
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
  
  public String get_XML_From_XMLTemplate(String XML, Map<String,Object> variables) throws Exception {
    return get_XML_From_XMLTemplate(XML, null, variables);
  }
  
  public String get_XML_From_XMLTemplate(String XML, List<String> jsModules, Map<String,Object> variables) throws Exception {
    return scriptValidator(XML, variables);
  }
  
  public String scriptValidator(String source, Map<String,Object> variables) throws Exception {
    variables.put("scriptContext", new TreeMap<>());
    //source = runGroovyScript(source, variables);
    //source = runJavaScript(source, variables);
    source = runVelocityScript(source, variables);
    return source;
  }
  
  public byte[] export_from_XML_to_bytea(String xml, String type) throws Exception {
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
  
  public InputStream transformAndReturnInputStream(Source xslSource, Source src, String type, FOUserAgent userAgent) 
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
  
  
  
  
  
  
  
  
  public File createTempFile(String text) throws FileNotFoundException, IOException {
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
  
  private VelocityContext createVelocityContext(Map<String,Object> variables) {
    VelocityContext context = new VelocityContext();
    context.put("convert",    new ConversionTool());
    context.put("dateTool",   new DateTool());
    context.put("mathTool",   new MathTool());
    context.put("numberTool", new NumberTool());
    context.put("listTool",   new ListTool());
    context.put("utility",    new Utility());
    context.put("GzipUtil",   new GzipUtil());
    
    if(variables != null)
      for(String variable:variables.keySet())
        context.put(variable, variables.get(variable));
    return context;
  }
  
  public StreamSource createXSLStream() throws FileNotFoundException {
    InputStream in = new FileInputStream(XHTML_TO_XSLFO);
    StreamSource stream = new StreamSource(in);
    return stream;
  }
  
  public String runGroovyScript(String source, Map<String,Object> variables) throws Exception {
    // Маркируем исходные данные, чтобы отделить их от новых
    source = "{text}"+source.replaceAll("\n", "{next-line}").replaceAll("\\$", "{velocity}")+"{text}";
    
    Pattern p = Pattern.compile("(<gs>([\\s\\S]*?)</gs>)");
    Matcher m = p.matcher(source);
    while(m.find()) {
      source = source.replaceAll(Pattern.quote(m.group(1)), "{text}"+m.group(2).replaceAll("\\{next-line\\}", "\n").replaceAll("\\\\", "\\\\\\\\")+"{text}");
    }
    
    p = Pattern.compile("(\\{text\\}([\\s\\S]*?)\\{text\\})");
    m = p.matcher(source);
    while(m.find()) {
      source = source.replaceAll(Pattern.quote(m.group(1)), "out('"+m.group(2).replaceAll("'", "{one-quote}")+"');");
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
  
  public String runJavaScript(String source, Map<String,Object> variables) throws Exception {
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
  
  public String runVelocityScript(String source, Map<String,Object> variables) throws Exception {
    Map<String,Object> gjscontext = (Map<String,Object>) variables.remove("scriptContext");
    variables.putAll(gjscontext);
    VelocityContext context = createVelocityContext(variables);
    File tempFile = createTempFile(source);
    Velocity.setProperty("file.resource.loader.path", tempFile.getParent());
    Velocity.setProperty("file.resource.loader.cache", false);
    Velocity.setProperty(Velocity.INPUT_ENCODING, "UTF8");
   /// Velocity.setProperty(Velocity.OUTPUT_ENCODING, "UTF8");
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
//Имущество в Активы.