package org.myrobotlab.serial;

// Copyright 2016, S&K Software Development Ltd.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Package crc implements generic CRC calculations up to 64 bits wide.
// It aims to be fairly complete, allowing users to match pretty much
// any CRC algorithm used in the wild by choosing appropriate Parameters.
// And it's also fairly fast for everyday use.
//
// This package has been largely inspired by Ross Williams' 1993 paper "A Painless Guide to CRC Error Detection Algorithms".
// A good list of parameter sets for various CRC algorithms can be found at http://reveng.sourceforge.net/crc-catalogue/.

/**
 * This class provides utility functions for CRC calculation using either
 * canonical straight forward approach or using "fast" table-driven
 * implementation. Note, that even though table-driven implementation is much
 * faster for processing large amounts of data and is commonly referred as fast
 * algorithm, sometimes it might be quicker to calculate CRC using canonical
 * algorithm then to prepare the table for table-driven implementation.
 * 
 * <p>
 * Using src is easy. Here is an example of calculating CCITT crc in one call
 * using canonical approach.
 * 
 * <pre>
 * {
 *   &#64;code
 *   String data = "123456789";
 *   long ccittCrc = CRC.calculateCRC(CRC.Parameters.CCITT, data.getBytes());
 *   System.out.printf("CRC is 0x%04X\n", ccittCrc); // prints "CRC is 0x29B1"
 * }
 * </pre>
 * 
 * <p>
 * For larger data, table driven implementation is faster. Here is how to use
 * it.
 * 
 * <pre>
 * {
 *   &#64;code
 *   String data = "123456789";
 *   CRC tableDriven = new CRC(CRC.Parameters.XMODEM);
 *   long xmodemCrc = tableDriven.calculateCRC(data.getBytes());
 *   System.out.printf("CRC is 0x%04X\n", xmodemCrc); // prints "CRC is 0x31C3"
 * }
 * </pre>
 * 
 * <p>
 * You can also reuse CRC object instance for another crc calculation.
 * <p>
 * Given that the only state for a CRC calculation is the "intermediate value"
 * and it is stored in your code, you can even use same CRC instance to
 * calculate CRC of multiple data sets in parallel. And if data is too big, you
 * may feed it in chunks
 * 
 * <pre>
 * {
 *   &#64;code
 *   long curValue = tableDriven.init(); // initialize intermediate value
 *   curValue = tableDriven.update(curValue, "123456789".getBytes()); // feed
 *   // first
 *   // chunk
 *   curValue = tableDriven.update(curValue, "01234567890".getBytes()); // feed
 *   // next
 *   // chunk
 *   long xmodemCrc2 = tableDriven.finalCRC(curValue); // gets CRC of whole data
 *   // ("12345678901234567890")
 *   System.out.printf("CRC is 0x%04X\n", xmodemCrc2); // prints "CRC is 0x2C89"
 * }
 * </pre>
 * 
 */
public class CRC {
  /**
   * Parameters represents set of parameters defining a particular CRC
   * algorithm.
   */
  public static class Parameters {
    private int width; // Width of the CRC expressed in bits
    private long polynomial; // Polynomial used in this CRC calculation
    private boolean reflectIn; // Refin indicates whether input bytes should be
    // reflected
    private boolean reflectOut; // Refout indicates whether input bytes should
    // be reflected
    private long init; // Init is initial value for CRC calculation
    private long finalXor; // Xor is a value for final xor to be applied before
    // returning result

    public Parameters(int width, long polynomial, long init, boolean reflectIn, boolean reflectOut, long finalXor) {
      this.width = width;
      this.polynomial = polynomial;
      this.reflectIn = reflectIn;
      this.reflectOut = reflectOut;
      this.init = init;
      this.finalXor = finalXor;
    }

    public Parameters(Parameters orig) {
      width = orig.width;
      polynomial = orig.polynomial;
      reflectIn = orig.reflectIn;
      reflectOut = orig.reflectOut;
      init = orig.init;
      finalXor = orig.finalXor;
    }

    public int getWidth() {
      return width;
    }

    public long getPolynomial() {
      return polynomial;
    }

    public boolean isReflectIn() {
      return reflectIn;
    }

    public boolean isReflectOut() {
      return reflectOut;
    }

    public long getInit() {
      return init;
    }

    public long getFinalXor() {
      return finalXor;
    }

    /** CCITT CRC parameters */
    public static final Parameters CCITT = new Parameters(16, 0x1021, 0x00FFFF, false, false, 0x0);
    /** CRC16 CRC parameters, also known as ARC */
    public static final Parameters CRC16 = new Parameters(16, 0x8005, 0x0000, true, true, 0x0);
    /** XMODEM is a set of CRC parameters commonly referred as "XMODEM" */
    public static final Parameters XMODEM = new Parameters(16, 0x1021, 0x0000, false, false, 0x0);
    /**
     * XMODEM2 is another set of CRC parameters commonly referred as "XMODEM"
     */
    public static final Parameters XMODEM2 = new Parameters(16, 0x8408, 0x0000, true, true, 0x0);

    /**
     * CRC32 is by far the the most commonly used CRC-32 polynom and set of
     * parameters
     */
    public static final Parameters CRC32 = new Parameters(32, 0x04C11DB7, 0x00FFFFFFFFL, true, true, 0x00FFFFFFFFL);
    /** IEEE is an alias to CRC32 */
    public static final Parameters IEEE = CRC32;
    /**
     * Castagnoli polynomial. used in iSCSI. And also provided by hash/crc32
     * package.
     */
    public static final Parameters Castagnoli = new Parameters(32, 0x1EDC6F41L, 0x00FFFFFFFFL, true, true, 0x00FFFFFFFFL);
    /** CRC32C is an alias to Castagnoli */
    public static final Parameters CRC32C = Castagnoli;
    /** Koopman polynomial */
    public static final Parameters Koopman = new Parameters(32, 0x741B8CD7L, 0x00FFFFFFFFL, true, true, 0x00FFFFFFFFL);

    /** CRC64ISO is set of parameters commonly known as CRC64-ISO */
    public static final Parameters CRC64ISO = new Parameters(64, 0x000000000000001BL, 0xFFFFFFFFFFFFFFFFL, true, true, 0xFFFFFFFFFFFFFFFFL);
    /** CRC64ECMA is set of parameters commonly known as CRC64-ECMA */
    public static final Parameters CRC64ECMA = new Parameters(64, 0x42F0E1EBA9EA3693L, 0xFFFFFFFFFFFFFFFFL, true, true, 0xFFFFFFFFFFFFFFFFL);

  }

  /**
   * Reverses order of last count bits.
   * 
   * @param in
   *          value wich bits need to be reversed
   * @param count
   *          indicates how many bits be rearranged
   * @return the value with specified bits order reversed
   */
  private static long reflect(long in, int count) {
    long ret = in;
    for (int idx = 0; idx < count; idx++) {
      long srcbit = 1L << idx;
      long dstbit = 1L << (count - idx - 1);
      if ((in & srcbit) != 0) {
        ret |= dstbit;
      } else {
        ret = ret & (~dstbit);
      }
    }
    return ret;
  }

  /**
   * This method implements simple straight forward bit by bit calculation. It
   * is relatively slow for large amounts of data, but does not require any
   * preparation steps. As a result, it might be faster in some cases then
   * building a table required for faster calculation.
   * 
   * @param crcParams
   *          CRC algorithm parameters
   * @param data
   *          data for the CRC calculation
   * @return the CRC value of the data provided
   */
  public static long calculateCRC(Parameters crcParams, byte[] data) {
    long curValue = crcParams.init;
    long topBit = 1L << (crcParams.width - 1);
    long mask = (topBit << 1) - 1;

    for (int i = 0; i < data.length; i++) {
      long curByte = ((long) (data[i])) & 0x00FFL;
      if (crcParams.reflectIn) {
        curByte = reflect(curByte, 8);
      }
      curValue ^= (curByte << (crcParams.width - 8));
      for (int j = 0; j < 8; j++) {
        if ((curValue & topBit) != 0) {
          curValue = (curValue << 1) ^ crcParams.polynomial;
        } else {
          curValue = (curValue << 1);
        }
      }

    }

    if (crcParams.reflectOut) {
      curValue = reflect(curValue, crcParams.width);
    }

    curValue = curValue ^ crcParams.finalXor;

    return curValue & mask;
  }

  private Parameters crcParams;
  private long initValue;
  private long[] crctable;
  private long mask;

  /**
   * Returns initial value for this CRC intermediate value This method is used
   * when starting a new iterative CRC calculation (using init, update and
   * finalCRC methods, possibly supplying data in chunks).
   * 
   * @return initial value for this CRC intermediate value
   */
  public long init() {
    return initValue;
  }

  /**
   * This method is used to feed data when performing iterative CRC calculation
   * (using init, update and finalCRC methods, possibly supplying data in
   * chunks). It can be called multiple times per CRC calculation to feed data
   * to be processed in chunks.
   * 
   * @param curValue
   *          CRC intermediate value so far
   * @param chunk
   *          data chunk to b processed by this call
   * @param offset
   *          is 0-based offset of the data to be processed in the array
   *          supplied
   * @param length
   *          indicates number of bytes to be processed.
   * @return updated intermediate value for this CRC
   */
  public long update(long curValue, byte[] chunk, int offset, int length) {
    if (crcParams.reflectIn) {
      for (int i = 0; i < length; i++) {
        byte v = chunk[offset + i];
        curValue = crctable[(((byte) curValue) ^ v) & 0x00FF] ^ (curValue >>> 8);
      }
    } else {
      for (int i = 0; i < length; i++) {
        byte v = chunk[offset + i];
        curValue = crctable[((((byte) (curValue >>> (crcParams.width - 8))) ^ v) & 0xFF)] ^ (curValue << 8);
      }
    }

    return curValue;
  }

  /**
   * A convenience method for feeding a complete byte array of data.
   * 
   * @param curValue
   *          CRC intermediate value so far
   * @param chunk
   *          data chunk to b processed by this call
   * @return updated intermediate value for this CRC
   */
  public long update(long curValue, byte[] chunk) {
    return update(curValue, chunk, 0, chunk.length);
  }

  /**
   * This method should be called to retrieve actual CRC for the data processed
   * so far.
   * 
   * @param curValue
   *          CRC intermediate value so far
   * @return calculated CRC
   */
  public long finalCRC(long curValue) {
    long ret = curValue;
    if (crcParams.reflectOut != crcParams.reflectIn) {
      ret = reflect(ret, crcParams.width);
    }
    return (ret ^ crcParams.finalXor) & mask;
  }

  /**
   * A convenience method allowing to calculate CRC in one call.
   * 
   * @param data
   *          is data to calculate CRC on
   * @return calculated CRC
   */
  public long calculateCRC(byte[] data) {
    long crc = init();
    crc = update(crc, data);
    return finalCRC(crc);
  }

  /**
   * Constructs a new CRC processor for table based CRC calculations.
   * Underneath, it just calls finalCRC() method.
   * 
   * @param crcParams
   *          CRC algorithm parameters
   */
  public CRC(Parameters crcParams) {
    this.crcParams = new Parameters(crcParams);

    initValue = (crcParams.reflectIn) ? reflect(crcParams.init, crcParams.width) : crcParams.init;
    this.mask = ((crcParams.width >= 64) ? 0 : (1L << crcParams.width)) - 1;
    this.crctable = new long[256];

    byte[] tmp = new byte[1];

    Parameters tableParams = new Parameters(crcParams);

    tableParams.init = 0;
    tableParams.reflectOut = tableParams.reflectIn;
    tableParams.finalXor = 0;
    for (int i = 0; i < 256; i++) {
      tmp[0] = (byte) i;
      crctable[i] = CRC.calculateCRC(tableParams, tmp);
    }
  }

  /**
   * Is a convenience method to spare end users from explicit type conversion
   * every time this package is used. Underneath, it just calls finalCRC()
   * method.
   * 
   * @param curValue
   *          current intermediate crc state value
   * @return the final CRC value
   * @throws RuntimeException
   *           if crc being calculated is not 8-bit
   */
  public byte finalCRC8(long curValue) {
    if (crcParams.width != 8)
      throw new RuntimeException("CRC width mismatch");
    return (byte) finalCRC(curValue);
  }

  /**
   * Is a convenience method to spare end users from explicit type conversion
   * every time this package is used. Underneath, it just calls finalCRC()
   * method.
   * 
   * @param curValue
   *          current intermediate crc state value
   * @return the final CRC value
   * @throws RuntimeException
   *           if crc being calculated is not 16-bit
   */
  public short finalCRC16(long curValue) {
    if (crcParams.width != 16)
      throw new RuntimeException("CRC width mismatch");
    return (short) finalCRC(curValue);
  }

  /**
   * Is a convenience method to spare end users from explicit type conversion
   * every time this package is used. Underneath, it just calls finalCRC()
   * method.
   * 
   * @param curValue
   *          current intermediate crc state value
   * @return the final CRC value
   * @throws RuntimeException
   *           if crc being calculated is not 32-bit
   */
  public int finalCRC32(long curValue) {
    if (crcParams.width != 32)
      throw new RuntimeException("CRC width mismatch");
    return (int) finalCRC(curValue);
  }

}