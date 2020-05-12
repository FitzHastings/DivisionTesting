package scripts.groovy

import javax.swing.*;
import division.swing.*;
import division.util.*
import division.swing.table.*;
import bum.editors.util.*;
import bum.interfaces.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import division.editors.objects.company.*;
import documents.*;
import util.filter.local.*;
import division.swing.guimessanger.*;
import org.apache.fop.apps.*;
import division.util.EmailUtil.Addres;
import division.util.EmailUtil.Attachment
import division.swing.guimessanger.*;

public void startEmail(partition, d) {
	try {
		def query = ""
		def param = []
		if(partition != null)
			param << partition;
		def template = [:]
		d.each {
			query += " OR [CreatedDocument(document)]=? AND date BETWEEN ? AND ?"
			param << it[0]
			param << new java.sql.Timestamp(it[1].getTime())
			param << new java.sql.Timestamp(it[2].getTime())
			template[it[0]] = it[3]
		}
	
		if(!query.equals(""))
			query = query.substring(4);

		def moduls = [];
	     def data = ObjectLoader.getData(JSModul.class, new DBFilter(JSModul.class), ["script"].toArray(new String[0]));
	     data.each{moduls << it[0]}
		
		data = ObjectLoader.getData("SELECT "
		+"id, "
		+"(SELECT name FROM [Document] WHERE id = [CreatedDocument(document)]), "
		+"number, "
		+"(SELECT email FROM [CompanyPartition] WHERE id=[CreatedDocument(customerCompanyPartition)]) as email, "
		+"[CreatedDocument(customerName)], "
		+"[CreatedDocument(document)], "
		+"[CreatedDocument(params)] "
		+" FROM [CreatedDocument] "
		+"WHERE "
		+(partition!=null?"[CreatedDocument(sellerCompanyPartition)]=? AND":"")
		+"("+query+") "
		+"ORDER BY email", 
		param.toArray());

		def id
		def name
		def number
		def email
		def companyName
		def documentId
		def variables
		def emailDocs = []
		def errorEmail = [:]
		printf "DATA SIZE = "+data.size()+"\n";
		data.each {
			if(it[3] == null || it[3].equals("") || !it[3].matches("[^\\s\\@]+\\@[^\\s\\.]+\\.\\w{2,4}")) {
				errorEmail[it[4]] = it[3];
			}else {
				if(!it[3].equals(email) && email != null) {
					try {
						def to     = new Addres("seniorroot@gmail.com", "Руслан Платонов");
						def from = new Addres("russoforjob@yandex.ru", "Руслан Платонов");
						printf "SEND TO "+companyName+"\n"
						printf ObjectLoader.getServer().sendEmail(
							"smtp.masterhost.ru", 
							25, 
							"sales@dnc-trade.ru",
							"1010",
							[to].toArray(new EmailUtil.Addres[0]), 
							[from].toArray(new EmailUtil.Addres[0]), 
							"Документы для "+companyName, 
							"test",
							"UTF-8",
							emailDocs.toArray(new Attachment[0]))+"\n\n\n";
						emailDocs.clear();
					}catch(Exception ex) {
						printf "put "+companyName+" = "+" ("+email+") "+ex.getMessage()+"\n"
						errorEmail[companyName] = " ("+email+") "+ex.getMessage();
					}
				}
				id                   = it[0];
				name              = it[1];
				number           = it[2];
				email              = it[3];
				companyName = it[4];
				documentId     = it[5];
				variables         = it[6];
				emailDocs << new Attachment(FOP.export_from_XML_to_bytea(
					FOP.get_XML_From_XMLTemplate(template[documentId], moduls,  variables), 
					MimeConstants.MIME_PDF), 
					"application/pdf", 
					javax.mail.internet.MimeUtility.encodeWord(name+" № "+number)+".pdf");
			}
		}

		if(email != null) {
			try {
				def to     = new Addres("seniorroot@gmail.com", "Руслан Платонов");
				def from = new Addres("russoforjob@yandex.ru", "Руслан Платонов");
				printf ObjectLoader.getServer().sendEmail(
					"smtp.masterhost.ru", 
					25, 
					"sales@dnc-trade.ru",
					"1010",
					[to].toArray(new Addres[0]), 
					from, 
					"Документы для "+companyName, 
					"test",
					"UTF-8",
					emailDocs.toArray(new Attachment[0]))+"\n\n\n";
				emailDocs.clear();
			}catch(Exception ex) {
				printf "put "+companyName+" = "+" ("+email+") "+ex.getMessage()+"\n"
				errorEmail[companyName] = " ("+email+") "+ex.getMessage();
			}
		}

		String warning = "";
		errorEmail.each { key, value -> warning += key+value+"\n"}
		if(!warning.equals(""))
			Messanger.fireMessage(GuiMessageListener.Type.WARNING, "", warning, null)
		
	}catch(Exception ex) {
		Messanger.showErrorMessage(ex);
	}
}

def frame = new DivisionDialog();
frame.setTitle("Рассылка документов");



frame.getContentPane().setLayout(new GridBagLayout());
def onlyDebt = new JCheckBox("Только долги", false);

def docTable = new DivisionTable();
docTable.setColumns(["id","Выбрать","Документ","с","по", "шаблон"].toArray());
docTable.setColumnWidthZero(0);
docTable.getColumn("Документ").setMinWidth(200);
docTable.getColumn("шаблон").setMinWidth(150);
docTable.getColumn("Выбрать").setMinWidth(30);
docTable.getColumn("Выбрать").setMaxWidth(30);
docTable.setColumnEditable(1,true);
docTable.setColumnEditable(3,true);
docTable.setColumnEditable(4,true);
docTable.setColumnEditable(5,true);

def data = ObjectLoader.getData("SELECT "
	+"id, "
	+"name, "
	+"(SELECT array_agg(id||'^~^'||name||'^~^'||main||'^~^'||XML) FROM [DocumentXMLTemplate] WHERE [DocumentXMLTemplate(document)]=[Document(id)] and tmp=false and type='CURRENT') "
	+"FROM [Document] "
	+"WHERE system=true AND tmp=false AND type='CURRENT' AND (SELECT COUNT(id) FROM [DocumentXMLTemplate] WHERE [DocumentXMLTemplate(document)]=[Document(id)] and tmp=false and type='CURRENT')>0");
data.each {
	def temp = new DivisionComboBox();
	it[2].each {em -> 
		def item = em.split("\\^\\~\\^");
		temp.addItem(new DivisionItem(Integer.valueOf(item[0]), item[1], "", item.length > 3?item[3]:""));
		if(Boolean.valueOf(item[2])) {
			temp.setSelectedIndex(temp.getItemCount()-1);
		}
	}
	docTable.getTableModel().addRow([it[0], false, it[1], new java.util.Date(), new java.util.Date(), temp].toArray());
}

def start = new JButton("Начать рассылку");

def sellerPanel   = new SellerCustomerPanel("Продавец", false, true, true, true, false, false);
def scroll = new JScrollPane(docTable);
scroll.setPreferredSize(new Dimension(600,150));

frame.getContentPane().add(sellerPanel,  new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
frame.getContentPane().add(scroll,          new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
frame.getContentPane().add(start,          new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

docTable.getTableModel().addTableModelListener(new TableModelListener() {
	public void tableChanged(TableModelEvent e) {
		if(e.getColumn() >= 3 && e.getColumn() <= 4 && e.getType() == TableModelEvent.UPDATE && e.getLastRow() < docTable.getRowCount()-1) {
			docTable.getTableModel().setValueAt(docTable.getTableModel().getValueAt(e.getLastRow(),e.getColumn()), e.getLastRow()+1, e.getColumn());
		}
	}
});

start.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
		def d = [];
		docTable.getTableModel().getDataVector().each{
			if(it[1]) {
				d << [it[0], it[3], it[4], it[5].getSelectedItem().getData()]
			}
		}
		startEmail(sellerPanel.getPartition(), d);
	}
});

frame.centerLocation();
frame.setAlwaysOnTop(true);
frame.setVisible(true);
