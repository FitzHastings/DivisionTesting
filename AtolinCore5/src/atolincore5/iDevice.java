package atolincore5;


//import ru.atol.drivers10.fptr.IFptr;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**  Interface of a device that is implemented on the Server side as Device. 
    This class is used to call device's methods on the client side.
*/
public interface iDevice extends Remote
{

    //I am already missing #define
    
    final String NAME = "Device";
    
    /** loads a device from custom settings (don't use this function it's for testing purposed only) */
    void loadCustomDevice(String settings) throws RemoteException;
    
    /** this function updates info about the device that the device polls on itself*/
    void queryInfo() throws RemoteException;
    
    /** prints the receipt on the server side. Receipt must have an Xml Format 
     * returns value returned by the closeReceipt() function at the end, if
     * Receipt failed to be printed either throws an exception or returns a 
     * negative number.
     * Returns -80085 if an excpetion was thrown by the umarshaller. 
     */
    int printReceipt(String XMLreceipt) throws RemoteException;
    
    /** logs into the device with the login/password pair that is sent in plain text */
    void logIn(String login, String password) throws RemoteException;
    
    /** opens the shift in the fiscal hardware and prints out a report*/
    void openShift() throws RemoteException;
    
    /**closes the shift in the fiscal hardware and prints out a report*/
    void closeShift() throws RemoteException;
    
    /** cancels a pending receipt if there is one*/
    void cancelReceipt() throws RemoteException;
    
    /** prints any custom string. Does not check if string exceeds max width*/
    void printCustomString(String str) throws RemoteException; 
    
    /** makes the device beep*/
    void beep() throws RemoteException;
    
    /** forces the cutter to do a full cut*/
    void cut() throws RemoteException;
    
    /** returns the Operator ID*/
    int getOperatorID() throws RemoteException;

    /** returns the Model ID of the device that is connected to the server*/
    int getModelID() throws RemoteException;//returns model ID declared by the manufacturer
    
    /** Returns the mode of the device */
    int getMode() throws RemoteException;//TODO: Find out what thsi function ACUALLY returns.
    
    /** returns the submode of the device */
    int getSubmode() throws RemoteException;//TODO: look at getMode.
    
    /** returns the receipt Number*/
    int getReceiptNumber() throws RemoteException;
    
    /** returns the document Number*/
    int getDocumentNumber() throws RemoteException;
    
    /** returns the shift number*/
    int getShiftNumber() throws RemoteException;
    
    /** returns the shift state*/
    int getShiftState() throws RemoteException;
    
    /** returns the current receipt type*/
    int getCurrentReceiptType() throws RemoteException;
     
    /** returns the line length in characters of the current device*/
    int getLineLength() throws RemoteException;
    
    /** returns the line length of the current device in pixels*/ 
    int getLineLengthInPixels() throws RemoteException;
    
    //int GetLogicalNumber(); //TODO: Find out what "Logical Number" is and figure out a better name for whatever it is.
    
    /** returns current Receipt total*/
    double getCurrentReceiptSum() throws RemoteException;
    
    /** returns the model name (Legacy(?))*/
    String getModelName() throws RemoteException; //returns model name that can be understood by humans;
    
    /** returns the Firmware Version of the current device*/
    String getFirmwareVersion() throws RemoteException;
    
    /** returns true Fiscal Memory Device is Present*/
    boolean FMDIsPresent() throws RemoteException;//FMD Stands for Fiscal Memory Device
    
    /** returns true if Fiscal Memory Device is Fiscal */
    boolean FMDIsFiscal() throws RemoteException;//FMD Stands for Fiscal Memory Device
    
    /** returns true if cashdrawer is open*/
    boolean cashdrawerIsOpen() throws RemoteException;
    
    /** returns true if device registers paper*/
    boolean hasPaper() throws RemoteException;
    
    /** returns true if device is Fiscal*/
    boolean isFiscal() throws RemoteException;
    
    /** returns true if device is blocked */
    boolean isBlocked() throws RemoteException;
    
    /** returns ture if cover is open */
    boolean coverIsOpen() throws RemoteException;
    
    /** returns true if printer connection is lost*/
    boolean printerConnectionIsLost() throws RemoteException;
    
    /** prints out an X Report */
    void xReport() throws RemoteException; 
    
    /**ebables Error Snatcher, a system which prints error messages on receipts
     automatically terminating them if one of the parameters triggers an exception
     such as: Incorrect Receipt types.*/
    void enableErrorSnatcher() throws RemoteException;
    
    /** squelches Error Snatcher if it was enabled.*/
    void squelchErrorSnatcher() throws RemoteException;
    //boolean printerError();

    //boolean printerHasOverheated();
      
    //boolean cutterError();
}
