package division.util.yandex;

import division.json.JsonReader;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;

public class YandexApi {
  public enum MapType {схема, спутник, гибрид}
  public static int returnResulCount = 10;
  
  private static String urlGeocode   = "http://geocode-maps.yandex.ru/1.x/?";
  private static String urlStaticMap = "http://static-maps.yandex.ru/1.x/?";
  
  private static TreeMap<String, String> imageCash = new TreeMap<>();
  
  public static int minWidth  = 100;
  public static int minHeight = 100;
  public static int maxWidth  = 650;
  public static int maxHeight = 450;
  
  public static void setMetro(JSONObject[] divisionAddresses) throws Exception {
    Map<String, String> params = new TreeMap<>();
    params.put("format",  "json");
    params.put("kind",    "metro");
    params.put("result",  String.valueOf(returnResulCount));
    
    for(JSONObject address:divisionAddresses) {
      params.put("geocode", address.getString("location"));
      JSONObject response = getResponse(urlGeocode+JsonReader.encodeParams(params));
      if(getFoundCount(response) > 0){
        JSONObject metro = response.getJSONObject("GeoObjectCollection").getJSONArray("featureMember").getJSONObject(0).getJSONObject("GeoObject");
        address.put("metro-name", metro.getString("name"));
        address.put("metro-location", metro.getJSONObject("Point").getString("pos").replaceAll(" ", ","));
      }
    }
  }
  
  public static void setDistrict(JSONObject[] divisionAddresses) throws Exception {
    Map<String, String> params = new TreeMap<>();
    params.put("format",  "json");
    params.put("kind",    "district");
    params.put("result",  String.valueOf(returnResulCount));
    
    for(JSONObject address:divisionAddresses) {
      params.put("geocode", address.getString("location"));
      JSONObject response = getResponse(urlGeocode+JsonReader.encodeParams(params));
      if(getFoundCount(response) > 0){
        JSONObject district = response.getJSONObject("GeoObjectCollection").getJSONArray("featureMember").getJSONObject(0).getJSONObject("GeoObject");
        address.put("district", district.getString("name"));
      }
    }
  }
  
  public static void reloadAddresses(JSONObject[] divisionAddresses) throws Exception {
    reloadAddresses(divisionAddresses, null);
  }
  
  public static JSONObject[] reloadAddresses(JSONObject[] divisionAddresses, String param) throws Exception {
    for(int i=divisionAddresses.length-1;i>=0;i--) {
      if(param == null || !divisionAddresses[i].containsKey(param)) {
        JSONObject[] addresses = getAddresses(divisionAddresses[i].getString("title"));
        if(addresses.length > 0) {
          divisionAddresses = (JSONObject[]) ArrayUtils.remove(divisionAddresses, i);
          divisionAddresses = (JSONObject[]) ArrayUtils.addAll(divisionAddresses, addresses);
        }
      }
    }
    return divisionAddresses;
  }
  
  public static JSONObject[] getAddresses(String address) throws Exception {
    JSONObject[] addresses = new JSONObject[0];
    if(address != null && !address.equals("")) {
      Map<String, String> params = new TreeMap<>();
      params.put("format",  "json");
      params.put("result",  String.valueOf(returnResulCount));
      params.put("geocode", address);
      
      JSONObject response = getResponse(urlGeocode+JsonReader.encodeParams(params));
      
      if(getFoundCount(response) > 0) {
        
        JSONArray featureMember = response.getJSONObject("GeoObjectCollection").getJSONArray("featureMember");

        for(Object obj:featureMember) {
          JSONObject geoObject = ((JSONObject)obj).getJSONObject("GeoObject");
          JSONObject jsonAddress = geoObject.getJSONObject("metaDataProperty").getJSONObject("GeocoderMetaData").getJSONObject("AddressDetails");
          JSONObject country = jsonAddress.getJSONObject("Country");

          if(country != null && !country.isEmpty()) {
            JSONObject divisionAddress = new JSONObject();
            divisionAddress.put("location",    geoObject.getJSONObject("Point").getString("pos").replaceAll(" ", ","));

            divisionAddress.put("countryCode", country.getString("CountryNameCode"));
            divisionAddress.put("title",       country.getString("AddressLine"));
            divisionAddress.put("countryName", country.getString("CountryName"));

            JSONObject administrativeArea = country.getJSONObject("AdministrativeArea");
            if(administrativeArea != null && !administrativeArea.isEmpty()) {

              if(administrativeArea.containsKey("AdministrativeAreaName"))
                divisionAddress.put("region", administrativeArea.getString("AdministrativeAreaName"));


              JSONObject subAdministrativeArea = administrativeArea.getJSONObject("SubAdministrativeArea");
              if(subAdministrativeArea != null && !subAdministrativeArea.isEmpty()) {

                if(subAdministrativeArea.containsKey("SubAdministrativeAreaName"))
                  divisionAddress.put("subRegion", subAdministrativeArea.getString("SubAdministrativeAreaName"));

                JSONObject locality = subAdministrativeArea.getJSONObject("Locality");
                if(locality != null && !locality.isEmpty()) {

                  if(locality.containsKey("LocalityName"))
                    divisionAddress.put("town", locality.getString("LocalityName"));

                  JSONObject thoroughfare = locality.getJSONObject("Thoroughfare");
                  if(thoroughfare != null && !thoroughfare.isEmpty()) {

                    if(thoroughfare.containsKey("ThoroughfareName"))
                      divisionAddress.put("street", thoroughfare.getString("ThoroughfareName"));

                    JSONObject premise = thoroughfare.getJSONObject("Premise");
                    if(premise != null && !premise.isEmpty() && premise.containsKey("PremiseNumber"))
                      divisionAddress.put("home", premise.getString("PremiseNumber"));
                  }
                }
              }
            }
            addresses = (JSONObject[]) ArrayUtils.add(addresses, divisionAddress);
          }
        }
        setMetro(addresses);
        setDistrict(addresses);
      }
    }
    return addresses;
  }
  
  private static JSONObject getResponse(String url) throws Exception {
    JSONObject json = JsonReader.read(url);
    if(json.containsKey("response"))
      return json.getJSONObject("response");
    throw new IOException(json.getJSONObject("error").getString("status")+" "+json.getJSONObject("error").getString("message"));
  }
  
  private static int getFoundCount(JSONObject response) {
    return Integer.parseInt(response.getJSONObject("GeoObjectCollection").getJSONObject("metaDataProperty").getJSONObject("GeocoderResponseMetaData").getString("found"));
  }
  
  public static Image getMap(
          MapType mapType, 
          int mapWidth, 
          int mapHeight, 
          int mapZoom, 
          float[] mapCenter, 
          JSONObject[] divisionAddresses) throws Exception {
    
    mapWidth  = mapWidth<minWidth?minWidth:mapWidth;
    mapHeight = mapHeight<minHeight?minHeight:mapHeight;
    
    mapWidth  = mapWidth>maxWidth?maxWidth:mapWidth;
    mapHeight = mapHeight>maxHeight?maxHeight:mapHeight;
    
    Map<String, String> params = new TreeMap<>();
    
    switch(mapType) {
      case схема:
        params.put("l",  "map");
        break;
      case спутник:
        params.put("l",  "sat");
        break;
      case гибрид:
        params.put("l",  "sat,skl");
        break;
    }
    
    params.put("size",   mapWidth+","+mapHeight);
    
    if(mapCenter != null && mapCenter.length == 2)
      params.put("ll",    mapCenter[0]+","+mapCenter[1]);
    else if(divisionAddresses == null || divisionAddresses.length == 0) {
      params.put("ll",    "37.619899,55.753676");
    }
    
    if(mapZoom >= 0 && mapZoom <= 18)
      params.put("z",    String.valueOf(mapZoom));
    
    if(divisionAddresses != null && divisionAddresses.length > 0) {
      String pt = "";
      for(JSONObject address:divisionAddresses) {
        if(address.containsKey("location"))
          pt += "~"+address.getString("location").trim().replaceAll(" ", ",")+",pmwtm"+(address.containsKey("metka")?address.getString("metka"):"");
      }
      if(!pt.equals("")) {
        pt.substring(1);
        params.put("pt",    pt.substring(1));
      }else params.put("ll",    "37.619899,55.753676");
    }
    
    BufferedImage im = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_RGB);
    InputStream in = null;
    try {
      String link = urlStaticMap+JsonReader.encodeParams(params);
      im = getImageFromCash(link);
      if(im == null) {
        im = ImageIO.read(in = new URL(link).openStream());
        imageTocash(link, im);
      }
    } catch(Exception ex) {
      throw new Exception(ex);
    } finally {
      if(in != null) {
        in.close();
        in = null;
      }
    }
    return im;
  }
  
  private static void imageTocash(String link, BufferedImage im) {
    try {
      File tmp = new File("yandex");
      tmp.deleteOnExit();
      if(!tmp.exists())
        tmp.mkdir();
      
      File png = new File(tmp.getName()+File.separator+System.currentTimeMillis()+".png");
      png.deleteOnExit();
      
      ImageIO.write(im, "PNG", png);
      imageCash.put(link, png.getAbsolutePath());
    }catch(Exception ex) {}
  }
  
  private static BufferedImage getImageFromCash(String link) throws IOException {
    BufferedImage im = null;
    InputStream in = null;
    if(imageCash.containsKey(link)) {
      try {
        in = new FileInputStream(imageCash.get(link));
        im = ImageIO.read(in);
      }catch(Exception ex) {}
      finally {
        if(in != null) {
          in.close();
          in = null;
        }
      }
    }
    return im;
  }
}