package atolinserver5;

import atolincore5.Atolin;
import atolincore5.Item;
import atolincore5.Receipt;
import atolincore5.iDevice;
import java.io.StringReader;
import java.rmi.RemoteException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import ru.atol.drivers10.fptr.Fptr;
import ru.atol.drivers10.fptr.IFptr;


public class Device implements iDevice {
    private final Fptr device = new Fptr();
    
    private int operatorID;
    private int modelID;
    private int mode;
    private int submode;
    private int receiptNumber;
    private int documentNumber;
    private int shiftNumber;
    private int shiftState;
    private int lineLength;
    private int lineLengthInPixels;
    private double currentReceiptSum;
    private int currentReceeptType;
    private String modelName;
    private String firmwareVersion;
    private boolean FMDisPresent;
    private boolean FMDisFiscal;
    private boolean cashDrawerIsOpen;
    private boolean hasPaper;
    private boolean isFiscal;
    private boolean isBlocked;
    private boolean coverIsOpen;
    private boolean printerConnectionIsLost;
    
    Device() throws RemoteException {
    }
    
    public void loadDevice() {
      String settings = String.format("{\"%s\": %d, \"%s\": %d}",
              IFptr.LIBFPTR_SETTING_MODEL, IFptr.LIBFPTR_MODEL_ATOL_AUTO,
              IFptr.LIBFPTR_SETTING_PORT, IFptr.LIBFPTR_PORT_USB);
      loadCustomDevice(settings);
    }
    
    @Override
    public void loadCustomDevice(String settings) {
      if(device.isOpened())
        device.close();
      device.setSettings(settings);
      device.open();
    }
    
    @Override
    public void cancelReceipt() {
      if(!device.isOpened())
        loadDevice();
      device.cancelReceipt();
    }
    
    private void init() {
      if(!device.isOpened())
        loadDevice();
    }
    
    @Override
    public void queryInfo() {
      init();

      device.setParam(IFptr.LIBFPTR_PARAM_DATA_TYPE, IFptr.LIBFPTR_DT_STATUS);
      device.queryData();

      operatorID = (int) device.getParamInt(IFptr.LIBFPTR_PARAM_OPERATOR_ID);
      modelID = (int) device.getParamInt(IFptr.LIBFPTR_PARAM_MODEL);
      mode = (int) device.getParamInt(IFptr.LIBFPTR_PARAM_MODE);
      submode = (int) device.getParamInt(IFptr.LIBFPTR_PARAM_SUBMODE);
      receiptNumber = (int) device.getParamInt(IFptr.LIBFPTR_PARAM_RECEIPT_NUMBER);
      documentNumber = (int) device.getParamInt(IFptr.LIBFPTR_PARAM_DOCUMENT_NUMBER);
      shiftNumber = (int) device.getParamInt(IFptr.LIBFPTR_PARAM_SHIFT_NUMBER);
      shiftState = (int) device.getParamInt(IFptr.LIBFPTR_PARAM_SHIFT_STATE);
      lineLength = (int) device.getParamInt(IFptr.LIBFPTR_PARAM_RECEIPT_LINE_LENGTH);
      lineLengthInPixels = (int) device.getParamInt(IFptr.LIBFPTR_PARAM_RECEIPT_LINE_LENGTH_PIX);
      currentReceiptSum = device.getParamDouble(IFptr.LIBFPTR_PARAM_RECEIPT_SUM);
      modelName = device.getParamString(IFptr.LIBFPTR_PARAM_MODEL_NAME);
      firmwareVersion = device.getParamString(IFptr.LIBFPTR_PARAM_UNIT_VERSION);
      FMDisPresent = device.getParamBool(IFptr.LIBFPTR_PARAM_FN_PRESENT);
      FMDisFiscal = device.getParamBool(IFptr.LIBFPTR_PARAM_FN_FISCAL);
      cashDrawerIsOpen = device.getParamBool(IFptr.LIBFPTR_PARAM_CASHDRAWER_OPENED);
      hasPaper = device.getParamBool(IFptr.LIBFPTR_PARAM_RECEIPT_PAPER_PRESENT);
      isFiscal = device.getParamBool(IFptr.LIBFPTR_PARAM_FISCAL);
      isBlocked = device.getParamBool(IFptr.LIBFPTR_PARAM_BLOCKED);
      coverIsOpen = device.getParamBool(IFptr.LIBFPTR_PARAM_COVER_OPENED);
      printerConnectionIsLost =  device.getParamBool(IFptr.LIBFPTR_PARAM_PRINTER_CONNECTION_LOST);
    }
    
    private int runRegular(Receipt receipt) {
      device.openReceipt();
      registerItems(receipt);
      return device.closeReceipt();
    }
    
    private int runCorrection(Receipt receipt) {
      init();
      device.setParam(1177, receipt.getCorrectionComment());
      device.setParam(1178, receipt.correctionDate.getTime());
      device.setParam(1179, receipt.getCorrectionDocumentNumber());
      device.utilFormTlv();
      byte[] correctionInfo = device.getParamByteArray(IFptr.LIBFPTR_PARAM_TAG_VALUE);

      device.setParam(IFptr.LIBFPTR_PARAM_RECEIPT_TYPE, receipt.getType());
      device.setParam(1173, receipt.getCorrectionReason());
      device.setParam(1174, correctionInfo);
      device.openReceipt();
      registerItems(receipt);
      return device.closeReceipt();
    }
    
    
    private void registerItems(Receipt receipt) {
      for(Item item:receipt.getItems()) {
        device.setParam(IFptr.LIBFPTR_PARAM_COMMODITY_NAME, item.getName());
        device.setParam(IFptr.LIBFPTR_PARAM_PRICE         , item.getPrice().doubleValue());
        device.setParam(IFptr.LIBFPTR_PARAM_QUANTITY      , item.getQuantity().doubleValue());
        device.setParam(IFptr.LIBFPTR_PARAM_TAX_TYPE      , item.getTaxType());
        
        device.setParam(1214, item.getPaymentType());
        
        //Note grammar. word "only" implies a colomn, so the MACRO reads: "use only, tax type", 
        //which has different meaning than intended.
        device.setParam(IFptr.LIBFPTR_PARAM_USE_ONLY_TAX_TYPE, item.getUseTaxTypeOnly());
        device.setParam(IFptr.LIBFPTR_PARAM_COMMODITY_PIECE, item.getCommodityIsPiece());
        device.setParam(IFptr.LIBFPTR_PARAM_CHECK_SUM, item.getCheckCash());
        
       
        if(item.getPositionSum() != null)
            device.setParam(IFptr.LIBFPTR_PARAM_POSITION_SUM, item.getPositionSum().doubleValue());
        if(item.getTaxSum()!= null)
            device.setParam(IFptr.LIBFPTR_PARAM_TAX_SUM, item.getTaxSum().doubleValue());
             if(!item.getDepartment().isEmpty())
            device.setParam(IFptr.LIBFPTR_PARAM_DEPARTMENT, item.getDepartment());
        if(item.getTaxMode() != Atolin.TaxModeDefault)
            device.setParam(IFptr.LIBFPTR_PARAM_TAX_MODE, item.getTaxMode());
        if(!item.getDiscountInfo().isEmpty())
            device.setParam(IFptr.LIBFPTR_PARAM_INFO_DISCOUNT_SUM, item.getDiscountInfo());
        if(!item.getUnits().isEmpty())
            device.setParam(1197, item.getUnits());
        //??? i Have no idea what PaymentItem is and it's not mentioned in documentation
        if(item.getPaymentItem() != 0)
            device.setParam(1212, item.getPaymentItem());
        
        if(item.getAgentType() != Atolin.AgentTypeNone)
        {
            device.setParam(1005, item.getOperatorAdress());
            device.setParam(1016, item.getOperatorTaxID());
            device.setParam(1026, item.getOperatorName());
            device.setParam(1044, item.getAgentOperation());
            device.setParam(1073, item.getAgentPhoneNumber());  
            device.setParam(1074, item.getRecievingOperatorPhoneNumber());
            device.setParam(1075, item.getTransactionOperatorPhoneNumber());
            device.utilFormTlv();
            device.setParam(1223, device.getParamByteArray(IFptr.LIBFPTR_PARAM_TAG_VALUE));
        }
        
        if(!item.getSupplierName().isEmpty())
        {
            device.setParam(1171, item.getSupplierPhoneNumber());
            device.setParam(1225, item.getSupplierName());
            device.utilFormTlv();
            device.setParam(1224, device.getParamByteArray(IFptr.LIBFPTR_PARAM_TAG_VALUE));
            
            device.setParam(1226, item.getSupplierTaxID());
        }
        device.registration();
      }
    }
    
    @Override
    public int printReceipt(String XMLreceipt) {
      if(XMLreceipt == null || XMLreceipt.isEmpty())
      {
        if(snatch != null)
            snatch.snatch("Ошибка: Чек пуст или передан null", Snatcher.MESSAGE_TYPE_ERROR); 
        throw new NullPointerException("Expected XMLReceipt, got a null or String is empty");
      }
      init();
      try {
        JAXBContext context = JAXBContext.newInstance(Receipt.class);
        Unmarshaller um = context.createUnmarshaller();
        StringReader reader = new StringReader(XMLreceipt);
        Receipt receipt = (Receipt) um.unmarshal(reader);

        device.setParam(IFptr.LIBFPTR_PARAM_RECEIPT_TYPE, receipt.getType());

        if(receipt.getType() == Atolin.ReceiptBuyCorrection || receipt.getType() == Atolin.ReceiptSellCorrection)
            return runCorrection(receipt);
        else if(receipt.getType() == Atolin.ReceiptBuy || receipt.getType() == Atolin.ReceiptSell 
                || receipt.getType() == Atolin.ReceiptBuyReturn || receipt.getType() == Atolin.ReceiptSellReturn)
            return runRegular(receipt);
        else if(snatch != null)
        {
            if(receipt.getType() == Atolin.ReceiptBuyReturnCorrection)
                snatch.snatch("Ошибка: Чек Коррекции Возврата Расхода не поддерживается, выберете другой тип чека.", Snatcher.MESSAGE_TYPE_ERROR);
            else if(receipt.getType() == Atolin.ReceiptSellReturnCorrection)
                snatch.snatch("Ошибка: Чек Коррекции Возврата Прихода не поддерживается, выберете другой тип чека.", Snatcher.MESSAGE_TYPE_ERROR);
            else if(receipt.getType() == Atolin.ReceiptClosed)
                snatch.snatch("Ошибка: Закрытый чек нельзя Печатать/Регестрировать ФН, выберете другой тип чека.", Snatcher.MESSAGE_TYPE_ERROR);
            else
                snatch.snatch("Ошибка: Некоректный тип чека.", mode);
        }
        throw new Exception("Incorrect Receipt Type");
      }
      catch(Exception e) {
        if(snatch != null)
            snatch.snatch("Ошибка: Провал Маршализации/Анмаршализации чека.", mode);
        e.printStackTrace();
        return -80085;
      }
    }
    
    @Override
    public void logIn(String login, String password) {
      init();
      device.setParam(1021, login);
      device.setParam(1203, password);
      device.operatorLogin();
    }
    
    @Override
    public void openShift() {
      init();
      device.openShift();
      device.checkDocumentClosed();
    }
    
    @Override
    public void closeShift() {
      init();
      device.setParam(IFptr.LIBFPTR_PARAM_REPORT_TYPE, IFptr.LIBFPTR_RT_CLOSE_SHIFT);
      device.report();
      device.checkDocumentClosed();
    }
    
    @Override
    public void cut() {
      init();
      device.cut();
    }

    public void cleanCut()
    {
        for(int i = 0; i < 6; i++)
            printCustomString("");
        cut(); 
    }
    
    public void cutPartial()
    {
        init();
        device.setParam(IFptr.LIBFPTR_PARAM_CUT_TYPE, IFptr.LIBFPTR_CT_PART);
        device.cut();
    }
    
    public void cleanCutPartial()
    {
        for(int i = 0; i < 6; i++)
            printCustomString("");
        cut();
    }
    
    public void setCutPartial()
    {
        init();
        device.setParam(IFptr.LIBFPTR_PARAM_CUT_TYPE, IFptr.LIBFPTR_CT_PART);
    }
    
    public void setCutFull()
    {
        init();
        device.setParam(IFptr.LIBFPTR_PARAM_CUT_TYPE, IFptr.LIBFPTR_CT_FULL);
    }
    
    @Override
    public void printCustomString(String str) {
      init();
      device.setParam(IFptr.LIBFPTR_PARAM_TEXT, str);
      device.printText();
    }
        
    @Override
    public void beep() {
      init();
      device.beep();
    }
        
    @Override
    public int getOperatorID() {
      return operatorID;
    }

    @Override
    public int getModelID()//returns model ID declared by the manufacturer
    {
        return modelID;
    }
    
    @Override
    public int getMode()//TODO: Find out what thsi function ACUALLY returns.
    {
        return mode;
    }
    
    @Override
    public int getSubmode()//TODO: look at getMode.
    {
        return submode;
    }
    
    @Override
    public int getReceiptNumber()
    {
        return receiptNumber;
    }
    
    @Override
    public int getDocumentNumber()
    {
        return documentNumber;
    }
    
    @Override
    public int getShiftNumber()
    {
        return shiftNumber;
    }
    
    @Override
    public int getShiftState()
    {
        return shiftState;
    }
     
    @Override
    public int getLineLength()
    {
        return lineLength;
    }
    
    @Override
    public int getLineLengthInPixels()
    {
        return lineLengthInPixels;
    }
    
    //int GetLogicalNumber(); //TODO: Find out what "Logical Number" is and figure out a better name for whatever it is.
    
    @Override
    public double getCurrentReceiptSum()
    {
        return currentReceiptSum;
    }
    
    @Override
    public int getCurrentReceiptType()
    {
        return currentReceeptType;
    }
    
    @Override
    public String getModelName() //returns model name that can be understood by humans;
    {
        String buffer = modelName;
        return modelName;
    }
            
    @Override
    public String getFirmwareVersion()
    {
        String buffer = firmwareVersion;
        return buffer;
    }
    
    @Override
    public boolean FMDIsPresent()//FMD Stands for Fiscal Memory Device
    {
        return FMDisPresent;
    }
    
    @Override
    public boolean FMDIsFiscal()//FMD Stands for Fiscal Memory Device
    {
        return FMDisFiscal;
    }
    
    @Override
    public boolean cashdrawerIsOpen()
    {
        return cashDrawerIsOpen;
    }
    
    @Override
    public boolean hasPaper()
    {
        return hasPaper;
    }
    
    @Override
    public boolean isFiscal()
    {
        return isFiscal;
    }
    
    @Override
    public boolean isBlocked()
    {
        return isBlocked;
    }
    
    @Override
    public boolean coverIsOpen()
    {
        return coverIsOpen;
    }
    
    @Override
    public boolean printerConnectionIsLost()
    {
        return printerConnectionIsLost;
    }
    
    @Override
    public void xReport() {
      init();
      device.setParam(IFptr.LIBFPTR_PARAM_REPORT_TYPE, IFptr.LIBFPTR_RT_X);
      device.report();
    }
    
    @Override
    public void enableErrorSnatcher()
    {
        snatch = new Snatcher();
    }
    
    @Override
    public void squelchErrorSnatcher()
    {
        snatch = null;
    }
    
    private class Snatcher
    {
        public final static int MESSAGE_TYPE_ERROR = 0;
        public final static int MESSAGE_TYPE_WARNING = 1;
        public final static int MESSAGE_TYPE_MESSAGE = 2;
        
        public Snatcher() {}
        
        public void snatch(String message, int type)
        {
            queryInfo(); 
            String stars = "";
            for(int i = 0; i < lineLength; i++)
                stars += '*';
            
            printCustomString(stars);
            printCustomString("");
            if(type == MESSAGE_TYPE_ERROR)
            {
                String errorStars = "";
                for(int i = 0; i < (lineLength-6)/2; i++)
                    errorStars += '*';
                printCustomString(errorStars +"ERROR!" + errorStars);
            }
            else if (type == MESSAGE_TYPE_WARNING)
            {
                String warningStars = "";
                for(int i = 0; i < (lineLength-8)/2; i++)
                    warningStars += '*';
                printCustomString(warningStars+ "WARNING!" + warningStars);
                
            }
            else if (type == MESSAGE_TYPE_MESSAGE)
            {
                String warningStars = "";
                for(int i = 0; i < (lineLength-8)/2; i++)
                    warningStars += '*';
                printCustomString(warningStars+ "MESSAGE!" + warningStars);
            }
            
            printCustomString(message);
            printCustomString("");
            printCustomString(stars);
            printCustomString("");
            printCustomString(stars);
            cleanCutPartial();
        }
    }
    Snatcher snatch = null;
}
