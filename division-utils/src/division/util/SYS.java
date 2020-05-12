package division.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SYS {
  public static String getHDDSerial() throws Exception {
    String hdds = "";
    if(System.getProperty("os.name").toLowerCase().contains("win")) {
      String sc = "cmd /c" + "wmic diskdrive get serialnumber";
      Process p = Runtime.getRuntime().exec(sc);
      p.waitFor();
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      StringBuilder sb = new StringBuilder();
      while ((line = reader.readLine()) != null)
        sb.append(line);
      hdds = sb.substring(sb.toString().lastIndexOf("r") + 1).trim();
    }
    if(System.getProperty("os.name").toLowerCase().contains("nux")) {
      String sc = "/sbin/udevadm info --query=property --name=sda"; // get HDD parameters as non root user
      String[] scargs = {"/bin/sh", "-c", sc};

      Process p = Runtime.getRuntime().exec(scargs);
      p.waitFor();

      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream())); 
      String line;
      StringBuilder sb  = new StringBuilder();

      while ((line = reader.readLine()) != null)
        if(line.indexOf("ID_SERIAL_SHORT") != -1)
          sb.append(line);

      hdds = sb.toString().substring(sb.toString().indexOf("=") + 1);
    }
    return hdds;
  }
}