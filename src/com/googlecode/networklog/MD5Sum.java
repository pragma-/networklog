/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.math.BigInteger;

public class MD5Sum {
  public static String digestFile(File file) {
    MessageDigest md;
    InputStream is = null;

    try {
      md = MessageDigest.getInstance("MD5");
      is = new DigestInputStream(new FileInputStream(file), md);
      byte[] bytes = new byte[8192];
      while(is.read(bytes) > 0) {}
    } catch (java.security.NoSuchAlgorithmException nsa) {
      Log.e("NetworkLog", "MD5 algorithm not supported", nsa);
      return "";
    } catch (java.io.FileNotFoundException nfe) {
      Log.e("NetworkLog", "File not found", nfe);
      return "";
    } catch (java.io.IOException ioe) {
      Log.e("NetworkLog", "IO exception", ioe);
      return "";
    }  finally {
      try {
        is.close();
      } catch (java.io.IOException e) {
        e.printStackTrace();
      }
    }

    byte[] digest = md.digest();
    return new BigInteger(1, digest).toString(16);
  }
}
