package division.json;

import conf.P;
import division.fx.PropertyMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class RESTReader {
  private final ObservableList<RESTChangeListener> listeners = FXCollections.observableArrayList();
  private ExecutorService pool = Executors.newCachedThreadPool();
  
  private RESTReader() {}
  
  public PropertyMap get(String urlstring, JSONObject request) {
    OutputStream outputStream = null;
    InputStream inputStream = null;
    try {
      URL url = new URL(urlstring);
      URLConnection connection = null;
      if(P.get("http.proxySet", boolean.class)) {
        if(P.get("http.proxyAuth", boolean.class)) {
          Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
              return new PasswordAuthentication(P.String("http.proxyUser"), P.String("http.proxyPassword").toCharArray());
            }
          });
        }
        connection = url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(P.String("http.proxyHost"), P.Integer("http.proxyPort"))));
      }else connection = url.openConnection();
      connection.setDoOutput(true);
      for(Object h:P.get("REST.headers", JSONArray.class)) {
        JSONObject header = JSONObject.fromObject(h);
        connection.setRequestProperty(header.getString("name"), header.getString("value"));
      }

      outputStream = connection.getOutputStream();
      outputStream.write(request.toString().getBytes());
      
      inputStream = connection.getInputStream();
      return PropertyMap.fromJson(readAll(new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))));
    }catch(Exception ex) {
      ex.printStackTrace();
    }finally {
      if(outputStream != null) {
        try{outputStream.flush();}catch(Exception ex){}
        try{outputStream.close();}catch(Exception ex){}
      }
      if(inputStream != null)
        try{inputStream.close();}catch(Exception ex){}
    }
    return null;
  }
  
  public PropertyMap get(String urlstring, String request) {
    return get(urlstring, new JSONObject().accumulate(P.String("REST.json-key"), request));
  }
  
  public static String readAll(final Reader rd) throws IOException {
    final StringBuilder sb = new StringBuilder();
    int cp;
    while((cp = rd.read()) != -1)
      sb.append((char) cp);
    return sb.toString();
  }
  
  public static RESTReader create() {
    return new RESTReader();
  }
  
  public RESTReader add(TextInputControl node, String url, String listPropertyName, int startLength, PropertyMapChoose chooselistener, RequestData data) {
    RESTChangeListener listener = new RESTChangeListener(node, url, listPropertyName, startLength, chooselistener, data);
    listeners.add(listener);
    node.textProperty().addListener(listener);
    return this;
  }
  
  public class RESTChangeListener implements ChangeListener<String> {
    private final TextInputControl node;
    private final ContextMenu nameContextMenu = new ContextMenu();
    private boolean active = true;
    private final String url;
    private final String listPropertyName;
    private final int startLength;
    private final PropertyMapChoose chooselistener;
    private final RequestData data;

    public RESTChangeListener(TextInputControl node, String url, String listPropertyName, int startLength, PropertyMapChoose chooselistener, RequestData data) {
      this.node = node;
      this.url = url;
      this.listPropertyName = listPropertyName;
      this.startLength = startLength;
      this.chooselistener = chooselistener;
      this.data = data;
    }

    public boolean isActive() {
      return active;
    }

    public void setActive(boolean active) {
      this.active = active;
    }

    public TextInputControl getNode() {
      return node;
    }

    public ContextMenu getNameContextMenu() {
      return nameContextMenu;
    }

    public String getUrl() {
      return url;
    }

    public String getListPropertyName() {
      return listPropertyName;
    }

    public int getStartLength() {
      return startLength;
    }

    public PropertyMapChoose getChooselistener() {
      return chooselistener;
    }

    public RequestData getData() {
      return data;
    }
    
    @Override
    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
      if(!node.isDisable() && isActive() && newValue.length() > startLength) {
        pool.submit(() -> {
          PropertyMap resp = get(url, newValue);
          Platform.runLater(() -> {
            nameContextMenu.getItems().clear();
            resp.getList(listPropertyName).stream().forEach(o -> {
              MenuItem item = new MenuItem(data.itemName(o));
              item.setOnAction(e -> {
                listeners.stream().forEach(l -> l.setActive(false));
                nameContextMenu.hide();
                chooselistener.choose(o);
                listeners.stream().forEach(l -> l.setActive(true));
              });
              nameContextMenu.getItems().add(item);
            });

            if(!nameContextMenu.isShowing() && !nameContextMenu.getItems().isEmpty())
              nameContextMenu.show(node, Side.BOTTOM, 0, 0);

            if(nameContextMenu.getItems().isEmpty())
              nameContextMenu.hide();
          });
        });
      }else nameContextMenu.hide();
    }
  }
  
  public interface PropertyMapChoose {
    public void choose(PropertyMap p);
  }
  
  public interface RequestData {
    public abstract String itemName(PropertyMap p);
  }
}