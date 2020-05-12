package division.fx.controller.company;

import division.fx.border.titleborder.TitleBorderPane;
import division.fx.FXUtility;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class DocsData extends VBox {
  private final TitleBorderPane fileExportPane   = new TitleBorderPane(new Label("Файл экспорта"));
  private final TextField       fileExport       = new TextField();
  private final Button          fileExportSelect = new Button("...");
  
  private final TitleBorderPane  contractNumberingPane   = new TitleBorderPane(new Label("Нумерация договоров"));
  private final GridPane         contractGridPane        = new GridPane();
  private final TextField        contractPrefix          = new TextField();
  private final ComboBox<String> contractPrefixDataType  = new ComboBox<>();
  private final TextField        contractSplitPrefix     = new TextField();
  private final TextField        contractSplitSufix      = new TextField();
  private final ComboBox<String> contractSufixDataType   = new ComboBox<>();
  private final TextField        contractSufix           = new TextField();
  private final TextField        startNumber             = new TextField();
  
  private final String[] formats = new String[]{
    " ",
    "гг","гггг",
    "ггмм","гг/мм","гг.мм","гг-мм",
    "ммгг","мм/гг","мм.гг","мм-гг",
    "ггггмм","гггг/мм","гггг.мм","гггг-мм",
    "ммгггг","мм/гггг","мм.гггг","мм-гггг",
    "ггммдд","гг/мм/дд","гг.мм.дд","гг-мм-дд",
    "ммггдд","мм/гг/дд","мм.гг.дд","мм-гг-дд",
    "ггггммдд","гггг/мм/дд","гггг.мм.дд","гггг-мм-дд",
    "ммггггдд","мм/гггг/дд","мм.гггг.дд","мм-гггг-дд",
    "ггддмм","гг/дд/мм","гг.дд.мм","гг-дд-мм",
    "ммддгг","мм/дд/гг","мм.дд.гг","мм-дд-гг",
    "ггггддмм","гггг/дд/мм","гггг.дд.мм","гггг-дд-мм",
    "ммддгггг","мм/дд/гггг","мм.дд.гггг","мм-дд-гггг",
    "ддггмм","дд/гг/мм","дд.гг.мм","дд-гг-мм",
    "ддммгг","дд/мм/гг","дд.мм.гг","дд-мм-гг",
    "ддггггмм","дд/гггг/мм","дд.гггг.мм","дд-гггг-мм",
    "ддммгггг","дд/мм/гггг","дд.мм.гггг","дд-мм-гггг"
  };

  public DocsData() {
    super(5);
    initComponents();
  }

  private void initComponents() {
    FXUtility.initCss(this);
    getChildren().addAll(fileExportPane, contractNumberingPane);
    
    fileExportPane.setCenter(fileExport);
    fileExportPane.setRight(fileExportSelect);
    
    contractNumberingPane.setCenter(contractGridPane);
    
    contractGridPane.addRow(0, new Label("Префикс"), new Label("Дата"), new Label("Разделитель префикса"), new Label("Разделитель суффикса"), new Label("Дата"), new Label("Суффикс"), new Label("Начальный номер"));
    contractGridPane.addRow(1, contractPrefix, contractPrefixDataType, contractSplitPrefix, contractSplitSufix, contractSufixDataType, contractSufix, startNumber);
    contractGridPane.setAlignment(Pos.CENTER);
  }
}