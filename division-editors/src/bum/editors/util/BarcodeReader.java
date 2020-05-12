package bum.editors.util;

import division.swing.guimessanger.Messanger;
import division.xml.Document;
import division.xml.Node;
import gnu.io.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeReader {
  private static ExecutorService pool = Executors.newCachedThreadPool();
  private static ConcurrentLinkedQueue<BarcodeListener> listeners = new ConcurrentLinkedQueue<>();
  private static InputStream in;
  private static SerialPort serialPort;
  private static CommPort commPort;
  
  public static void connect() throws Exception {
    Document document = Document.load("conf"+File.separator+"machinery_configuration.xml");
    Node scaner = document.getRootNode().getNode("SCANER");
    Node port   = scaner.getNode("PORT");
    Node speed  = scaner.getNode("SPEED");
    if(scaner != null && port != null && speed != null) {
      String portName = port.getValue();
      int portSpeed = Integer.valueOf(speed.getValue());
      connect(portName, portSpeed);
    }
  }

  public static void connect(String portName, int portSpeed) throws Exception {
    CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
    if(portIdentifier.isCurrentlyOwned())
      System.out.println("Error: Port is currently in use");
    else {
      System.out.println("OPENING PORT "+portName+"...");
      commPort = portIdentifier.open(BarcodeReader.class.getName(),2000);
      if(commPort instanceof SerialPort) {
        serialPort = (SerialPort)commPort;
        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN);
        serialPort.setSerialPortParams(portSpeed,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
        in = serialPort.getInputStream();
        serialPort.notifyOnDataAvailable(true);
        serialPort.addEventListener(new SerialReader(in));
        System.out.println("PORT "+portName+" IS OPEN ON SPEED "+portSpeed);
      }else System.out.println("Error: Only serial ports are handled by this example.");
    }
  }

  public static class SerialReader implements SerialPortEventListener {
    private BufferedReader reader;

    public SerialReader(InputStream in) {
      reader = new BufferedReader(new InputStreamReader(in));
    }

    private void readSerial() {
      try {
        synchronized(reader) {
          String barcode = reader.readLine();
          if(barcode != null && !barcode.equals(""))
            fireBarcodeListener(barcode);
        }
      }catch(Exception e) {
      }
    }

    @Override
    public void serialEvent(SerialPortEvent events) {
      switch(events.getEventType()) {
        case SerialPortEvent.DATA_AVAILABLE:
          readSerial();
          break;
      }
    }
  }

  public static void close() {
    pool.submit(new Runnable() {
      @Override
      public void run() {
        if(serialPort != null) {
          try{
            listeners.clear();
            in.close();
          }catch(Exception ex) {
            Messanger.showErrorMessage(ex);
          }
          System.out.println("closing...");
          serialPort.removeEventListener();
          System.out.println("closing serial port...");
          serialPort.close();
          System.out.println("finished closing");
        }
      }
    });
  }

  public static void clearBarcodeListeners() {
    listeners.clear();
  }

  public static void addBarcodeListener(BarcodeListener barcodeListener) {
    listeners.add(barcodeListener);
  }

  public static void removeBarcodeListener(BarcodeListener barcodeListener) {
    listeners.remove(barcodeListener);
  }

  public static void fireBarcodeListener(String message) {
    for(BarcodeListener barcodeListener:listeners) {
      try {
        barcodeListener.message(message);
      }catch(Exception ex) {
        System.out.println(ex.getMessage());
      }
    }
  }
}