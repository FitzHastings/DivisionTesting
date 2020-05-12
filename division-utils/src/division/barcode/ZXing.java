package division.barcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Hashtable;
import java.util.Random;
import javax.imageio.ImageIO;

public class ZXing {
  private static String liters = "qwertyuiopasdfghjklzxcvbnm0123456789";
  
  public static BufferedImage generateImage(String content, int width, int height, Color background, Color codeColor) throws WriterException {
    return generateImage(BarcodeFormat.QR_CODE, content, width, height, background, codeColor);
  }
  
  public static BufferedImage generateImage(String content) throws WriterException {
    return generateImage(BarcodeFormat.QR_CODE, content, 100, 100);
  }
  
  public static BufferedImage generateImage(BarcodeFormat format, String content) throws WriterException {
    return generateImage(format, content, 100, 100);
  }
  
  public static BufferedImage generateImage(BarcodeFormat format, String content, int width, int height) throws WriterException {
    return generateImage(format, content, width, height, Color.BLACK);
  }
  
  public static BufferedImage generateImage(BarcodeFormat format, String content, int width, int height, Color background) throws WriterException {
    return generateImage(format, content, width, height, background, Color.BLACK);
  }
  
  public static BufferedImage generateImage(BarcodeFormat format, String content, int width, int height, Color background, Color codeColor) throws WriterException {
    BitMatrix bitMatrix = getMatrix(format, content, width, height);
    
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    image.createGraphics();
    Graphics2D graphics = (Graphics2D) image.getGraphics();
    graphics.setColor(background);
    graphics.fillRect(0, 0, width, height);
    graphics.setColor(codeColor);
    
    for(int i = 0; i < width; i++)
      for(int j = 0; j < height; j++)
        if(bitMatrix.get(i, j))
          graphics.fillRect(i, j, 1, 1);
    return image;
  }
  
  private static BitMatrix getMatrix(BarcodeFormat format, String content, int width, int height) throws WriterException {
    Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<>();
    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
    
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix bitMatrix = qrCodeWriter.encode(content, format, width, height, hintMap);
    return bitMatrix;
  }
  
  public static String QRFile(String content, int width, int height) throws IOException, WriterException {
    return generateToImageFile(BarcodeFormat.QR_CODE, content, width, height, Color.WHITE, Color.BLACK);
  }
  
  public static String QRFile(String content, int width, int height, Color background, Color codeColor) throws IOException, WriterException {
    return generateToImageFile(BarcodeFormat.QR_CODE, content, width, height, background, codeColor);
  }
  
  public static String generateToImageFile(BarcodeFormat format, String content, int width, int height, Color background, Color codeColor) throws IOException, WriterException {
    return generateToImageFile(generateTmpFile("png"), format, content, width, height, background, codeColor);
  }
  
  public static String generateToImageFile(String fileName, BarcodeFormat format, String content, int width, int height, Color background, Color codeColor) throws IOException, WriterException {
    return generateToImageFile(new File(fileName), format, content, width, height, background, codeColor);
  }
  
  public static String generateToImageFile(File file, BarcodeFormat format, String content, int width, int height, Color background, Color codeColor) throws IOException, WriterException {
    ImageIO.write(generateImage(format, content, width, height, background, codeColor), file.getName().substring(file.getName().lastIndexOf(".")+1), file);
    return file.getAbsolutePath();
  }
  
  private static String generateRandomString() {
    Random random = new Random();
    StringBuilder s = new StringBuilder();
    for(int i=0;i<10;i++)
      s.append(liters.charAt(random.nextInt(liters.length())));
    return s.toString();
  }
  
  private static File generateTmpFile(String type) {
    File f = new File("tmp"+File.separator+generateRandomString()+"."+type);
    f.mkdirs();
    return f;
  }
}