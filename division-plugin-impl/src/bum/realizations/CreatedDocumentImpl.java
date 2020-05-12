package bum.realizations;

import bum.annotations.*;
import bum.interfaces.*;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;

@Table(clientName="Документы",
        queryColumns={
          @QueryColumn(
            name="seller-nds",
            desctiption="Агент - плательщик НДС",
            query="(SELECT [Company(ndsPayer)] FROM [Company] WHERE id=[CreatedDocument(seller)])"),
          @QueryColumn(
            name="seller-inn",
            desctiption="инн агента",
            query="(SELECT [Company(inn)] FROM [Company] WHERE id=[CreatedDocument(seller)])"),
          @QueryColumn(
            name="seller-book-keeper",
            desctiption="бухгалтер агента",
            query="(SELECT [Company(bookkeeper)] FROM [Company] WHERE id=[CreatedDocument(seller)])"),
          @QueryColumn(
            name="customer-inn",
            desctiption="инн контрагента",
            query="(SELECT [Company(inn)] FROM [Company] WHERE id=[CreatedDocument(customer)])"),
          @QueryColumn(
            name="seller-name",
            desctiption="наименование агента",
            query="getCompanyPartitionName([CreatedDocument(sellerCompanyPartition)])"),
          @QueryColumn(
            name="customer-name",
            desctiption="наименование контрагента",
            query="getCompanyPartitionName([CreatedDocument(customerCompanyPartition)])"),
          @QueryColumn(
            name="seller-bank-bik",
            desctiption="Бик банка продавца",
            query="(SELECT [Account(bik)] FROM [Account] WHERE [Account(current)]=true AND [Account(companyPartition)]=[CreatedDocument(sellerCompanyPartition)])"),
          @QueryColumn(
            name="seller-account-number",
            desctiption="Расчётный счёт продавца",
            query="(SELECT [Account(number)] FROM [Account] WHERE [Account(current)]=true AND [Account(companyPartition)]=[CreatedDocument(sellerCompanyPartition)])"),
          @QueryColumn(
            name="link-documents",
            desctiption="Связанные документы",
            query="SELECT getLinkDocuments([CreatedDocument(id)])"),
          @QueryColumn(
            name = "seller-template",
            query = "ARRAY(SELECT id FROM [DocumentXMLTemplate] WHERE tmp=false AND [DocumentXMLTemplate(document)]=[CreatedDocument(document)] AND "
                    + "[DocumentXMLTemplate(companyPartition)]=[CreatedDocument(sellerCompanyPartition)] ORDER BY [DocumentXMLTemplate(main)] DESC)"
          ),
          
          @QueryColumn(
            name = "customer-template",
            query = "ARRAY(SELECT id FROM [DocumentXMLTemplate] WHERE tmp=false AND [DocumentXMLTemplate(document)]=[CreatedDocument(document)] AND "
                    + "[DocumentXMLTemplate(companyPartition)]=[CreatedDocument(customerCompanyPartition)] ORDER BY [DocumentXMLTemplate(main)] DESC)"
          ),
          @QueryColumn(
            name = "template",
            query = "ARRAY(SELECT id FROM [DocumentXMLTemplate] WHERE tmp=false AND [DocumentXMLTemplate(document)]=[CreatedDocument(document)] AND "
                    + "[DocumentXMLTemplate(companyPartition)] ISNULL ORDER BY [DocumentXMLTemplate(main)] DESC)"
          )
        })

//Последний день месяца
//SELECT date_trunc('month', CURRENT_TIMESTAMP) + interval '1 month' - interval '1 milliseconds'
@Procedures(
  procedures={
    @Procedure(
        name="getLinkDocuments",
        arguments={"integer"},
        returnType="TEXT[][]",
        procedureText=""
                + "DECLARE\n"
                + "  arr        TEXT[][];\n"
                + "  dps        integer[];\n"
                + "  payment_id integer;\n"
                + "  docs       record;\n"
                + "BEGIN\n\n"
                
                + "SELECT array_agg([CreatedDocument(dealPositions):target]) INTO dps FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):object]=$1;\n"
                + "SELECT [CreatedDocument(payment)] INTO payment_id FROM [CreatedDocument] WHERE id=$1;\n"
                + "IF payment_id IS NOT NULL THEN\n"
                + "  SELECT dps || array_agg([DealPosition(id)]) INTO dps FROM [DealPosition] WHERE [DealPosition(deal)] IN (SELECT [DealPayment(deal)] FROM [DealPayment] WHERE [DealPayment(payment)]=payment_id);\n"
                + "END IF;\n\n"
                
                + "FOR docs IN SELECT DISTINCT \n"
                + " [CreatedDocument(id)], \n"
                + " [CreatedDocument(document_name)] AS d_name, \n"
                + " [CreatedDocument(number)], \n"
                + " [CreatedDocument(date)], \n"
                + " [CreatedDocument(payment)] AS p_id, \n"
                + " getPayAmount(payment_id,[CreatedDocument(id)]) AS p_amount \n"
                + "FROM [CreatedDocument] \n"
                + "WHERE \n"
                + " [CreatedDocument(document-system)] = true AND \n"
                + " tmp = false AND \n"
                + " type = 'CURRENT' AND \n"
                + " [CreatedDocument(stornoDate)] IS NULL AND \n"
                + " [CreatedDocument(id)] != $1 AND \n"
                + " [CreatedDocument(id)] IN \n"
                + " (\n"
                + "  SELECT [CreatedDocument(dealPositions):object] \n"
                + "  FROM [CreatedDocument(dealPositions):table] \n"
                + "  WHERE [CreatedDocument(dealPositions):target] = ANY(dps)\n"
                + " ) LOOP\n\n"
                
                + "   SELECT arr || ARRAY[[docs.d_name::text,docs.number::text,docs.date::text,docs.p_id::text,docs.p_amount::text]] INTO arr;\n\n"
                
                + " END LOOP;\n"
                /*
                + "FOR docs IN SELECT DISTINCT [CreatedDocument(id)], [CreatedDocument(document_name)] AS doc_name, [CreatedDocument(number)], "
                + "[CreatedDocument(date)], [CreatedDocument(payment)] AS payment_id, getPayAmount($1,[CreatedDocument(id)]) AS payAmount \n"
                + "FROM [CreatedDocument] WHERE [CreatedDocument(stornoDate)] IS NULL AND [CreatedDocument(number)] NOTNULL AND tmp=false AND type='CURRENT' AND [CreatedDocument(id)]!=$1 AND \n"
                + "([CreatedDocument(id)] IN (SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):target] IN \n"
                + "(SELECT [CreatedDocument(dealPositions):target] FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):object]=$1)) OR \n"
                + "[CreatedDocument(payment)] IN (SELECT [DealPayment(payment)] FROM [DealPayment] WHERE [DealPayment(deal)] IN\n"
                + "(SELECT [DealPosition(deal)] FROM [DealPosition] WHERE [DealPosition(id)] IN \n"
                + "(SELECT [CreatedDocument(dealPositions):target] FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):object]=$1))) OR \n"
                + "[CreatedDocument(payment)]=(SELECT [CreatedDocument(payment)] FROM [CreatedDocument] WHERE [CreatedDocument(id)]=$1) OR \n"
                + "[CreatedDocument(id)] IN (SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] \n"
                + "WHERE [CreatedDocument(dealPositions):target] IN (SELECT [DealPosition(id)] FROM [DealPosition] WHERE [DealPosition(deal)] IN \n"
                + "(SELECT [DealPayment(deal)] FROM [DealPayment] WHERE [DealPayment(payment)]=(SELECT [CreatedDocument(payment)] "
                + "FROM [CreatedDocument] WHERE [CreatedDocument(id)]=$1))))) LOOP\n"
                + "    SELECT arr || ARRAY[[docs.doc_name::text,docs.number::text,docs.date::text,docs.payment_id::text,docs.payAmount::text]] INTO arr;\n"
                + "  END LOOP;\n"
                */
                + "RETURN arr;\n"
                + "END\n"
    ),
    @Procedure(
        name="getPayAmount",
        arguments={"integer","integer"},
        returnType="FLOAT",
        procedureText=""
                + "DECLARE\n"
                + "  payment_id integer := $1;\n"
                + "  doc     integer := $2;\n"
                + "BEGIN"
                + "  IF payment_id IS NULL THEN"
                + "    RETURN 0;"
                + "  END IF;"
                + "  RETURN (SELECT SUM([DealPayment(amount)]) FROM [DealPayment] WHERE tmp=false AND type='CURRENT' AND"
                + "    [DealPayment(payment)]=payment_id AND"
                + "    [DealPayment(deal)] IN (SELECT [DealPosition(deal)] FROM [DealPosition] WHERE tmp=false AND type='CURRENT' AND "
                + "      [DealPosition(id)] IN (SELECT [CreatedDocument(dealPositions):target] FROM [CreatedDocument(dealPositions):table] WHERE"
                + "        [CreatedDocument(dealPositions):object]=doc)));\n"
                + "END\n"
        ),
    @Procedure(
      name="createDocument",
      arguments={"integer","integer[]","text","integer","text","timestamp"},
      returnType="integer",
      procedureText= ""
        + "DECLARE \n"
        + "  createdDocId           integer;\n"
        + "  newNumber              text;\n"
        + "  previosNumber          integer;\n"
        + "  previosDate            timestamp;\n"
        + "  sellerpartition        record;\n"
        + "  customerpartition      record;\n"
        + "  partitionDocument      record;\n"
        + "  numbers                record;\n"
        + "  prefix                 text;\n"
        + "  number                 integer;\n"
        + "  suffix                 text;\n"
        + "  documentId             integer   := $1;\n"
        + "  dealPositions          integer[] := $2;\n"
        + "  actionType             text      := $3;\n"
        + "  paymentId              integer   := $4;\n"
        + "  documentNumber         text      := $5;\n"
        + "  documentDate           timestamp := $6;\n"
        + "  typeFormat             text;\n"
        + "  documentData           record;\n"
        + "  itemCount              integer;\n"
        + "  partition              integer;\n"
        + "BEGIN \n"
              
        + "  IF documentDate IS NULL THEN documentDate = CURRENT_TIMESTAMP; END IF;\n"
              
        + "  IF paymentId IS NULL THEN \n"
        + "    /*Получаем агента документа*/\n"
        + "    SELECT * INTO sellerpartition FROM [CompanyPartition] WHERE id=(SELECT [DealPosition(seller_partition_id)] FROM [DealPosition] WHERE id=dealPositions[1]);\n"
        + "    /*Получаем контрагента документа*/\n"
        + "    SELECT * INTO customerpartition FROM [CompanyPartition] \n"
        + "      WHERE id=(SELECT id FROM [CompanyPartition] WHERE id in (SELECT [DealPosition(customer_partition_id)] FROM [DealPosition] WHERE id=ANY(dealPositions)) ORDER BY [CompanyPartition(mainPartition)] desc limit 1);\n"
        + "  ELSE\n"
        + "    --Получаем агента документа\n"
        + "    SELECT * INTO sellerpartition FROM [CompanyPartition] WHERE id=(SELECT [Payment(sellerCompanyPartition)] FROM [Payment] WHERE id=paymentId);\n"
        + "    --Получаем контрагента документа\n"
        + "    SELECT * INTO customerpartition FROM [CompanyPartition] WHERE id=(SELECT [Payment(customerCompanyPartition)] FROM [Payment] WHERE id=paymentId);\n"
        + "  END IF;\n"
              
        + "  IF documentNumber IS NULL THEN\n"
              
        + "  SELECT * INTO documentData FROM [Document] WHERE [Document(id)]=documentId;\n"
        
        + "  /*Получаем правила нумерации*/\n"
        + "  IF documentData.documentSource = 'SELLER' THEN\n"
        + "    partition := sellerpartition.id;\n"
        + "    IF sellerpartition.mainnumbering = true AND sellerpartition.mainPartition = false THEN\n"
        + "      SELECT id INTO partition FROM [CompanyPartition] WHERE [CompanyPartition(company)]=sellerpartition.company_company_id AND mainPartition=true;"      
        + "    END IF;\n"
        + "    SELECT * INTO partitionDocument FROM [CompanyPartitionDocument] WHERE tmp=false AND type='CURRENT' AND \n"
        + "      [CompanyPartitionDocument(document)]=documentId AND [CompanyPartitionDocument(partition)]=partition;\n"
        + "  ELSE\n"
        + "    partition := customerpartition.id;\n"
        + "    IF customerpartition.mainnumbering = true AND customerpartition.mainPartition = false THEN\n"
        + "      SELECT id INTO partition FROM [CompanyPartition] WHERE [CompanyPartition(company)]=sellerpartition.company_company_id AND mainPartition=true;"      
        + "    END IF;\n"
        + "    SELECT * INTO partitionDocument FROM [CompanyPartitionDocument] WHERE tmp=false AND type='CURRENT' AND \n"
        + "      [CompanyPartitionDocument(document)]=documentId AND [CompanyPartitionDocument(partition)]=partition;\n"
        + "  END IF;\n"
        
        + "  IF partitionDocument IS NULL THEN\n"
        
        + "    INSERT INTO [CreatedDocument] ([!CreatedDocument(document)],\n"
        + "      [!CreatedDocument(sellerCompanyPartition)],[!CreatedDocument(customerCompanyPartition)],\n"
        + "      [!CreatedDocument(actionType)],[!CreatedDocument(date)],[!CreatedDocument(payment)]) \n"
        + "      VALUES (documentId,sellerpartition.id,customerpartition.id,actionType,documentDate,paymentId);\n"
        + "    SELECT MAX(id) INTO createdDocId FROM [CreatedDocument];\n"
        
        + "  ELSE"
        
        + "    previosNumber := 0;\n"
        + "    IF partitionDocument.periodForZero = 'год' OR partitionDocument.periodForZero = 'P1Y' THEN \n"
        + "      IF partitionDocument.grabFreeNumber = false THEN \n"
        + "        SELECT MAX([CreatedDocument(intNumber)]) INTO number FROM [CreatedDocument] WHERE tmp=false AND type='CURRENT' AND [CreatedDocument(stornoDate)] IS NULL AND \n"
        + "        [CreatedDocument(sellerCompanyPartition)]=partition AND [CreatedDocument(document)]=documentId \n"
        + "         AND date BETWEEN date_trunc('year', documentDate) AND date_trunc('year', documentDate)+ interval '1 year' - interval '1 milliseconds';\n"
        + "      ELSE \n"
        + "        /*Цикл по отсортированным документам этого типа, принадлежащих данному владельцу в рамках заданного периода*/\n"
        + "        FOR numbers IN SELECT [CreatedDocument(intNumber)],[CreatedDocument(date)],[CreatedDocument(number)] FROM [CreatedDocument] WHERE \n"
        + "          [CreatedDocument(sellerCompanyPartition)]=partition AND [CreatedDocument(document)]=documentId AND tmp=false AND type='CURRENT' \n"
        + "          AND [CreatedDocument(stornoDate)] IS NULL AND date BETWEEN date_trunc('year', documentDate) AND \n"
        + "          date_trunc('year', documentDate)+ interval '1 year' - interval '1 milliseconds' \n"
        + "          ORDER BY [CreatedDocument(intNumber)] LOOP \n"
        + "            /*Если имеется пропуск номера, то занимаем его, если дата совпадает с текущей*/\n"
        + "            IF previosNumber != 0 AND \n"
        + "                numbers.[!CreatedDocument(intNumber)] - previosNumber > 1 AND \n"
        + "                numbers.[!CreatedDocument(date)]::date = documentDate::date THEN \n"
        + "              number  := previosNumber;\n"
        + "              --newDate := numbers.[!CreatedDocument(date)];\n"
        + "            END IF;\n"
        + "            previosNumber := numbers.[!CreatedDocument(intNumber)];\n"
        + "            previosDate   := numbers.[!CreatedDocument(date)];\n"
        + "          EXIT WHEN number IS NOT NULL;"
        + "        END LOOP;\n"
        + "      END IF;\n"
        + "    END IF;\n"
        
        + "    IF partitionDocument.periodForZero = 'месяц' OR partitionDocument.periodForZero = 'P1M' THEN \n"
        + "      IF partitionDocument.grabFreeNumber = false THEN \n"
        + "        SELECT MAX([CreatedDocument(intNumber)]) INTO number FROM [CreatedDocument] WHERE tmp=false AND type='CURRENT' AND [CreatedDocument(stornoDate)] IS NULL AND \n"
        + "        [CreatedDocument(sellerCompanyPartition)]=partition AND [CreatedDocument(document)]=documentId \n"
        + "        AND date BETWEEN date_trunc('month', documentDate) AND date_trunc('month', documentDate)+ interval '1 month' - interval '1 milliseconds';\n"
        + "      ELSE \n"
        + "        /*Цикл по отсортированным документам этого типа, принадлежащих данному владельцу в рамках заданного периода*/\n"
        + "        FOR numbers IN SELECT [CreatedDocument(intNumber)],[CreatedDocument(date)],[CreatedDocument(number)] FROM [CreatedDocument] WHERE \n"
        + "            [CreatedDocument(sellerCompanyPartition)]=partition AND [CreatedDocument(document)]=documentId AND tmp=false AND type='CURRENT' \n"
        + "            AND [CreatedDocument(stornoDate)] IS NULL AND date BETWEEN date_trunc('month', documentDate) AND \n"
        + "            date_trunc('month', documentDate)+ interval '1 month' - interval '1 milliseconds' \n"
        + "            ORDER BY [CreatedDocument(intNumber)] LOOP \n"
        + "          /*Если имеется пропуск номера, то занимаем его, если месяц совпадает с текущим*/\n"
        + "          IF previosNumber != 0 AND \n"
        + "              numbers.[!CreatedDocument(intNumber)] - previosNumber > 1 AND \n"
        + "              extract(YEAR FROM numbers.[!CreatedDocument(date)]) = extract(YEAR FROM documentDate) AND \n"
        + "              extract(MONTH FROM numbers.[!CreatedDocument(date)]) = extract(MONTH FROM documentDate) THEN \n"
        + "            number  := previosNumber;\n"
        + "            --newDate := numbers.[!CreatedDocument(date)];\n"
        + "          END IF;\n"
        + "          previosNumber := numbers.[!CreatedDocument(intNumber)];\n"
        + "          previosDate   := numbers.[!CreatedDocument(date)];\n"
        + "          EXIT WHEN number IS NOT NULL;"
        + "        END LOOP;\n"
        + "      END IF;\n"
        + "    END IF;\n"
        
        + "    IF partitionDocument.periodForZero = 'день' OR partitionDocument.periodForZero = 'P1D' THEN \n"
        + "      IF partitionDocument.grabFreeNumber = false THEN \n"
        + "        SELECT MAX([CreatedDocument(intNumber)]) INTO number FROM [CreatedDocument] WHERE tmp=false AND type='CURRENT' AND [CreatedDocument(stornoDate)] IS NULL AND \n"
        + "        [CreatedDocument(sellerCompanyPartition)]=partition AND [CreatedDocument(document)]=documentId \n"
        + "        AND date BETWEEN date_trunc('day', documentDate) AND date_trunc('day', documentDate) + interval '1 day' - interval '1 milliseconds';\n"
        + "      ELSE \n"
        + "      /*Цикл по отсортированным документам этого типа, принадлежащих данному владельцу в рамках заданного периода*/\n"
        + "        FOR numbers IN SELECT [CreatedDocument(intNumber)],[CreatedDocument(date)],[CreatedDocument(number)] FROM [CreatedDocument] WHERE \n"
        + "            [CreatedDocument(sellerCompanyPartition)]=partition AND [CreatedDocument(document)]=documentId AND tmp=false AND type='CURRENT' \n"
        + "            AND [CreatedDocument(stornoDate)] IS NULL AND date BETWEEN date_trunc('day', documentDate) AND \n"
        + "            date_trunc('day', documentDate) + interval '1 day' - interval '1 milliseconds' \n"
        + "            ORDER BY [CreatedDocument(intNumber)] LOOP \n"
        + "          /*Если имеется пропуск номера, то занимаем его, если день совпадает с текущим*/\n"
        + "          IF previosNumber != 0 AND \n"
        + "              numbers.[!CreatedDocument(intNumber)] - previosNumber > 1 AND \n"
        + "              numbers.[!CreatedDocument(date)]::date = documentDate::date THEN \n"
        + "            number  := previosNumber;\n"
        + "            --newDate := numbers.[!CreatedDocument(date)];\n"
        + "          END IF;\n"
        + "          previosNumber := numbers.[!CreatedDocument(intNumber)];\n"
        + "          previosDate   := numbers.[!CreatedDocument(date)];\n"
        + "          EXIT WHEN number IS NOT NULL;"
        + "        END LOOP;\n"
        + "      END IF;\n"
        + "    END IF;\n"

        + "    IF partitionDocument.periodForZero IS NULL THEN \n"
        + "      /*Получаем последний номер документа этого типа, принадлежащего данному владельцу*/\n"
        + "      SELECT [CreatedDocument(intNumber)], [CreatedDocument(date)] INTO numbers FROM [CreatedDocument] WHERE \n"
        + "        [CreatedDocument(intNumber)]=(SELECT MAX([CreatedDocument(intNumber)]) FROM [CreatedDocument] WHERE \n"
        + "        [CreatedDocument(sellerCompanyPartition)]=partition AND [CreatedDocument(document)]=documentId AND tmp=false AND type='CURRENT' \n"
        + "        AND [CreatedDocument(stornoDate)] IS NULL) AND [CreatedDocument(sellerCompanyPartition)]=partition AND \n"
        + "        [CreatedDocument(document)]=documentId AND tmp=false AND type='CURRENT' AND [CreatedDocument(stornoDate)] IS NULL;\n"
        + "      previosNumber := numbers.[!CreatedDocument(intNumber)];\n"
        + "      previosDate   := numbers.[!CreatedDocument(date)];\n"
        + "      RAISE NOTICE 'Последний номер =  %', number;\n"
        + "    END IF;\n"
        
        + "    IF number IS NULL THEN number := previosNumber; END IF;\n"
        + "    --IF newDate IS NULL OR documentDate >= newDate THEN \n"
        + "      --newDate := documentDate;\n"
        + "    --END IF;\n"
        + "    --IF newDate IS NULL THEN newDate := previosDate; END IF;\n"
        
        + "    /*Если нет номеров, то задаём начальный номер*/\n"
        + "    IF number IS NULL OR number = 0 THEN \n"
        + "      number := partitionDocument.startNumber-1;\n"
        + "    END IF;\n"
        
        + "    number := number+1;\n"
        + "    RAISE NOTICE 'Устанавливаю номер =  %', number;\n"
        
        + "    prefix := partitionDocument.prefix;\n"
        + "    typeFormat := NULL;\n"
        + "    IF partitionDocument.prefixTypeFormat IS NOT NULL THEN \n"
        + "      typeFormat := to_char(documentDate, replace(replace(replace(partitionDocument.prefixTypeFormat,'г','Y'),'м','M'),'д','D'));\n"
        + "    ELSE typeFormat := '';\n"
        + "    END IF;\n"
        + "    prefix := prefix||typeFormat||partitionDocument.prefixSplit;\n"
        
        + "    suffix := partitionDocument.suffixSplit;\n"
        + "    typeFormat := NULL;\n"
        + "    IF partitionDocument.suffixTypeFormat IS NOT NULL THEN \n"
        + "      typeFormat := to_char(documentDate, replace(replace(replace(partitionDocument.suffixTypeFormat,'г','Y'),'м','M'),'д','D'));\n"
        + "    ELSE typeFormat := '';\n"
        + "    END IF;\n"
        + "    suffix := suffix||typeFormat||partitionDocument.suffix;\n"
        
        + "    newNumber := number;\n"
        
        + "    IF prefix IS NOT NULL THEN newNumber := prefix||newNumber; END IF;\n"
        + "    IF suffix IS NOT NULL THEN newNumber := newNumber||suffix; END IF;\n"
        
        + "  END IF;\n"
              
        + "  ELSE\n"
        + "    newNumber := documentNumber;\n"
        + "    number    := 0;\n"
        + "    --newDate   := documentdate;\n"
        + "  END IF;\n"
              
        + "  IF newNumber IS NOT NULL THEN\n"
              
        + "    INSERT INTO [CreatedDocument] ([!CreatedDocument(document)],\n"
        + "      [!CreatedDocument(sellerCompanyPartition)],[!CreatedDocument(customerCompanyPartition)],[!CreatedDocument(payment)],\n"
        + "      [!CreatedDocument(actionType)],[!CreatedDocument(number)],[!CreatedDocument(intNumber)],[!CreatedDocument(date)]) \n"
        + "      VALUES (documentId,sellerpartition.id,customerpartition.id,paymentId,actionType,newNumber,number,documentDate);\n"
        + "    SELECT MAX(id) INTO createdDocId FROM [CreatedDocument];\n"
              
        + "  END IF;\n"
              
        + "    IF createdDocId IS NOT NULL AND paymentId IS NULL THEN \n"
        + "      IF dealPositions IS NOT NULL AND array_length(dealPositions, 1) > 0 THEN \n"
        + "        FOR i IN 1..array_length(dealPositions, 1) LOOP \n"
        + "          INSERT INTO [CreatedDocument(dealPositions):table] ([!CreatedDocument(dealPositions):object],[!CreatedDocument(dealPositions):target]) \n"
        + "            VALUES(createdDocId,dealPositions[i]);\n"
        + "        END LOOP;\n"
        + "      END IF;\n"
        + "    END IF;\n"
              
        + "  RETURN createdDocId;\n"
        + "END\n"
    )
  }
)

public class CreatedDocumentImpl extends MappingObjectImpl implements CreatedDocument {
  @ManyToOne(viewFields={"id","name","script","scriptLanguage","system"},
          viewNames={"document_id","document_name","document_script","document-scriptLanguage","document-system"})
  private Document document;

  @ManyToOne(
          viewFields = {
            "company",
            "kpp",
            "urAddres",
            "postAddres"},
          viewNames  = {
            "seller",
            "seller-kpp",
            "seller-uraddres",
            "seller-postaddres"})
  private CompanyPartition sellerCompanyPartition;

  @ManyToOne(
          viewFields = {
            "company",
            "kpp",
            "urAddres",
            "postAddres"},
          viewNames  = {
            "customer",
            "customer-kpp",
            "customer-uraddres",
            "customer-postaddres"})
  private CompanyPartition customerCompanyPartition;
  
  @Column
  private ProductDocument.ActionType actionType;
  
  @ManyToMany
  private List<DealPosition> dealPositions = new ArrayList<>();
  
  @ManyToMany
  private List<Deal> deals = new ArrayList<>();

  @Column
  private String number;

  @Column
  private int sellerAccountID;

  @Column
  private Integer intNumber;

  @Column(length=1000)
  private String xml;
  
  @Column
  private Timestamp stornoDate;
  
  @Column
  private ExportType exportType;
  
  @Column
  private SendType sendType;
  
  @ManyToOne(
          viewFields = {"amount"}, 
          viewNames = {"payment_amount"})
  private Payment payment;

  public CreatedDocumentImpl() throws RemoteException {
  }

  @Override
  public ExportType getExportType() throws RemoteException {
    return exportType;
  }

  @Override
  public void setExportType(ExportType exportType) throws RemoteException {
    this.exportType = exportType;
  }

  @Override
  public SendType getSendType() throws RemoteException {
    return sendType;
  }

  @Override
  public void setSendType(SendType sendType) throws RemoteException {
    this.sendType = sendType;
  }

  @Override
  public Timestamp getStornoDate() throws RemoteException {
    return stornoDate;
  }
  
  @Override
  public void setStornoDate(Timestamp stornoDate) throws RemoteException {
    this.stornoDate = stornoDate;
  }

  @Override
  public int getSellerAccountID() throws  RemoteException {
    return sellerAccountID;
  }

  @Override
  public void setSellerAccountID(int sellerAccountID) throws  RemoteException {
    this.sellerAccountID = sellerAccountID;
  }

  @Override
  public ProductDocument.ActionType getActionType() throws RemoteException {
    return actionType;
  }

  @Override
  public void setActionType(ProductDocument.ActionType actionType) throws RemoteException {
    this.actionType = actionType;
  }

  @Override
  public Integer getIntNumber() throws RemoteException {
    return intNumber;
  }

  @Override
  public void setIntNumber(Integer intNumber) throws RemoteException {
    this.intNumber = intNumber;
  }

  @Override
  public String getXml() throws RemoteException {
    return xml;
  }

  @Override
  public void setXml(String xml) throws RemoteException {
    this.xml = xml;
  }

  @Override
  public CompanyPartition getCustomerCompanyPartition() throws RemoteException {
    return customerCompanyPartition;
  }

  @Override
  public void setCustomerCompanyPartition(CompanyPartition customerCompanyPartition) throws RemoteException {
    this.customerCompanyPartition = customerCompanyPartition;
  }

  @Override
  public CompanyPartition getSellerCompanyPartition() throws RemoteException {
    return sellerCompanyPartition;
  }

  @Override
  public void setSellerCompanyPartition(CompanyPartition sellerCompanyPartition) throws RemoteException {
    this.sellerCompanyPartition = sellerCompanyPartition;
  }

  @Override
  public List<Deal> getDeals() throws RemoteException {
    return deals;
  }

  @Override
  public void setDeals(List<Deal> deals) throws RemoteException {
    this.deals = deals;
  }

  @Override
  public List<DealPosition> getDealPositions() throws RemoteException {
    return dealPositions;
  }

  @Override
  public void setDealPositions(List<DealPosition> dealPositions) throws RemoteException {
    this.dealPositions = dealPositions;
  }

  @Override
  public Document getDocument() throws RemoteException {
    return document;
  }

  @Override
  public void setDocument(Document document) throws RemoteException {
    this.document = document;
  }

  @Override
  public String getNumber() throws RemoteException {
    return number;
  }

  @Override
  public void setNumber(String number) throws RemoteException {
    this.number = number;
  }

  @Override
  public Payment getPayment() throws RemoteException {
    return payment;
  }

  @Override
  public void setPayment(Payment payment) throws RemoteException {
    this.payment = payment;
  }
}