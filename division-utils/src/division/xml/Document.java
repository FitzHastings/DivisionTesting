package division.xml;

import division.util.FileLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class Document implements Serializable {
  private String name;
  private Node rootNode;

  public Document() {
    rootNode = new Node("root");
  }
  
  public Document(String name) {
    this.name = name;
    rootNode = new Node("root");
  }

  public Document(String name, Node rootNode) {
    this.name = name;
    this.rootNode = rootNode;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Node getRootNode() {
    return rootNode;
  }

  public void setRootNode(Node rootNode) {
    this.rootNode = rootNode;
  }

  public boolean isEmpty() {
    return getRootNode() == null;
  }
  
  public void load() throws IOException {
    load(this, name);
  }

  public void load(Document document,String fileName) throws IOException {
    FileLoader.createFileIfNotExists(fileName, false);
    File file = new File(fileName);
    if(file.exists()) {
      document.setName(name);
      org.w3c.dom.Document doc = XMLParser.parseFile(file);
      doc.getDocumentElement().normalize();
      Element rootElement = doc.getDocumentElement();
      Node root = new Node(rootElement.getNodeName(),"");
      document.setRootNode(root);
      NamedNodeMap attr = rootElement.getAttributes();
      if(attr != null)
        for(int i=0;i<attr.getLength();i++)
          root.setAttribute(attr.item(i).getNodeName(), attr.item(i).getNodeValue());
      createNodes(root,rootElement.getChildNodes());
    }
  }
  
  public static Document loadFromString(String xml) {
    org.w3c.dom.Document doc = XMLParser.parse(xml);
    if(doc != null) {
      doc.getDocumentElement().normalize();
      Element rootElement = doc.getDocumentElement();
      Node root = new Node(rootElement.getNodeName(),"");
      NamedNodeMap attr = rootElement.getAttributes();
      if(attr != null)
        for(int i=0;i<attr.getLength();i++)
          root.setAttribute(attr.item(i).getNodeName(), attr.item(i).getNodeValue());
      createNodes(root,rootElement.getChildNodes());
      return new Document("", root);
    }
    return null;
  }
  
  public static Document load(String fileName) {
    return load(fileName, false);
  }

  public static Document load(String fileName, boolean createIfNotExist) {
    try {
      Node root = new Node("root");
      FileLoader.createFileIfNotExists(fileName, createIfNotExist);
      File file = new File(fileName);
      if(file.exists()) {
        org.w3c.dom.Document doc = XMLParser.parseFile(file);
        if(doc != null) {
          doc.getDocumentElement().normalize();
          Element rootElement = doc.getDocumentElement();
          root = new Node(rootElement.getNodeName(),"");
          NamedNodeMap attr = rootElement.getAttributes();
          if(attr != null)
            for(int i=0;i<attr.getLength();i++)
              root.setAttribute(attr.item(i).getNodeName(), attr.item(i).getNodeValue());
          createNodes(root,rootElement.getChildNodes());
        }
        return new Document(fileName, root);
      }
    }catch(Exception ex) {
      Logger.getRootLogger().error("Ошибка записи", ex);
    }
    return null;
  }

  private static void createNodes(Node parentNode, NodeList nodes) {
    for(int j=0;j<nodes.getLength();j++) {
      org.w3c.dom.Node n = nodes.item(j);
      if(n.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
        parentNode.setValue(parentNode.getValue()+n.getNodeValue().trim());
      }else {
        Node node = new Node(n.getNodeName());
        NamedNodeMap attr = n.getAttributes();
        if(attr != null)
          for(int i=0;i<attr.getLength();i++)
            node.setAttribute(attr.item(i).getNodeName(), attr.item(i).getNodeValue());
        parentNode.addNode(node);
        if(n.getChildNodes().getLength() > 0)
          createNodes(node,n.getChildNodes());
      }
    }
  }

  public boolean save() {
    return save(name);
  }

  public boolean save(String fileName) {
    FileOutputStream out = null;
    Transformer transformer = null;
    DocumentBuilder builder = null;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      builder = factory.newDocumentBuilder();
      org.w3c.dom.Document document = builder.newDocument();

      Element rootElement = document.createElement(rootNode.getName());
      if(rootNode.getValue() != null && !rootNode.getValue().equals(""))
        rootElement.appendChild(document.createTextNode(rootNode.getValue()));
      for(String attrName:rootNode.getAttributes().keySet())
        rootElement.setAttribute(attrName, rootNode.getAttributes().get(attrName));
      document.appendChild(rootElement);
      createElements(document, rootElement, rootNode);
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(document);
      FileLoader.createFileIfNotExists(fileName);
      out = new FileOutputStream(fileName);
      StreamResult result =  new StreamResult(out);
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
      transformer.transform(source, result);
      return true;
    }catch(Exception ex) {
      ex.printStackTrace();
      return false;
    }finally {
      try {
        if(builder != null)
          builder.reset();
        if(out != null) {
          out.flush();
          out.close();
          System.out.println("out flash...");
        }
        if(transformer != null)
          transformer.reset();
      }catch(Exception ex){}
      builder = null;
      transformer = null;
      out = null;
    }
  }
  
  public String getXML() {
    String xml = null;
    DocumentBuilder builder = null;
    Transformer transformer = null;
    StringWriter writer = null;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      builder = factory.newDocumentBuilder();
      org.w3c.dom.Document document = builder.newDocument();

      Element rootElement = document.createElement(rootNode.getName());
      if(rootNode.getValue() != null && !rootNode.getValue().equals(""))
        rootElement.appendChild(document.createTextNode(rootNode.getValue()));
      for(String attrName:rootNode.getAttributes().keySet())
        rootElement.setAttribute(attrName, rootNode.getAttributes().get(attrName));
      document.appendChild(rootElement);
      createElements(document, rootElement, rootNode);
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(document);
      writer = new StringWriter();
      StreamResult result =  new StreamResult(writer);
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
      transformer.transform(source, result);
      xml = writer.toString();
    }catch(Exception ex) {
      ex.printStackTrace();
    }finally {
      try {
        if(builder != null)
          builder.reset();
        if(writer != null) {
          writer.flush();
          writer.close();
        }
        if(transformer != null)
          transformer.reset();
      }catch(Exception ex){}
      builder = null;
      transformer = null;
      writer = null;
    }
    return xml;
  }

  private static void createElements(org.w3c.dom.Document document, Element element, Node node) {
    Element em;
    for(Node n:node.getNodes()) {
      em = document.createElement(n.getName()!=null?n.getName().replaceAll(" ", "_"):"null");
      if(n.getValue() != null && !n.getValue().equals(""))
        em.appendChild(document.createTextNode(n.getValue()));
      for(String attrName:n.getAttributes().keySet())
        em.setAttribute(attrName, n.getAttributes().get(attrName));
      element.appendChild(em);
      if(n.getNodesCount() > 0)
        createElements(document,em,n);
    }
  }

  @Override
  public String toString() {
    return getRootNode()==null?"null":getRootNode().toString();
  }

  public void remove() {
    FileLoader.deleteFile(getName());
  }

  public List<Node> getNodes(String nodeName) {
    List<Node> nodes = new ArrayList<>();
    Node parentNode = getRootNode();
    if(nodeName.indexOf(".") == -1)
      nodeName = nodeName;
    else {
      parentNode = getNode(nodeName.substring(0, nodeName.lastIndexOf(".")));
      nodeName   = nodeName.substring(nodeName.lastIndexOf(".")+1);
    }
    for(Node n:parentNode.getNodes())
      if(n.getName().equals(nodeName))
        nodes.add(n);
    return nodes;
  }

  public Node getNode(String path) {
    Node node = getRootNode();
    if(!node.getName().equals(path)) {
      if(path.indexOf(".") > -1) {
        for(String nodeName:path.split("\\.")) {
          if(nodeName.indexOf("(") != -1) {
            int index = Integer.valueOf(nodeName.substring(nodeName.indexOf("(")+1, nodeName.length()-1));
            for(Node n:node.getNodes()) {
              if(n.getName().equals(nodeName.substring(0, nodeName.indexOf("("))))
                index--;
              if(index == 0) {
                node = n;
                break;
              }
            }
          }else node = node.getNode(nodeName);
        }
      }else node = node.getNode(path);
    }
    return node;
  }
}