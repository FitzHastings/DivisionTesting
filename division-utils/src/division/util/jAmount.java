package division.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class jAmount {

private BigInteger summ;
private String [] suff = new String[]{"рубль","рубля","рублей","копейка","копейки","копеек","M"};
private static final BigInteger zero     = new BigInteger ("0");
private static final BigInteger hundred  = new BigInteger ("100");
private static final BigInteger thousand = new BigInteger ("1000");

public jAmount(Double s) {
  init(s);
}

public jAmount(BigDecimal s) {
  init(s);
}

public jAmount (String [] suff, Double s) {
  this.suff = suff;
  init(s);
}

public jAmount (String [] suff, BigDecimal s) {
  this.suff = suff;
  init(s);
}

public jAmount(String s) {
  init(Double.valueOf(s));
}

public jAmount (String [] suff, String s) {
  this.suff = suff;
  init(Double.valueOf(s));
}

private void init(Double s) {
  try {
    summ = new BigInteger(Utility.doubleToString(s, 2).replaceAll("\\.", ""));
  }catch (Exception e) {
  }
}

private void init(BigDecimal s) {
  try {
    s = s.setScale(2, RoundingMode.HALF_UP);
    summ = new BigInteger(String.valueOf(s).replaceAll("\\.", ""));
  }catch (Exception e) {
  }
}

// Получить правую (дробную) часть суммы
public String getRightPart () {
    return alignSumm (summ.remainder (hundred).abs ().toString ());
}

// Если сумма меньше 10, то выровнять ее дописыванием "0"
String alignSumm (String s) {
    switch (s.length ()) {
    case 0: s = "00"; break;
    case 1: s = "0" + s; break;
    }
    return s;
}


  @Override
public String toString () {
    StringBuffer result = new StringBuffer ();
    BigInteger [] divrem = summ.divideAndRemainder (hundred);
    if (divrem [0].signum () == 0) result.append ("Ноль ");
    divrem = divrem [0].divideAndRemainder (thousand);
    BigInteger quotient  = divrem [0];
    BigInteger remainder = divrem [1];
    int group = 0;
    do {
       int value = remainder.intValue ();
       result.insert (0, groupToString (value, group));
       // Для нулевой группы добавим в конец соответствующую валюту
       if (group == 0) {
           int rank10 = (value % 100) / 10;
           int rank = value % 10;
           if (rank10 == 1) {
               result = result.append (suff [2]);
           }
           else {
                switch (rank) {
                case 1: result = result.append (suff [0]); break;
                case 2:
                case 3:
                case 4: result = result.append (suff [1]); break;
               default: result = result.append (suff [2]); break;
                }
           }
       }
       divrem = quotient.divideAndRemainder (thousand);
       quotient  = divrem [0];
       remainder = divrem [1];
       group++;
    }
    while (!quotient.equals (zero) || !remainder.equals (zero));

    // Дробная часть суммы
    String s = getRightPart ();
    result = result.append (" ").append (s);
    result = result.append (" ");

    if (s.charAt (0) == '1') {
        result = result.append (suff [5]);
    }
    else {
         switch (s.charAt(1)) {
         case '1': result = result.append (suff [3]); break;
         case '2':
         case '3':
         case '4': result = result.append (suff [4]); break;
         default:  result = result.append (suff [5]); break;
        }
    }
    // По правилам бухгалтерского учета первая буква строкового
    // представления должна быть в верхнем регистре
    result.setCharAt (0, Character.toUpperCase (result.charAt (0)));

    // Вот ради этой строки все и затевалось: результат получен !!!
    //fplAmount.res = result.toString();
    return result.toString();
}

// Преобразование группы цифр в строку
String groupToString (int value, int group) {
  if (value < 0 || value > 999) throw new IllegalArgumentException ("value must be between 0 an 999 inclusively");
  if (group < 0) throw new IllegalArgumentException ("group must be more than 0");
  StringBuffer result = new StringBuffer (32);
  if (value == 0) {
      return result.toString();
  }
  // Разбор числа по разрядам, начиная с сотен
  int rank = value / 100;
  switch (rank) {
  default: break;
  case 1:  result = result.append ("сто ");       break;
  case 2:  result = result.append ("двести ");    break;
  case 3:  result = result.append ("триста ");    break;
  case 4:  result = result.append ("четыреста "); break;
  case 5:  result = result.append ("пятьсот ");   break;
  case 6:  result = result.append ("шестьсот ");  break;
  case 7:  result = result.append ("семьсот ");   break;
  case 8:  result = result.append ("восемьсот "); break;
  case 9:  result = result.append ("девятьсот "); break;
  }
  // Далее, десятки
  rank = (value % 100) / 10;
  switch (rank) {
  default: break;
  case 2:  result = result.append ("двадцать ");    break;
  case 3:  result = result.append ("тридцать ");    break;
  case 4:  result = result.append ("сорок ");       break;
  case 5:  result = result.append ("пятьдесят ");   break;
  case 6:  result = result.append ("шестьдесят ");  break;
  case 7:  result = result.append ("семьдесят ");   break;
  case 8:  result = result.append ("восемьдесят "); break;
  case 9:  result = result.append ("девяносто ");   break;
  }
  // Если десятки = 1, добавить соответствующее значение. Иначе - единицы
  int rank10 = rank;
  rank = value % 10;
  if (rank10 == 1) {
      switch (rank) {
      case 0: result = result.append ("десять ");       break;
      case 1: result = result.append ("одиннадцать ");  break;
      case 2: result = result.append ("двенадцать ");   break;
      case 3: result = result.append ("тринадцать ");   break;
      case 4: result = result.append ("четырнадцать "); break;
      case 5: result = result.append ("пятнадцать ");   break;
      case 6: result = result.append ("шестнадцать ");  break;
      case 7: result = result.append ("семнадцать ");   break;
      case 8: result = result.append ("восемнадцать "); break;
      case 9: result = result.append ("девятнадцать "); break;
      }
  }
  else {
      switch (rank) {
      default:
           break;
      case 1:
           if (group == 1) // Тысячи
               result = result.append ("одна ");
           else
              // Учесть род валюты (поле "Sex" настроечной информации)
              if (suff [6].equals ("M")) result = result.append ("один ");
              if (suff [6].equals ("F")) result = result.append ("одна ");
           break;
      case 2:
           if (group == 1) // Тысячи
               result = result.append ("две ");
           else
              // Учесть род валюты (поле "Sex" настроечной информации)
              if (suff [6].equals ("M")) result = result.append ("два ");
              if (suff [6].equals ("F")) result = result.append ("две ");
           break;
      case 3: result = result.append ("три ");    break;
      case 4: result = result.append ("четыре "); break;
      case 5: result = result.append ("пять ");   break;
      case 6: result = result.append ("шесть ");  break;
      case 7: result = result.append ("семь ");   break;
      case 8: result = result.append ("восемь "); break;
      case 9: result = result.append ("девять "); break;
      }
  }
  // Значение группы: тысячи, миллионы и т.д.
  switch (group) {
  default:
       break;
  case 1:
       if (rank10 == 1)
           result = result.append ("тысяч ");
          else {
          switch (rank) {
          default: result = result.append ("тысяч ");  break;
          case 1:  result = result.append ("тысяча "); break;
          case 2:
          case 3:
          case 4:  result = result.append ("тысячи "); break;
          }
       }
       break;
  case 2:
       if (rank10 == 1)
           result = result.append ("миллионов ");
       else {
            switch (rank) {
            default: result = result.append ("миллионов "); break;
            case 1:  result = result.append ("миллион ");   break;
            case 2:
            case 3:
            case 4:  result = result.append ("миллиона ");  break;
            }
       }
       break;
  case 3:
       if (rank10 == 1)
           result = result.append ("миллиардов ");
       else {
            switch (rank) {
            default: result = result.append ("миллиардов "); break;
            case 1:  result = result.append ("миллиард ");   break;
            case 2:
            case 3:
            case 4:  result = result.append ("миллиарда ");  break;
            }
       }
      break;
  case 4:
       if (rank10 == 1)
           result = result.append ("триллионов ");
       else {
            switch (rank) {
            default: result = result.append ("триллионов "); break;
            case 1:  result = result.append ("триллион ");   break;
            case 2:
            case 3:
            case 4:  result = result.append ("триллиона ");  break;
            }
       }
       break;
  }
  return result.toString();
  }
}      