package division.util;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class Morpher {

  private Document doc = null;

  public Morpher(String phrase) throws IOException, SAXException, ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    String url = "http://api.morpher.ru/WebService.asmx/GetXml?s=";
    doc = builder.parse(url + phrase);
  }

  public String getCase(String padeg) {
    String result = "";
    Element root = doc.getDocumentElement();
    NodeList nodes = root.getChildNodes();
    for (int x = 0; x < nodes.getLength(); x++) {
      Node item = nodes.item(x);
      if(item instanceof Element) {
        Element el = (Element)item;
        if (el.getTagName().equals(padeg)) {
          result = ((Text)el.getFirstChild()).getData();
        }
      }
    }
    return result;
  }
}