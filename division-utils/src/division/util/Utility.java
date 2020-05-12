package division.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import javafx.util.StringConverter;
import javafx.util.converter.BigDecimalStringConverter;
import javafx.util.converter.BigIntegerStringConverter;
import javafx.util.converter.DateStringConverter;
import javafx.util.converter.DateTimeStringConverter;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.FloatStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.LocalDateStringConverter;
import javafx.util.converter.LocalDateTimeStringConverter;
import javafx.util.converter.LongStringConverter;
import javafx.util.converter.NumberStringConverter;
import javafx.util.converter.ShortStringConverter;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.apache.log4j.Logger;

public class Utility {
  private static String[] months = new String[]{
    "Январь","Февраль","Март","Апрель","Май","Июнь",
    "Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь"};

  public final static DecimalFormat decimal = new DecimalFormat("#0.00");
  public final static SimpleDateFormat simpleDate = new SimpleDateFormat("dd.MM.yyyy");
  public final static SimpleDateFormat timeStamp = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
  
  private static NumberFormat df = NumberFormat.getInstance(); // создали форматер
  
  public static String convert(Period p) {
    return p.getDays() > 0 ? p.getDays()+"d" : p.getMonths() > 0 ? p.getMonths()+"m" : p.getYears() > 0 ? p.getYears()+"y" : "";
  }
  
  public static Period convert(String dr) {
    if(dr != null) {
      try {
        return Period.parse(dr);
      }catch(Exception ex) {
        if(dr != null && !"".equals(dr)) {
          String[] split = dr.toLowerCase().split(" ");
          if(split.length == 2) {
            if(split[1].startsWith("д") || split[1].startsWith("d"))
              return Period.of(0, 0, Integer.valueOf(split[0]));
            else if(split[1].startsWith("м") || split[1].startsWith("m"))
              return Period.of(0, Integer.valueOf(split[0]), 0);
            else if(split[1].startsWith("л") || split[1].startsWith("г") || split[1].startsWith("y"))
              return Period.of(Integer.valueOf(split[0]), 0, 0);
          }
        }
      }
    }
    return Period.ZERO;
  }
  
  public static ExecutorService startCommandLine(String str, Function<String, Boolean> fun) {
    ExecutorService e = Executors.newSingleThreadExecutor();
    e.execute(() -> {
      Scanner scanner = new Scanner(System.in);
      System.out.println(str);
      while(scanner.hasNext()) {
        fun.apply(scanner.nextLine());
        System.out.println(str);
      }
    });
    return e;
  }
  
  public static ExecutorService startCommandLine(CountDownLatch countDown, String str, Function<String, Boolean> fun) {
    ExecutorService e = Executors.newSingleThreadExecutor();
    e.execute(() -> {
      try {
        countDown.await();
      }catch(Exception ex) {
        ex.printStackTrace();
      }
      Scanner scanner = new Scanner(System.in);
      System.out.println(str);
      while(scanner.hasNext()) {
        fun.apply(scanner.nextLine());
        System.out.println(str);
      }
    });
    return e;
  }
  
  /*public static String getMonthString(LocalDate date) {
    return months[date.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())];
  }*/

  public static String getMonthString(java.util.Date date) {
    return months[getMonth(date)];
  }

  public static String getMonthString(java.sql.Date date) {
    return getMonthString(new java.util.Date(date.getTime()));
  }

  public static String getMonthString(java.sql.Timestamp timestamp) {
    return getMonthString(new java.util.Date(timestamp.getTime()));
  }

  public static int getDay(java.util.Date date) {
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(date.getTime());
    return c.get(Calendar.DAY_OF_MONTH);
  }

  public static int getDay(java.sql.Date date) {
    return getDay(new java.util.Date(date.getTime()));
  }

  public static int getDay(java.sql.Timestamp timestamp) {
    return getDay(new java.util.Date(timestamp.getTime()));
  }
  
  public static int getMonth(java.util.Date date) {
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(date.getTime());
    return c.get(Calendar.MONTH);
  }

  public static int getMonth(java.sql.Date date) {
    return getMonth(new java.util.Date(date.getTime()));
  }

  public static int getMonth(java.sql.Timestamp timestamp) {
    return getMonth(new java.util.Date(timestamp.getTime()));
  }

  public static int getYear(java.sql.Timestamp timestamp) {
    return getYear(new java.util.Date(timestamp.getTime()));
  }

  public static int getYear(java.sql.Date date) {
    return getYear(new java.util.Date(date.getTime()));
  }

  public static int getYear(java.util.Date date) {
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(date.getTime());
    return c.get(Calendar.YEAR);
  }
  
  
  
  public static java.util.Date convert(LocalDate date) {
    return convert(date, 0, 0, 0, 0);
  }
  
  public static java.util.Date convert(LocalDate date, int h, int m, int s, int ms) {
    Calendar c = Calendar.getInstance();
    c.set(date.getYear(), date.getMonthValue()-1, date.getDayOfMonth(), h, m, s);
    c.set(c.MILLISECOND, ms);
    return new java.util.Date(c.getTimeInMillis());
  }
  
  
  
  public static java.sql.Date convertToSqlDate(LocalDate date, int h, int m, int s, int ms) {
    Calendar c = Calendar.getInstance();
    c.set(date.getYear(), date.getMonthValue()-1, date.getDayOfMonth(), h, m, s);
    c.set(c.MILLISECOND, ms);
    return new java.sql.Date(c.getTimeInMillis());
  }
  
  public static java.sql.Date convertToSqlDate(LocalDate date) {
    return convertToSqlDate(date, 0, 0, 0, 0);
  }
  
  
  
  public static java.sql.Timestamp convertToTimestamp(LocalDateTime date) {
    Calendar c = Calendar.getInstance();
    c.set(date.getYear(), date.getMonthValue()-1, date.getDayOfMonth(), date.getHour(), date.getMinute(), date.getSecond());
    return new java.sql.Timestamp(c.getTimeInMillis());
  }
  
  public static java.sql.Timestamp convertToTimestamp(LocalDate date) {
    Calendar c = Calendar.getInstance();
    c.set(date.getYear(), date.getMonthValue()-1, date.getDayOfMonth(), 0, 0, 0);
    return new java.sql.Timestamp(c.getTimeInMillis());
  }
  
  public static long getTimeInMillis(LocalDate date) {
    Calendar c = Calendar.getInstance();
    c.set(date.getYear(), date.getMonthValue()-1, date.getDayOfMonth(), 0, 0, 0);
    return c.getTimeInMillis();
  }
  
  public static LocalDate convert(java.sql.Date date) {
    return date == null ? null : convert(date.getTime());
  }
  
  public static LocalDate convert(long date) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault()).toLocalDate();
  }
  
  public static LocalDateTime convertToDateTime(long date) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault());
  }
  
  public static LocalDate convert(java.util.Date date) {
    return date == null ? null : convert(date.getTime());
  }
  
  public static LocalDate convert(Timestamp date) {
    return date == null ? null : convert(date.getTime());
  }
  
  public static LocalDateTime convertToDateTime(Timestamp date) {
    return date == null ? null : convertToDateTime(date.getTime());
  }
  
  public static String format(long d) {
    return format(new Timestamp(d));
  }
  
  public static String format(double d) {
    return decimal.format(d).replace(',', '.');
  }
  
  public static String format(Double d) {
    return d==null?"":decimal.format(d.doubleValue()).replace(',', '.');
  }

  public static String format(java.sql.Date date) {
    return date == null ? "" : simpleDate.format(new java.util.Date(date.getTime()));
  }

  public static String format(java.util.Date date) {
    return date == null ? "" : simpleDate.format(date);
  }

  public static String format(Timestamp tmTimestamp) {
    return format(tmTimestamp.toLocalDateTime());
  }

  public static java.util.Date parse(String date) throws ParseException {
    return simpleDate.parse(date);
  }

  public static java.sql.Date parseToSqlDate(String date) throws ParseException {
    return new java.sql.Date(parse(date).getTime());
  }

  public static java.sql.Timestamp parseToTimeStamp(String timestamp) throws ParseException {
    return new java.sql.Timestamp(timeStamp.parse(timestamp).getTime());
  }
  
  public static String costToString(Double cost) {
    return new jAmount(cost).toString();
  }
  
  public static String costToString(BigDecimal cost) {
    return new jAmount(cost).toString();
  }
  
  public static String doubleToString(BigDecimal doub, int scale) {
    df.setMaximumFractionDigits(scale); // не больше пяти цифр после запятой
    df.setMinimumFractionDigits(scale);
    String doubleString = df.format(doub);
    return doubleString;
  }
  
  public static String doubleToString(double doub, int scale) throws ParseException {
    df.setMaximumFractionDigits(scale); // не больше пяти цифр после запятой
    df.setMinimumFractionDigits(scale);
    String doubleString = String.valueOf(df.parse(df.format(doub)).doubleValue());
    return doubleString;
  }
  
  public static String join(Object[] arr, String joiner) {
    String jarr = "";
    if(arr != null) {
      for(Object o:arr)
        jarr += joiner+String.valueOf(o);
      if(jarr.length() > 0)
        jarr = jarr.substring(joiner.length());
    }
    return jarr;
  }
  
  public static String writeBytesToFile(byte[] im, String fileName) throws Exception {
    if(im == null || im.length == 0)
      return null;
    File file = FileLoader.createFileIfNotExists(fileName);
    Files.write(file.toPath(), im);
    return file.getAbsolutePath();
  }
  
  public static String writeBytesToFile(byte[] im) throws Exception {
    return writeBytesToFile(im, "tmp"+File.separator+"img-"+System.currentTimeMillis()+new Random());
  }
  
  public static String writeImageToFile(byte[] im) throws Exception {
    return writeImageToFile(im, "tmp"+File.separator+"img-"+System.currentTimeMillis()+".png", true);
  }
  
  public static String writeImageToFile(byte[] im, String fileName) throws Exception {
    return writeImageToFile(im, fileName, false);
  }
  
  public static String writeImageToFile(byte[] im, String fileName, boolean tmpFile) throws Exception {
    if(im == null || im.length == 0)
      return null;
    ByteArrayInputStream in = null;
    File file = FileLoader.createFileIfNotExists(fileName, true);
    try {
      if(tmpFile)
        file.deleteOnExit();
      ImageIO.write(ImageIO.read(in = new ByteArrayInputStream(im)), fileName.substring(fileName.lastIndexOf(".")+1).toUpperCase(), file);
    }catch(Exception ex) {
      throw new Exception(ex);
    }finally {
      if(in != null) {
        in.reset();
        in.close();
      }
      in = null;
    }
    return file.getPath();
  }
  
  public static Image setScale(byte[] im, int width, int height) throws Exception {
    ByteArrayInputStream in = null;
    try {
      return setScale(ImageIO.read(in = new ByteArrayInputStream(im)), width, height);
    }catch(Exception ex) {
      throw new Exception(ex);
    }finally {
      if(in != null) {
        in.reset();
        in.close();
        in = null;
      }
    }
  }
  
  public static Image setScale(BufferedImage img, int width, int height) throws Exception {
    int w = img.getWidth();
    int h = img.getHeight();

    if(w > h) {
      h *= (double)width/w;
      w = width;
    }else if(w < h) {
      w *= (double)height/h;
      h = height;
    }else {
      w = width;
      h = height;
    }
    return img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
  }
  
  public static BufferedImage getImageFromBytes(byte[] im) throws Exception {
    ByteArrayInputStream in = null;
    try {
      return ImageIO.read(in = new ByteArrayInputStream(im));
    }catch(Exception ex) {
      throw new Exception(ex);
    }finally {
      if(in != null) {
        in.reset();
        in.close();
        in = null;
      }
    }
  }
  
  public static byte[] getBytesFromImage(BufferedImage image, String type) throws IOException {
    ByteArrayOutputStream baos = null;
    try {
      baos = new ByteArrayOutputStream();
      ImageIO.write(image, type, baos);
      return baos.toByteArray();
    }catch(IOException e) {
      throw new IOException(e);
    }finally {
      if(baos != null) {
        baos.reset();
        baos.flush();
        baos.close();
        baos = null;
      }
    }
  }
  
  public static byte[] getBytesFromFile(String fileName) throws Exception {
    return getBytesFromFile(new File(fileName));
  }
  
  public static byte[] getBytesFromFile(File file) throws Exception {
    return Files.readAllBytes(file.toPath());
    /*FileInputStream in = null;
    try {
      in = new FileInputStream(file);
      byte[] bytes = new byte[in.available()];
      in.read(bytes);
      return bytes;
    }catch(Exception ex) {
      throw new Exception(ex);
    }finally {
      if(in != null) {
        in.close();
        in = null;
      }
    }*/
  }
  
  public static String getStringFromFile(String fileName) {
    return getStringFromFile(fileName, false);
  }
  
  public static String getStringFromFile(String fileName, boolean withNewLine) {
    String txt = "";
    Scanner scanner = null;
    try {
      scanner = new Scanner(new File(fileName));
    } catch (FileNotFoundException ex) {
      Logger.getRootLogger().error(ex);
    }
    if(scanner != null)
      while(scanner.hasNextLine())
        txt += scanner.nextLine()+(withNewLine ? "\n" : "");
    return txt;
  }
  
  public static Icon getIcon(byte[] im, int w, int h) throws Exception {
    return im!=null&&im.length>0?new ImageIcon(setScale(getImageFromBytes(im), w, h)):null;
  }
  
  public static String validatePath(String path) {
    return path==null?null:path.replaceAll("[\\\\/]", "\\"+File.separator);
  }
  
  public static Map<String, String> getMD5Files(String dirName, File file) throws IOException, NoSuchAlgorithmException {
    dirName = validatePath(dirName);
    Map<String, String> files = new TreeMap<>();
    if(file.exists()) {
      if(!file.isDirectory()) {
        String key = dirName==null?file.getPath():file.getAbsolutePath().substring(new File(dirName).getAbsolutePath().length()+1);
        files.put(key, getMD5(file));
      }else {
        for(File f:file.listFiles((File dir, String name) -> !name.endsWith("~")))
          if(!f.isHidden())
            files.putAll(getMD5Files(dirName, f));
      }
    }
    return files;
  }
  
  public static Map<String, String> getMD5Files(String dirName, String fileName) throws Exception {
    File file = new File(fileName);
    Map<String, String> files = new TreeMap<>();
    if(file.exists()) {
      if(!file.isDirectory()) {
        String key = dirName==null?file.getPath():file.getAbsolutePath().substring(new File(dirName).getAbsolutePath().length()+1);
        files.put(key, getMD5(file));
      }else {
        for(File f:file.listFiles((File dir, String name) -> !name.endsWith("~")))
          if(!f.isHidden())
            files.putAll(getMD5Files(dirName, f.getPath()));
      }
    }
    return files;
  }

  public static String getMD5(File f) throws IOException, NoSuchAlgorithmException {
    String output = "";
    InputStream is = null;
    MessageDigest digest = MessageDigest.getInstance("MD5");
    try {
      is = new FileInputStream(f);
      byte[] buffer = new byte[819200];
      int read = 0;
      while ((read = is.read(buffer)) > 0) {
        digest.update(buffer, 0, read);
      }
      byte[] md5sum = digest.digest();
      BigInteger bigInt = new BigInteger(1, md5sum);
      output = bigInt.toString(16);
    }catch(Exception ex) {
      Logger.getRootLogger().error(ex);
    }finally {
      if(is != null)
        is.close();
      is = null;
    }
    return output;
  }
  
  public static String getMD5(Object object) throws Exception {
    return getMD5(GzipUtil.serializable(object));
  }
  
  public static String getMD5(byte[] bytes) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("MD5");
    byte[] md5sum = digest.digest(bytes);
    BigInteger bigInt = new BigInteger(1, md5sum);
    return bigInt.toString(16);
  }

  public static String format(LocalDate localDate) {
    return localDate == null ? "" : localDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
  }
  
  public static LocalDate parseLocalDate(String localDate) {
    return LocalDate.parse(localDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
  }
  
  public static String format(LocalDateTime localDate) {
    return localDate == null ? "" : localDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
  }
  
  public static LocalDateTime parseLocalDateTime(String localDateTime) {
    return LocalDateTime.parse(localDateTime, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
  }
  
  public static String format(LocalTime localTime) {
    return localTime == null ? "" : localTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
  }
  
  public static LocalTime parseLocalTime(String localTime) {
    return LocalTime.parse(localTime, DateTimeFormatter.ofPattern("HH:mm:ss"));
  }
  
  public static String format(Period period) {
    return getTypeOfPeriod(period.getDays(), PeriodType.DAY)+getTypeOfPeriod(period.getMonths(), PeriodType.MONTH)+getTypeOfPeriod(period.getYears(), PeriodType.YEAR);
  }
  
  public static Integer getCount(Period p) {
    return p.getDays() > 0 ? p.getDays() : p.getMonths() > 0 ? p.getMonths() : p.getYears();
  }
  
  public static String convertToReadable(Period p) {
    String[] types = getPeriodTypes(getCount(p));
    return types.length == 0 ? "" : (getCount(p)+" "+(p.getDays() > 0 ? types[0] : p.getMonths() > 0 ? types[1] : types[2]));
  }
  
  public static String getType(Period p) {
    String[] types = getPeriodTypes(getCount(p));
    return types.length == 0 ? "" : (p.getDays() > 0 ? types[0] : p.getMonths() > 0 ? types[1] : types[2]);
  }
  
  public enum PeriodType {DAY, MONTH, YEAR}
  
  public static String getTypeOfPeriod(int count, PeriodType pt) {
    String[] types = getPeriodTypes(count);
    if(types.length == 0)
      return "";
    String rt = count+" ";
    switch(pt) {
      case DAY:   rt += types[0];break;
      case MONTH: rt += types[1];break;
      case YEAR:  rt += types[2];break;
    }
    return rt;
  }
  
  public static String[] getPeriodTypes(int count) {
    if(count <= 0) 
      return new String[0];
    String[] types = new String[0];
    if(count%10 == 0 || count%10 >= 5 && count%10 <= 9 || count%100 >= 11 && count%100 <= 14)
      types = new String[]{"дней","месяцев","лет"};
    else if(count%10 == 1) types = new String[]{"день","месяц","год"};
    else types = new String[]{"дня","месяца","года"};
    return types;
  }
  
  public static StringConverter getConverter(Object item) {
    StringConverter converter = new DefaultStringConverter();
    if(item instanceof Integer)
      converter = new IntegerStringConverter();
    else if(item instanceof Long)
        converter = new LongStringConverter();
    else if(item instanceof Short)
      converter = new ShortStringConverter();
    else if(item instanceof Float)
      converter = new FloatStringConverter();
    else if(item instanceof Double)
      converter = new DoubleStringConverter();
    else if(item instanceof BigInteger)
      converter = new BigIntegerStringConverter();
    else if(item instanceof BigDecimal)
      converter = new BigDecimalStringConverter();
    else if(item instanceof Number)
      converter = new NumberStringConverter();
    else if(item instanceof Period)
      converter = new StringConverter<Period>() {
        @Override
        public String toString(Period object) {
          return Utility.format(object);
        }

        @Override
        public Period fromString(String string) {
          return Utility.convert(string);
        }
      };
    else if(item instanceof LocalDate)
      converter = new LocalDateStringConverter(DateTimeFormatter.ofPattern("dd.MM.yyyy"), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    else if(item instanceof Date)
      converter = new DateStringConverter(new SimpleDateFormat("dd.MM.yyyy"));
    else if(item instanceof java.sql.Date)
      converter = new DateStringConverter(new SimpleDateFormat("dd.MM.yyyy"));
    else if(item instanceof java.sql.Timestamp)
      converter = new DateTimeStringConverter(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"));
    else if(item instanceof LocalDateTime)
      converter = new LocalDateTimeStringConverter(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    return converter;
  }
}