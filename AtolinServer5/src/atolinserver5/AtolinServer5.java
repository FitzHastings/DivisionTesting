package atolinserver5;

import atolincore5.DeviceClientSocket;
import atolincore5.DeviceServerSocket;
import atolincore5.ServerSettings;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class AtolinServer5 {
    
    private static Device device;
    private static Registry localReg;
    private static Thread t;
    
  public static void main(String[] args) {
      try {
          device = new Device();
          AtolinServer5.startCommandLine();
          ServerSettings settings = new ServerSettings();
          
          File settingsFile = new File("Settings.xml");
          if(settingsFile.exists())
            settings.loadSettings(settingsFile);
          else settings.saveSettings(settingsFile);

          
          //localReg.bind(settings.getDeviceName(), (iDevice)device);
          for(int i = 0; i < settings.getConnections().size(); i++) {
            // Unless I'm going insane there should be no difference in perfomance between declaring a variable
            // inside and outside of the loop.
            localReg = LocateRegistry.createRegistry(settings.getConnections().get(i).getPort());
            
            localReg.rebind(settings.getConnections().get(i).getName(), 
                    UnicastRemoteObject.exportObject(new Device(),
                    0, 
                    new DeviceClientSocket(settings.getConnections().get(i).getHost()), 
                    new DeviceServerSocket()));
            
              System.out.println("DeviceServer opened : "+settings.getConnections().get(i).getName()+" "+settings.getConnections().get(i).getHost()+":"+settings.getConnections().get(i).getPort());
          }
        }catch (Exception e) {
          e.printStackTrace();
        }
    }
    
    private static void startCommandLine() {
    t = new Thread(() -> {
      try {
        System.out.print("Device Server: ");
        String cmd = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while((cmd = br.readLine()) != null) {
          startCommand(cmd);
        }
      }catch(Exception ex) {
        ex.printStackTrace();
        System.exit(0);
      }
    });
    //t.setDaemon(true);
    t.start();
  }
    
    private static void handleInfo()
    {
        System.out.print("Operator ID: ");
        System.out.println(device.getOperatorID());
        System.out.print("Model ID: ");
        System.out.println(device.getModelID());
        System.out.print("Mode: ");
        System.out.println(device.getMode());
        System.out.print("Submode: ");
        System.out.println(device.getSubmode());
        System.out.print("Receipt Number: ");
        System.out.println(device.getReceiptNumber());
        System.out.print("Docuemnt Number: ");
        System.out.println(device.getDocumentNumber());
        System.out.print("ShiftNumber: ");
        System.out.println(device.getShiftNumber());
        System.out.print("ShiftState: ");
        System.out.println(device.getShiftState());
        System.out.print("Line Length: ");
        System.out.println(device.getLineLength());
        System.out.print("Line Length in Pixels: ");
        System.out.println(device.getLineLengthInPixels());
        System.out.print("Current Receipt Sum: ");
        System.out.println(device.getCurrentReceiptSum());       
        System.out.print("Current Receipt Type ");
        System.out.println(device.getCurrentReceiptType());       
        System.out.print("Model Name: ");
        System.out.println(device.getModelName());      
        System.out.print("Firmware Version: ");
        System.out.println(device.getFirmwareVersion());
        System.out.print("FMD is Present: ");
        System.out.println(device.FMDIsPresent());
        System.out.print("FMD is Fiscal: ");
        System.out.println(device.FMDIsFiscal());
        System.out.print("Cashdrawer is Open: ");
        System.out.println(device.cashdrawerIsOpen());
        System.out.print("Has Papert: ");
        System.out.println(device.hasPaper());
        System.out.print("Is Fiscal: ");
        System.out.println(device.isFiscal());
        System.out.print("Is Blocked: ");
        System.out.println(device.isBlocked());
        System.out.print("Cover is Open: ");
        System.out.println(device.coverIsOpen());
        System.out.print("Printer Connection is Lost: ");
        System.out.println(device.printerConnectionIsLost());
    }
    
    private static void handleQuery()
    {
        device.queryInfo();
        System.out.println("Attempt to Qyery info is complete.");
    }
    
    private  static void handleHelp()
    {
        System.out.println("Device Server accepts the following commands:");
        System.out.println(">'e' or 'exit' to exit the programm,");
        System.out.println(">'i' or 'info' to display the current state of the Device.");
        System.out.println(">'q' or 'query' to force the server to query info");
    }
    
    private static void startCommand(String command) {
    try {
        if(command.equals("exit") || command.equals("e"))
            System.exit(0);
        else if(command.equals("i") || command.equals("info"))
            handleInfo();
        else if(command.equals("q") || command.equals("query"))
            handleQuery();
        else if(command.equals("?"))
            handleHelp();
        else
          System.out.println("Command not found.");
    } catch(Exception ex) {
      ex.printStackTrace();
    } finally {
      System.out.print("Device Server: ");
    }
  }
}