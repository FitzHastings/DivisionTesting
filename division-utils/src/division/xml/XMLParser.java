package division.xml;

import java.io.File;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * XML парсер, что тут не понятного?
 * <p>Title: Бизнес платформа</p>
 * <p>Description: Платформа бизнес приложений</p>
 * <p>Copyright: Copyright (c) 2007</p>
 * <p>Company: Штрих-С</p>
 * @author Платонов Р.А.
 * @version 1.0
 */

public class XMLParser {
  /**
   * Парсит XML файл
   * @param file файл xml
   * @return Document
   */
  public static Document parseFile(File file) {
    Document document = null;
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(false);
    factory.setNamespaceAware(true);
    DocumentBuilder builder = null;
    try {
      builder = factory.newDocumentBuilder();
      document = builder.parse(file);
    }catch(Exception e) {
    }finally {
      //if(builder != null)
       // builder.reset();
      builder = null;
    }
    return document;
  }
  
  public static Document parse(String xml) {
    Document document = null;
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(false);
    factory.setNamespaceAware(true);
    DocumentBuilder builder = null;
    StringReader sr = new StringReader(xml);
    try {
      builder = factory.newDocumentBuilder();
      InputSource in = new InputSource(sr);
      document = builder.parse(in);
    }catch(Exception e) {
    }finally {
      try {
        sr.reset();
        sr.close();
      }catch(Exception ex){}
      if(builder != null)
        builder.reset();
      sr = null;
      builder = null;
    }
    return document;
  }
}
