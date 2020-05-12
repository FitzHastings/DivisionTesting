package division.json;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;
import net.sf.json.JSONObject;

public class JsonReader {
  public static String readAll(final Reader rd) throws IOException {
    final StringBuilder sb = new StringBuilder();
    int cp;
    while((cp = rd.read()) != -1)
      sb.append((char) cp);
    return sb.toString();
  }

  public static JSONObject read(final String url) throws Exception {
    InputStream is = null;
    BufferedReader rd = null;
    try {
      is = new URL(url).openStream();
      rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
      String jsonText = readAll(rd);
      JSONObject json = JSONObject.fromObject(jsonText);
      return json;
    } finally {
      if(rd != null) {
        rd.reset();
        rd.close();
      }
      if(is != null)
        is.close();
      rd = null;
      is = null;
    }
  }
  
  public static String encodeParams(final Map<String, String> params) {
    final String paramsUrl = Joiner.on('&').join(// получаем значение вида key1=value1&key2=value2...
    Iterables.transform(params.entrySet(), new Function<Entry<String, String>, String>() {
      @Override
      public String apply(final Entry<String, String> input) {
        try {
          final StringBuffer buffer = new StringBuffer();
          buffer.append(input.getKey());// получаем значение вида key=value
          buffer.append('=');
          buffer.append(URLEncoder.encode(input.getValue(), "utf-8"));// кодируем строку в соответствии со стандартом HTML 4.01
          return buffer.toString();
        }catch (final UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }
    }));
    return paramsUrl;
  }
}