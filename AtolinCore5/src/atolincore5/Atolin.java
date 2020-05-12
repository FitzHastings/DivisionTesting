package atolincore5;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import ru.atol.drivers10.fptr.IFptr;

/** Atolin Class contains utilities that make using Easier. 
 * Atolin Class is non Istantiable, and should be used as a Library of useful
 * methods that make Client and Server's life a little bit easier.
 */
public final class Atolin {
    
    public static final int TaxModeDefault = -1;
    public static final int TaxModePosition = IFptr.LIBFPTR_TM_POSITION;
    public static final int TaxModeUnit = IFptr.LIBFPTR_TM_UNIT;
    
    public static final int AgentTypeNone = IFptr.LIBFPTR_AT_NONE;
    public static final int AgentTypeBank = IFptr.LIBFPTR_AT_BANK_PAYING_AGENT;
    public static final int AgentTypeBankSubajent = IFptr.LIBFPTR_AT_BANK_PAYING_SUBAGENT;
    public static final int AgentTypeAgent = IFptr.LIBFPTR_AT_PAYING_AGENT;
    public static final int AgentTypeSubagent= IFptr.LIBFPTR_AT_PAYING_SUBAGENT;
    public static final int AgentTypeAttorney = IFptr.LIBFPTR_AT_ATTORNEY;
    public static final int AgentTypeCommisionAgent = IFptr.LIBFPTR_AT_COMMISSION_AGENT;
    public static final int AgentTypeAnother = IFptr.LIBFPTR_AT_ANOTHER;
    
    public static final int PaymentTypeCredit = 6;
    public static final int PaymentTypePrepaidFully = 1;
    public static final int PaymentTypePrepaid = 2;
    public static final int PaymentTypeAdvance = 3;
    public static final int PaymentTypeFullPayment = 4;
    public static final int PaymentTypePartialCredit = 5;
    
    public static final int ItemDefault = 0;
    public static final int ItemCommodity = 1;
    public static final int ItemExciseCommodity = 2;
    public static final int ItemJob = 3;
    public static final int ItemService = 4;
    public static final int ItemGamblingBet = 5;
    public static final int ItemGamblingWinnings = 6;
    public static final int ItemLotteryTicket = 7;
    public static final int ItemLotteryWinnings = 8;
    public static final int ItemRentingIP = 9;
    public static final int ItemPayment = 10;
    public static final int ItemAgentCommission = 11;
    public static final int ItemCompound = 12;
    public static final int ItemOther = 13;
    public static final int ItemPropertyRight = 14;
    public static final int ItemNonSaleIncome = 15;
    public static final int ItemInsuranceFee = 16;
    public static final int ItemTradeFee = 17;
    public static final int ItemResortFee = 18;
    
    public static final int ReceiptClosed = IFptr.LIBFPTR_RT_CLOSED;
    public static final int ReceiptSell = IFptr.LIBFPTR_RT_SELL;
    public static final int ReceiptSellReturn = IFptr.LIBFPTR_RT_SELL_RETURN;
    public static final int ReceiptSellCorrection = IFptr.LIBFPTR_RT_SELL_CORRECTION;
    public static final int ReceiptBuy = IFptr.LIBFPTR_RT_BUY;
    public static final int ReceiptBuyReturn = IFptr.LIBFPTR_RT_BUY_RETURN;
    public static final int ReceiptBuyCorrection= IFptr.LIBFPTR_RT_BUY_CORRECTION;
    public static final int ReceiptSellReturnCorrection = IFptr.LIBFPTR_RT_SELL_RETURN_CORRECTION;
    public static final int ReceiptBuyReturnCorrection = IFptr.LIBFPTR_RT_BUY_RETURN_CORRECTION;
    
    public static final int CorrectionReasonAutonomously = 0;
    public static final int CorrectionReasonByRequirement = 1;
    
    public static final int ShiftStateClosed= IFptr.LIBFPTR_SS_CLOSED; /**Shift is Closed */
    public static final int ShiftStateOpen = IFptr.LIBFPTR_SS_OPENED; /**Shift is Open */
    public static final int ShiftStateExpired = IFptr.LIBFPTR_SS_EXPIRED;/**Shift was open for 24 hours and got tired */
    
    public static final int TaxTypeDepartment = IFptr.LIBFPTR_TAX_DEPARTMENT; /**Tax type that is dictated by the item's department */
    public static final int TaxTypeVAT18 = IFptr.LIBFPTR_TAX_VAT18; /**Value Addded Tax 18% */
    public static final int TaxTypeVAT10 = IFptr.LIBFPTR_TAX_VAT0; /**Value Added Tax 10% */
    public static final int TaxTypeVAT118 = IFptr.LIBFPTR_TAX_VAT118; /**Value Added Tax calculated by 18/118 */
    public static final int TaxTypeVAT110 = IFptr.LIBFPTR_TAX_VAT110; /**Value Added Tax calculated by 10/110 */
    public static final int TaxTypeVAT0 = IFptr.LIBFPTR_TAX_VAT0; /**Value Added Tax 0% */
    public static final int TaxTypeNoTax = IFptr.LIBFPTR_TAX_NO;/**No Tax Rate */
    public static final int TaxTypeVAT20 = IFptr.LIBFPTR_TAX_VAT20; /**Value Addded Tax 20% */
    public static final int TaxTypeVAT120 = IFptr.LIBFPTR_TAX_VAT120; /**Vaule Added Tax calculated by 20/120 */
    
    /** Private Constructor for Atolin.
     * Enforces non instantiability of this class. 
     * Throws an Assertion Error if somehow called. 
     */
    private Atolin()
    {
        throw new AssertionError();
    }
    
    /**
     * A function, that can be used to establish connection to a local Server from a client side.
     * It is recommended that you use this function instead of establishing connection manually, unless
     * you like wasting space.
     * @param registryAdress adress of the Registry.
     * @param port Registry Port.
     * @param DeviceName Name of the Device defined by the Server.
     * @return iDevice, an interface to a remote Device, located on a local machine.
     */
    public static iDevice getConnectionLocal(String registryAdress, int port, String DeviceName) throws RemoteException, NotBoundException
    {
        Registry reg = LocateRegistry.getRegistry(registryAdress, port);
            
        System.out.println(String.join("; ", reg.list()));
        return (iDevice) reg.lookup(DeviceName);       
    }
    /**
     * A function, that can be used to establish connection to a local Server from a client side.
     * It is recommended that you use this function instead of establishing connection manually, unless
     * you like wasting space.
     * This function uses default settings of the server, should be able to connect to a local server running
     * with default Settings
     */
    public static iDevice getConnectionDefault() throws RemoteException, NotBoundException
    {
        Registry reg = LocateRegistry.getRegistry(1099);
        return (iDevice) reg.lookup(iDevice.NAME);
    }
}

/*
catch(Exception e)
{
    System.out.println("Error: Failed to Connect to a Server with Follwing Properties: "
        + "\n\t>Registry Adress: "+registryAdress+"\n\t>port:"+String.valueOf(port)
        + "\n\t>Device Name: " + DeviceName);
        return null;
}
*/