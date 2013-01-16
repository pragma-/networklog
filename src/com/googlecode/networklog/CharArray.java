/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
   */

// Utility class for manipulating a char[] without unnecessary allocations

package com.googlecode.networklog;

public class CharArray implements Comparable<CharArray> {
  char[] value;
  int offset;
  int length;
  char[] intBuffer = new char[32];

  public CharArray() {
    value = null;
    offset = 0;
    length = 0;
  }

  public CharArray(int size) {
    value = new char[size];
    offset = 0;
    length = 0;
  }

  public CharArray(char[] value, int offset, int length) {
    this.value = value;
    this.offset = offset;
    this.length = length;
  }

  public String toString() {
    return new String(value, offset, length);
  }

  public int compareTo(String target) {
    int i = offset;
    int j = 0;
    int targetLength = target.length();
    int end = offset + length;
    int difference;

    while(i < end && j < targetLength) {
      difference = value[i++] - target.charAt(j++);

      if(difference > 0) {
        return 1;
      } else if(difference < 0) {
        return -1;
      }
    }

    if(length > targetLength) {
      return 1;
    } else if(length < targetLength) {
      return -1;
    } else {
      return 0;
    }
  }

  public int compareTo(CharArray target) {
    int i = offset;
    int j = target.offset;
    int end = offset + length;
    int targetEnd = target.offset + target.length;
    int difference;

    while(i < end && j < targetEnd) {
      difference = value[i++] - target.value[j++];

      if(difference > 0) {
        return 1;
      } else if(difference < 0) {
        return -1;
      }
    }

    if(length > target.length) {
      return 1;
    } else if(length < target.length) {
      return -1;
    } else {
      return 0;
    }
  }

  public CharArray append(char[] targetValue, int targetOffset, int targetLength) {
    int i = targetOffset;
    int end = targetOffset + targetLength;

    while(i < end) {
      if(length >= value.length) {
        throw new ArrayIndexOutOfBoundsException(length);
      }
      value[length++] = targetValue[i++];
    }

    return this;
  }

  public CharArray append(String string) {
    if(string == null) {
      return this;
    }

    int targetLength = string.length();
    int i = 0;

    while(i < targetLength) {
      if(length >= value.length) {
        throw new ArrayIndexOutOfBoundsException(length);
      }
      value[length++] = string.charAt(i++);
    }

    return this;
  }

  public CharArray append(int intval) {
    int intPos = 0;
    boolean negative = false;

    if(intval < 0) {
      negative = true;
      intval = Math.abs(intval);
    }

    int val;

    // convert int to char[] (note: will be reversed)
    if(intval == 0) {
      intBuffer[intPos++] = '0';
    } else {
      while(intval != 0) {
        val = intval % 10;
        intBuffer[intPos++] = (char) ('0' + val);
        intval /= 10;
      }
    }

    if(negative == true) {
      intBuffer[intPos++] = '-';
    }

    // reverse intBuffer so number is in correct format
    char temp;
    int right;
    for(int left = 0; left < intPos / 2; left++) {
      temp = intBuffer[left];
      right = intPos - left - 1;
      intBuffer[left] = intBuffer[right];
      intBuffer[right] = temp;
    }

    // append intBuffer to value
    for(int i = 0; i < intPos; i++) {
      if(length >= value.length) {
        throw new ArrayIndexOutOfBoundsException(length);
      }
      value[length++] = intBuffer[i];
    }

    return this;
  }

  public CharArray append(char charval) {
    if(length < value.length) {
      value[length++] = charval;
    } else {
      throw new ArrayIndexOutOfBoundsException(length);
    }

    return this;
  }

  public void reset() {
    length = 0;
  }

  public void setValue(char[] value, int offset, int length) {
    this.value = value;
    this.offset = offset;
    this.length = length;
  }

  public char[] getValue() {
    return value;
  }

  public int getOffset() {
    return offset;
  }

  public int getLength() {
    return length;
  }
}
