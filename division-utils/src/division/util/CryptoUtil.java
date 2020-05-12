package division.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
 
 
public class CryptoUtil {
  public static final String ALGORITHM_KEY = "DES";
  public static final String ALGORITHM = "DES/PCBC/PKCS5Padding";
  
  public static final String symbolsToRandomString = "abcdifghijklmnopqrstuvwxyz1234567890"+"abcdifghijklmnopqrstuvwxyz".toUpperCase();

  public static final byte[] SALT = new byte[] {
    (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8, (byte) 0xee, (byte) 0x99
  };


  public static byte[] encode(String clearText, Key key) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
    return crypt(clearText.getBytes("UTF-8"), key, Cipher.ENCRYPT_MODE);
  }


  public static String decode(byte[] cipherText, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
    return new String(crypt(cipherText, key, Cipher.DECRYPT_MODE));
  }


  public static SecretKey generateKey() throws NoSuchAlgorithmException {
    return KeyGenerator.getInstance(ALGORITHM_KEY).generateKey();
  }


  private static byte[] crypt(byte[] input, Key key, int mode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
    IvParameterSpec iv = new IvParameterSpec(SALT);
    Cipher c = Cipher.getInstance(ALGORITHM);
    c.init(mode, key, iv);
    return c.doFinal(input);
  }
  
  public static CryptoData encode(String string) throws NoSuchAlgorithmException, UnsupportedEncodingException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
    Key key = generateKey();
    return new CryptoData(key,encode(string, key));
  }
  
  public static class CryptoData {
    public Key KEY;
    public byte[] DATA;

    private CryptoData(Key KEY, byte[] DATA) {
      this.KEY = KEY;
      this.DATA = DATA;
    }
  }
  
  public static String generateRandomString(int stringLength) {
    return generateRandomString(stringLength, symbolsToRandomString);
  }
  
  public static String generateRandomString(int stringLength, String symbols) {
    Random random = new Random();
    StringBuilder randString = new StringBuilder();
    for(int i=1;i<=stringLength;i++)
      randString.append(symbols.charAt(random.nextInt(symbols.length())));
    return randString.toString();
  }
}