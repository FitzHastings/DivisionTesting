package division.util;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class GzipUtil {
  public static byte[] serializable(Object object) throws IOException {
    try(ByteArrayOutputStream out = new ByteArrayOutputStream(); ObjectOutputStream objOut = new ObjectOutputStream(out)) {
      objOut.writeObject(object);
      return out.toByteArray();
    }
  }
  
  public static byte[] serializable_old(Object object) throws IOException {
    byte[] bytes = new byte[0];
    ByteArrayOutputStream out = null;
    ObjectOutputStream objOut = null;
    try {
      out = new ByteArrayOutputStream();
      objOut = new ObjectOutputStream(out);
      objOut.writeObject(object);
      bytes = out.toByteArray();
    }catch(Exception ex) {
      throw new IOException(ex);
    }finally {
      if(out != null) {
        out.reset();
        out.flush();
        out.close();
      }
      if(objOut != null) {
        objOut.reset();
        objOut.flush();
        objOut.close();
      }
      
      out    = null;
      objOut = null;
      object = null;
    }
    return bytes;
  }
  
  public static <T> T deserializable(byte[] bytes, Class<? extends T> type) throws IOException, ClassNotFoundException {
    return type.cast(deserializable(bytes));
  }
  
  public static Object deserializable(byte[] bytes) throws IOException, ClassNotFoundException {
    try(ByteArrayInputStream inByte = new ByteArrayInputStream(bytes); ObjectInputStream in = new ObjectInputStream(inByte)) {
      return in.readObject();
    }
  }

  public static Object deserializable_old(byte[] bytes) throws IOException, ClassNotFoundException {
    if(bytes == null || bytes.length == 0)
      return null;
    Object o = null;
    ByteArrayInputStream inByte = null;
    ObjectInputStream in = null;
    try {
      inByte = new ByteArrayInputStream(bytes);
      in = new ObjectInputStream(inByte);
      o = in.readObject();
    }catch(IOException ex) {
      throw new IOException(ex);
    }catch(ClassNotFoundException ex) {
      throw  new ClassNotFoundException(null, ex);
    }finally {
      if(inByte != null) {
        inByte.reset();
        inByte.close();
      }
      if(in != null) {
        in.close();
      }
      bytes  = null;
      inByte = null;
      bytes  = null;
      in     = null;
    }
    return o;
  }
  
  public static byte[] zip(Object object) throws IOException {
    return zip(serializable(object));
  }
  
  public static byte[] zipFiles(BytesFile... files) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ZipOutputStream zop = new ZipOutputStream(bos);
    for(BytesFile f:files) {
      ZipEntry ze = new ZipEntry(f.getName());
      zop.putNextEntry(ze);
      zop.write(f.getSource());
      zop.closeEntry();
    }
    zop.finish();
    zop.close();
    byte[] b = bos.toByteArray();
    bos.flush();
    bos.close();
    bos = null;
    zop = null;
    return b;
  }
  
  public static byte[] zip(byte[] source) throws IOException {
    try(ByteArrayOutputStream bos = new ByteArrayOutputStream();ZipOutputStream zop = new ZipOutputStream(bos)) {
      ZipEntry ze = new ZipEntry("name");
      zop.putNextEntry(ze);
      zop.write(source);
      zop.closeEntry();
      return bos.toByteArray();
    }
  }

  public static byte[] zip_old(byte[] source) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ZipOutputStream zop = new ZipOutputStream(bos);
    ZipEntry ze = new ZipEntry("name");
    zop.putNextEntry(ze);
    zop.write(source);
    zop.closeEntry();
    zop.finish();
    zop.close();
    byte[] b = bos.toByteArray();
    bos.flush();
    bos.close();
    bos = null;
    zop = null;
    return b;
  }

  public static byte[] gzip(Object object) throws IOException {
    return gzip(serializable(object));
  }
  
  public static byte[] gzip(byte[] source) throws IOException {
    if(source == null || source.length == 0)
      return new byte[0];
    
    ByteArrayOutputStream bos = null;
    GZIPOutputStream zop = null;
    
    try {
      bos = new ByteArrayOutputStream();
      zop = new GZIPOutputStream(bos);
      zop.write(source);
      zop.flush();
      zop.finish();
      zop.close();
      source = null;
      source = bos.toByteArray();
    }catch(IOException ex) {
      throw new IOException(ex);
    }finally {
      if(bos != null) {
        bos.reset();
        bos.flush();
        bos.close();
      }
      if(zop != null) {
        zop.flush();
        zop.finish();
        zop.close();
      }
      zop = null;
      bos = null;
    }
    return source;
  }

  public static <T> T getObjectFromZip(byte[] zipsource, Class<? extends T> type) throws IOException, ClassNotFoundException {
    return deserializable(unzip(zipsource), type);
  }

  public static Object getObjectFromZip(byte[] zipsource) throws IOException, ClassNotFoundException {
    return deserializable(unzip(zipsource));
  }

  public static byte[] unzip(byte[] source) throws IOException {
    ByteArrayInputStream bis=new ByteArrayInputStream(source);
    ZipInputStream zip=new ZipInputStream(bis);
    zip.getNextEntry();
    ByteArrayOutputStream bos=new ByteArrayOutputStream();
    int read;
    byte[] buf=new byte[1024];
    while((read=zip.read(buf))!=-1)
      bos.write(buf,0,read);
    zip.close();
    zip = null;
    source = bos.toByteArray();
    bis.close();
    bis = null;
    bos.close();
    bos = null;
    return source;
  }
  
  public static <T> T getObjectFromGzip(byte[] gzipsource, T defvalue) throws IOException, ClassNotFoundException {
    return deserializable(ungzip(gzipsource), (Class<? extends T>) defvalue.getClass());
  }

  public static <T> T getObjectFromGzip(byte[] gzipsource, Class<? extends T> type) throws IOException, ClassNotFoundException {
    return deserializable(ungzip(gzipsource), type);
  }

  public static Object getObjectFromGzip(byte[] gzipsource) throws IOException, ClassNotFoundException {
    return deserializable(ungzip(gzipsource));
  }

  public static byte[] ungzip(byte[] source) throws IOException {
    if(source == null || source.length == 0)
      return null;
    ByteArrayInputStream bis = null;
    GZIPInputStream zip = null;
    ByteArrayOutputStream bos = null;

    try {
      bis = new ByteArrayInputStream(source);
      zip = new GZIPInputStream(bis);
      bos = new ByteArrayOutputStream();
      int read;
      byte[] buf=new byte[1024];
      while((read=zip.read(buf))!=-1)
        bos.write(buf,0,read);
      source = bos.toByteArray();
    }catch(IOException ex) {
      throw new IOException(ex);
    }finally {
      if(bis != null) {
        bis.reset();
        bis.close();
      }
      if(zip != null)
        zip.close();
      if(bos != null) {
        bos.reset();
        bos.flush();
        bos.close();
      }
      zip = null;
      bis = null;
      bos = null;
    }
    return source;
  }
}