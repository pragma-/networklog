/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

public class StringUtils {
  public static boolean contains(String string, String chars) {
    int stringLength = string.length();
    int charsLength = chars.length();
    int i, j;
    char c;

    for(i = 0; i < stringLength; i++) {
      c = string.charAt(i);
      for(j = 0; j < charsLength; j++) {
        if(c == chars.charAt(j)) {
          return true;
        }
      }
    }
    return false;
  }
}
