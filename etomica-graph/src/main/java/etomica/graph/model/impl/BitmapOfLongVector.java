/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.graph.model.impl;

import etomica.graph.model.Bitmap;

/**
 * Transparent implementation of arbitrarily large bitmaps based on an array of long
 * values.
 *
 * @author Demian Lessa
 */
public final class BitmapOfLongVector extends AbstractBitmap {

  private int bitSize = 0;
  private long[] bitmap = null;

  public BitmapOfLongVector(final Bitmap other) {

    this(other.bitSize(), false);
    copyFrom(other);
  }

  public BitmapOfLongVector(int bitSize) {

    this(bitSize, false);
  }

  public BitmapOfLongVector(int bitSize, final boolean isSet) {

    setBitSize(bitSize);
    allocateBitmap();
    setBits(isSet);
  }

  public BitmapOfLongVector(final String strBitmap) {

    this(strBitmap.length(), false);
    copyFrom(strBitmap);
  }

  public BitmapOfLongVector(final byte[] byteBitmap) {

    this((byte) byteBitmap.length, false);
    copyFrom(byteBitmap);
  }

  /**
   * @TIME = O(log N) for a compatible instance of other.
   */
  @Override
  public void and(final Bitmap other) {

    if (other instanceof BitmapOfLongVector) {
      BitmapOfLongVector bm = (BitmapOfLongVector) other;
      for (int i = 0; i < size(); i++) {
        bitmap[i] &= bm.bitmap[i];
      }
    }
    else {
      super.and(other);
    }
  }

  public int bitSize() {

    return bitSize;
  }

  /**
   * The highest order bit is at bit 0 of bitmap[0], and the lowest order bit is at bit
   * bitSize() - 1 of bitmap[size()-1].
   *
   * @TIME = O(1).
   */
  public void clearBit(int bitIndex) {

    bitmap[longOffset(bitIndex)] &= ~maskSingleBit(bitIndex);
  }

  /**
   * @TIME = O(log N) for an instance of BitmapOfLongVector.
   */
  @Override
  public boolean equals(Object other) {

    if (other instanceof BitmapOfLongVector) {
      BitmapOfLongVector bm = (BitmapOfLongVector) other;
      boolean result = (bitSize() == bm.bitSize());
      result = result && ((bitmap[0] & maskSingleLong(0)) == (bm.bitmap[0] & maskSingleLong(0)));
      int i = 1;
      while ((i < size()) && result) {
        result = result && (bitmap[i] == bm.bitmap[i]);
        i++;
      }
      return result;
    }
    else {
      return super.equals(other);
    }
  }

  /**
   * @TIME = O(log N).
   */
  @Override
  public int hashCode() {

    int hashCode = 0;
    for (int i = 0; i < size(); i++) {
      hashCode = (int) (31 * hashCode + (bitmap[i] & Bitmap.LONG_MASK));
    }
    return hashCode;
  }

  /**
   * @TIME = O(log N) for a compatible instance.
   */
  @Override
  public void nand(final Bitmap other) {

    if (other instanceof BitmapOfLongVector) {
      BitmapOfLongVector bm = (BitmapOfLongVector) other;
      for (int i = 0; i < size(); i++) {
        bitmap[i] &= ~bm.bitmap[i];
      }
    }
    else {
      super.nand(other);
    }
  }

  /**
   * @TIME = O(log N).
   */
  @Override
  public void not() {

    for (int i = 0; i < size(); i++) {
      bitmap[i] = ~bitmap[i];
    }
  }

  /**
   * @TIME = O(log N) for a compatible instance.
   */
  @Override
  public void or(final Bitmap other) {

    if (other instanceof BitmapOfLongVector) {
      BitmapOfLongVector bm = (BitmapOfLongVector) other;
      for (int i = 0; i < size(); i++) {
        bitmap[i] |= bm.bitmap[i];
      }
    }
    else {
      super.or(other);
    }
  }

  /**
   * The highest order bit is at bit 0 of bitmap[0], and the lowest order bit is at bit
   * bitSize() - 1 of bitmap[size()-1].
   *
   * @TIME = O(1).
   */
  public void setBit(int bitIndex) {

    bitmap[longOffset(bitIndex)] |= maskSingleBit(bitIndex);
  }

  /**
   * @TIME = O(log N).
   */
  @Override
  public void setBits(final boolean value) {

    for (int i = 0; i < size(); i++) {
      bitmap[i] = value ? maskSingleLong(i) : Bitmap.LONG_ZERO;
    }
  }

  /**
   * @TIME = O(1).
   */
  public boolean testBit(int bitIndex) {

    long bm = maskSingleBit(bitIndex);
    return (bitmap[longOffset(bitIndex)] & bm) == bm;
  }

  /**
   * @TIME = O(log N) for a compatible instance.
   */
  @Override
  public void xor(final Bitmap other) {

    if (other instanceof BitmapOfLongVector) {
      BitmapOfLongVector bm = (BitmapOfLongVector) other;
      for (int i = 0; i < size(); i++) {
        bitmap[i] ^= bm.bitmap[i];
      }
    }
    else {
      super.xor(other);
    }
  }

  @Override
  protected void allocateBitmap() {

    int size = bitSize() / Bitmap.SZ_LONG;
    if ((bitSize() % Bitmap.SZ_LONG) > 0) {
      size++;
    }
    bitmap = new long[size];
  }

  /**
   * Returns the number of valid bits in the long with the given index.
   *
   * @TIME = O(1).
   */
  protected int bitCount(int longIndex) {

    if (longIndex == 0) {
      return bitSize() % Bitmap.SZ_LONG;
    }
    else {
      return Bitmap.SZ_LONG;
    }
  }

  /**
   * The highest order bit is at bit 0 of bitmap[0], and the lowest order bit is at bit
   * bitSize() - 1 of bitmap[size()-1].
   *
   * @TIME = O(1).
   */
  protected int bitOffset(int bitIndex) {

    return bitIndex % Bitmap.SZ_LONG;
  }

  /**
   * @TIME = O(log N).
   */
  @Override
  protected void copyFrom(final Bitmap other) {

    if (other instanceof BitmapOfLongVector) {
      BitmapOfLongVector copy = (BitmapOfLongVector) other;
      for (int i = 0; i < size(); i++) {
        bitmap[i] = copy.bitmap[i];
      }
    }
  }

  @Override
  protected Bitmap createInstance(final Bitmap other) {

    return new BitmapOfLongVector(other);
  }

  @Override
  protected Bitmap createInstance(int bitSize) {

    return new BitmapOfLongVector(bitSize);
  }

  /**
   * The highest order bit is at bit 0 of bitmap[0], and the lowest order bit is at bit
   * bitSize() - 1 of bitmap[size()-1].
   *
   * @TIME = O(1).
   */
  protected int longOffset(int bitIndex) {

    return bitIndex / Bitmap.SZ_LONG;
  }

  /**
   * Returns a long with a single bit set, corresponding to the bit offset of the given
   * bit index.
   *
   * @TIME = O(1).
   */
  protected long maskSingleBit(int bitIndex) {

    return Bitmap.LONG_ONE << bitOffset(bitIndex);
  }

  /**
   * Returns a bitmask corresponding to the valid bits of the long with the given index.
   *
   * @TIME = O(1).
   */
  protected long maskSingleLong(int longIndex) {

    return Bitmap.LONG_MASK >>> (Bitmap.SZ_LONG - bitCount(longIndex));
  }

  /**
   * @TIME = O(1).
   * @return number of long values held by the bitmap
   */
  protected int size() {

    return bitmap.length;
  }

  protected void setBitSize(int bitSize) {

    this.bitSize = bitSize;
  }
}