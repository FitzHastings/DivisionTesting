package division.util;

import com.shtrih.fiscalprinter.command.FlexCommand;
import com.shtrih.fiscalprinter.command.FlexCommands;
import com.shtrih.fiscalprinter.command.FlexCommandsReader;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpos.FiscalPrinter;
import jpos.JposException;
import jpos.config.JposEntry;
import jpos.config.JposEntryRegistry;
import jpos.loader.JposServiceLoader;
import org.apache.commons.lang3.ArrayUtils;

public class Fprinter {
  private static TreeMap<String, FiscalPrinter> printers = new TreeMap<>();
  private static FlexCommands commands = new FlexCommands();
  static {
    try {
      new FlexCommandsReader().loadFromXml("commands.xml", commands);
    }catch (Exception ex) {
      Logger.getLogger(Fprinter.class.getName()).log(Level.SEVERE, "Не загрузились команды ФР", ex);
    }
  }
  
  public static void open(String printerName) throws JposException {
    if(!printers.containsKey(printerName)) {
      FiscalPrinter printer = new FiscalPrinter();
      printer.open(printerName);
      printer.claim(0);
      printer.setDeviceEnabled(true);
      printers.put(printerName, printer);
    }
  }
  
  private static Object getPropValue(String printerName, String propName) {
    JposEntryRegistry registry = JposServiceLoader.getManager().getEntryRegistry();
    registry.load();
    JposEntry en = registry.getJposEntry(printerName);
    return en.getPropertyValue(propName);
  }
  
  public static String[] getPrinters() {
    String[] names = new String[0];
    JposEntryRegistry registry = JposServiceLoader.getManager().getEntryRegistry();
    Enumeration<JposEntry> enumer = registry.getEntries();
    while(enumer.hasMoreElements()) {
      JposEntry en = enumer.nextElement();
      if("FiscalPrinter".equals(en.getPropertyValue("deviceCategory")))
        names = (String[]) ArrayUtils.add(names, en.getLogicalName());
    }
    return names;
  }
  
  public static void printZReport(String printerName) throws JposException {
    System.out.println("printZReport "+65);
    open(printerName);
    FlexCommand command = commands.itemByCode(65);
    command.getInParams().get(0).setValue((String) getPropValue(printerName,"sysAdminPassword"));
    printers.get(printerName).directIO(0x1D, new int[1], command);
    close(printerName);
  }
  
  public static void printXReport(String printerName) throws JposException {
    System.out.println("printXReport "+64);
    open(printerName);
    FlexCommand command = commands.itemByCode(64);
    command.getInParams().get(0).setValue((String) getPropValue(printerName,"sysAdminPassword"));
    printers.get(printerName).directIO(0x1D, new int[1], command);
    close(printerName);
  }
  
  /*public static int printReceipt(String printerName, String positionName, double positionCount, double positionCost, double clientCash) throws JposException {
    open(printerName);
    sale(printerName, positionName, positionCount, positionCost);
    closeReceipt(printerName, clientCash);
    int number = getCurrentReceiptNumber(printerName);
    close(printerName);
    return number;
  }*/
  
  public static void sale(String printerName, String positionName, double positionCount, BigDecimal positionCost) throws JposException {
    FlexCommand command = commands.itemByCode(128);
    command.getInParams().get(0).setValue((String) getPropValue(printerName, "operatorPassword"));
    command.getInParams().get(1).setValue(getCount(positionCount));
    command.getInParams().get(2).setValue(getAmount(positionCost));
    command.getInParams().get(3).setValue((String) getPropValue(printerName, "department"));
    command.getInParams().get(4).setValue("0");
    command.getInParams().get(5).setValue("0");
    command.getInParams().get(6).setValue("0");
    command.getInParams().get(7).setValue("0");
    command.getInParams().get(8).setValue(getString(positionName, command.getInParams().get(8).getSize()));
    printers.get(printerName).directIO(0x1D, new int[1], command);
  }
  
  public static void closeReceipt(String printerName, BigDecimal clientCash, String string) throws JposException {
    FlexCommand command = commands.itemByCode(133);
    command.getInParams().get(0).setValue((String) getPropValue(printerName, "operatorPassword"));
    command.getInParams().get(1).setValue(getAmount(clientCash));
    command.getInParams().get(2).setValue("0");
    command.getInParams().get(3).setValue("0");
    command.getInParams().get(4).setValue("0");
    command.getInParams().get(5).setValue("0");
    command.getInParams().get(6).setValue("0");
    command.getInParams().get(7).setValue("0");
    command.getInParams().get(8).setValue("0");
    command.getInParams().get(9).setValue("0");
    command.getInParams().get(10).setValue(getString(string, command.getInParams().get(10).getSize()));
    printers.get(printerName).directIO(0x1D, new int[1], command);
  }
  
  /*public static String doubleToString(double doub, int scale) throws ParseException {
    NumberFormat df = NumberFormat.getInstance();
    df.setMaximumFractionDigits(scale); // не больше пяти цифр после запятой
    df.setMinimumFractionDigits(scale);
    String ndsString = String.valueOf(df.parse(df.format(doub)).doubleValue());
    if(ndsString.length()-1-ndsString.indexOf(".") < scale) {
      while(ndsString.length()-1-ndsString.indexOf(".") < scale)
        ndsString += "0";
    }
    return ndsString;
  }*/
  
  public static int getCurrentReceiptNumber(String printerName) throws JposException {
    FlexCommand command = commands.itemByCode(17);
    command.getInParams().get(0).setValue((String) getPropValue(printerName, "operatorPassword"));
    printers.get(printerName).directIO(0x1D, new int[1], command);
    return Integer.parseInt(command.getOutParams().get(5).getValue());
  }
  
  public static void printString(String printerName, String string, int align) throws JposException {
    FlexCommand command = commands.itemByCode(23);
    int size = command.getInParams().get(2).getSize();
    
    String[] sarr = string.split(" ");
    string = "";
    for(String s:sarr) {
      while(string.length() > size) {
        printStr(printerName, string.substring(0, size), size, align);
        string = string.substring(size);
      }
      if(string.length()+s.length() > size-1) {
        printStr(printerName, string, size, align);
        string = "";
      }
      string += s+" ";
    }
    string = string.trim();
    printStr(printerName, string, size, align);
  }
  
  private static void printStr(String printerName, String str, int stringSize, int align) throws JposException {
    FlexCommand command = commands.itemByCode(23);
    command.getInParams().get(0).setValue((String) getPropValue(printerName, "operatorPassword"));
    command.getInParams().get(1).setValue("99");
    command.getInParams().get(2).setValue(getString(str.trim(), stringSize, align));
    printers.get(printerName).directIO(0x1D, new int[1], command);
  }
  
  private static String getString(String str, int stringSize) {
    return getString(str, stringSize, 0);
  }
  
  private static String getString(String str, int stringSize, int align) {
    while(str.length() < stringSize)
      if(align == 0)
        str += " ";
      else str = " "+str;
    return str.substring(0, stringSize);
  }
  
  private static String getAmount(BigDecimal amount) {
    return amount.multiply(new BigDecimal(100)).toBigInteger().toString();
  }
  
  private static String getCount(double count) {
    return String.valueOf(Math.round(count*1000));
  }
  
  public static void closeAll() throws JposException {
    for(String printerName:printers.keySet())
      close(printerName);
  }
  
  public static void close(String printerName) throws JposException {
    if(printers.containsKey(printerName)) {
      printers.get(printerName).clearError();
      printers.get(printerName).clearOutput();
      printers.get(printerName).close();
      printers.remove(printerName);
    }
  }

  @Override
  protected void finalize() throws Throwable {
    closeAll();
    super.finalize();
  }
}