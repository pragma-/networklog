/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
   */

// Utility class for manipulating a char[] without unnecessary allocations

package com.googlecode.networklog;

public class CharArray implements Comparable<CharArray> {
  char[] data;
  int len;
  int pos;
  char[] intBuffer = new char[32];

  public CharArray(int len) {
    this.len = len;
    data = new char[len];
  }

  public String toString() {
    return new String(data, 0, pos);
  }

  public int compareTo(String target) {
    int i = 0;
    int j = 0;
    int targetLength = target.length();
    int difference;

    while(i < pos && j < targetLength) {
      difference = data[i++] - target.charAt(j++);

      if(difference > 0) {
        return 1;
      } else if(difference < 0) {
        return -1;
      }
    }

    if(pos > targetLength) {
      return 1;
    } else if(pos < targetLength) {
      return -1;
    } else {
      return 0;
    }
  }

  public int compareTo(CharArray target) {
    int i = 0;
    int j = 0;
    int targetLength = target.getPos();
    char[] targetData = target.getData();
    int difference;

    while(i < pos && j < targetLength) {
      difference = data[i++] - targetData[j++];

      if(difference > 0) {
        return 1;
      } else if(difference < 0) {
        return -1;
      }
    }

    if(pos > targetLength) {
      return 1;
    } else if(pos < targetLength) {
      return -1;
    } else {
      return 0;
    }
  }

  public CharArray append(char[] chars, int pos, int length) {
    int i = pos;

    while(i < pos + length) {
      if(this.pos > len) {
        throw new ArrayIndexOutOfBoundsException(this.pos);
      }
      data[this.pos++] = chars[i++];
    }

    return this;
  }

  public CharArray append(String string) {
    int length = string.length();
    int i = 0;

    while(i < length) {
      if(pos > len) {
        throw new ArrayIndexOutOfBoundsException(pos);
      }
      data[pos++] = string.charAt(i++);
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

    // append intBuffer to data
    for(int i = 0; i < intPos; i++) {
      if(pos > len) {
        throw new ArrayIndexOutOfBoundsException(pos);
      }
      data[pos++] = intBuffer[i];
    }

    return this;
  }

  public CharArray append(char charval) {
    if(pos < len) {
      data[pos++] = charval;
    } else {
      throw new ArrayIndexOutOfBoundsException(pos);
    }

    return this;
  }

  public void reset() {
    pos = 0;
  }

  public char[] getData() {
    return data;
  }

  public int getPos() {
    return pos;
  }
}
