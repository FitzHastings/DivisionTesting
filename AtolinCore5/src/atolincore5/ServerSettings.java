package atolincore5;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement (name = "ServerSettings")
public class ServerSettings {
     /** Saves serverSettings to a file, in an xml format */
    
    private transient List<Connection> connections = new ArrayList<Connection>();
    
    public ServerSettings() { }
    
    @XmlElementWrapper(name="connections")
    @XmlElement(name="connection")
    public List<Connection> getConnections()
    {
        return connections;
    }
    
    public void setConnections(List<Connection> connections)
    {
        this.connections = connections;
    }
    
    public void saveSettings(File file) {
      if(file == null)
          return;
      
      if(connections.isEmpty())
          connections.add(new Connection());
      
      if (!file.getPath().endsWith(".xml")) 
        file = new File(file.getPath() + ".xml");
      try {
        JAXBContext context = JAXBContext.newInstance(ServerSettings.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(this, file);
      }
      catch(Exception e) {
        e.printStackTrace();
      }
    }
    
    /** loads settings from a file that has xml formatting and can be created by saveSettings*/
    public void loadSettings(File file) {
      if(file == null)
        return;
      try {
        JAXBContext context = JAXBContext.newInstance(ServerSettings.class);
        Unmarshaller um = context.createUnmarshaller();

        ServerSettings buffer = (ServerSettings) um.unmarshal(file);
        connections = buffer.connections;
      }catch(Exception e) {
        e.printStackTrace();
      }
    }
}