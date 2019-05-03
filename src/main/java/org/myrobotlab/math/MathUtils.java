package org.myrobotlab.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * A place to have some handy math functions.
 * 
 * @author kwatters
 *
 */
public class MathUtils {
  
  transient public final static Logger log = LoggerFactory.getLogger(MathUtils.class);

  // convert degrees to radians.
  static public double degToRad(double degrees) {
    return degrees * Math.PI / 180.0;
  };

  public static Integer averageMaxFromArray(int howMany, ArrayList<Integer> list) {
    Collections.sort(list);

    int sumMaxAvg = 0;
    // Log.info("size:"+list.size()+" howmany: "+howMany);
    if (list.size() < howMany) {
      howMany = list.size();
    }
    if (list.size() > 0) {

      for (int i = list.size(); i > list.size() - howMany; i--) {
        sumMaxAvg += list.get(i - 1);
        // Log.debug(i-1+" "+list.get(i-1));
      }
      // Log.debug("math returned "+ sumMaxAvg / howMany);
      return sumMaxAvg / howMany;
    }
    log.warn("averageMaxFromArray error");
    return 0;
  }

  public static Integer getPercentFromRange(int min, int max, int percent) {
    return (((max - min) * percent) / 100 + min);
  }

  static public double radToDeg(double radians) {
    return radians * 57.2957795;
  }

  static public String msToString(long ms) {
    long seconds = ms / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;
    long days = hours / 24;
    String time = days + ":" + hours % 24 + ":" + minutes % 60 + ":" + seconds % 60;
    return time;
  }

  /*
   * Converts Gray encoded values to their corresponding decimal value
   */
  static public int decimalToGray(int decimal) {

    return (decimal ^ (decimal >> 1));
  }

  /*
   * Converts a decimal number to it's corresponding Gray encoded value
   */
  static public int grayToDecimal(int gray) {
    gray ^= (gray >> 16);
    gray ^= (gray >> 8);
    gray ^= (gray >> 4);
    gray ^= (gray >> 2);
    gray ^= (gray >> 1);
    return gray;
  }

  /*
   * just round a decimal based on digit
   */
  public static double round(double value, int digit) {
    if (digit < 0)
      throw new IllegalArgumentException();

    BigDecimal bd = new BigDecimal(value);
    bd = bd.setScale(digit, RoundingMode.HALF_UP);
    return bd.doubleValue();
  }

  public static String md5(String toGenerate) {

    MessageDigest m = null;
    try {
      m = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    m.update(toGenerate.getBytes(), 0, toGenerate.length());
    return new BigInteger(1, m.digest()).toString(16);

  }
}
