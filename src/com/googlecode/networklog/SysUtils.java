/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.BufferedWriter;
import java.util.zip.ZipInputStream;

public class SysUtils {
  public static String iptablesBinary;
  public static String iptablesMd5;
  public static int iptablesResource;
  public static String grepBinary;
  public static String grepMd5;
  public static int grepResource;
  public static String nflogBinary;
  public static String nflogMd5;
  public static int nflogResource;

  public static boolean getBinariesIdentifiers() {
    String cpu_abi = Build.CPU_ABI.toLowerCase();

    if(cpu_abi.contains("armeabi-v7")) {
      iptablesBinary = "iptables_armv7";
      iptablesMd5 = "5515873b7ce1617f3d724a3332c2b947";
      iptablesResource = R.raw.iptables_armv7;
      grepBinary = "grep_armv7";
      grepMd5 = "69d0726f2b314a32fcd906a753deaabb";
      grepResource = R.raw.grep_armv7;
      nflogBinary = "nflog_armv7";
      nflogMd5 = "5cdaa519c10ace37f62a0e6d65ce4f6e";
      nflogResource = R.raw.nflog_armv7;
    } else if(cpu_abi.contains("armeabi")) {
      iptablesBinary = "iptables_armv5";
      iptablesMd5 = "50e39f66369344b692084a9563c185d4";
      iptablesResource = R.raw.iptables_armv5;
      grepBinary = "grep_armv5";
      grepMd5 = "7904ae3e4f310f9a1bf9867cfadb71ef";
      grepResource = R.raw.grep_armv5;
      nflogBinary = "nflog_armv5";
      nflogMd5 = "c8e7b5f4e96a47b686a68b84c9d12f24";
      nflogResource = R.raw.nflog_armv5;
    } else if(cpu_abi.contains("x86")) {
      iptablesBinary = "iptables_x86";
      iptablesMd5 = "3e7090f93ae3964c98e16016b742acbc";
      iptablesResource = R.raw.iptables_x86;
      grepBinary = "grep_x86";
      grepMd5 = "75210f186d666f32a14d843fd1e9fac5";
      grepResource = R.raw.grep_x86;
      nflogBinary = "nflog_x86";
      nflogMd5 = "1fb4cf7be57cb2c30ab2224ffac65ac2";
      nflogResource = R.raw.nflog_x86;
    } else if(cpu_abi.contains("mips")) {
      iptablesBinary = "iptables_mips";
      iptablesMd5 = "c208f8f9a6fa8d7b436c069b71299668";
      iptablesResource = R.raw.iptables_mips;
      grepBinary = "grep_mips";
      grepMd5 = "a29534a420f9eb9cc519088eacf6b7e7";
      grepResource = R.raw.grep_mips;
      nflogBinary = "nflog_mips";
      nflogMd5 = "21d507600aa498395f00a9daf37eeb02";
      nflogResource = R.raw.nflog_mips;
    } else {
      iptablesBinary = null;
      grepBinary = null;
      nflogBinary = null;
      return false;
    }
    return true;
  }

  public static String getIptablesBinary() {
    if(iptablesBinary == null) {
      getBinariesIdentifiers();
    }
    return iptablesBinary;
  }

  public static String getGrepBinary() {
    if(grepBinary == null) {
      getBinariesIdentifiers();
    }
    return grepBinary;
  }

  public static String getNflogBinary() {
    if(nflogBinary == null) {
      getBinariesIdentifiers();
    }
    return nflogBinary;
  }

  public static boolean installBinary(Context context, String binary, String md5sum, int resource, String path) {
    boolean needsInstall = false;
    File file = new File(path);

    MyLog.d("Checking for " + binary + " with md5sum " + md5sum);

    if(file.isFile()) {
      String hash = MD5Sum.digestFile(file);
      if(!hash.equals(md5sum)) {
        needsInstall = true;
      }
      MyLog.d(binary + " found with md5sum " + hash + "; needsInstall: " + needsInstall);
    } else {
      MyLog.d(binary + " does not exist.");
      needsInstall = true;
    }

    if(needsInstall) {
      try {
        MyLog.d(binary + " not found: installing to " + path);

        InputStream raw = context.getResources().openRawResource(resource);
        ZipInputStream zip = new ZipInputStream(raw);
        zip.getNextEntry();

        InputStream in = zip;
        FileOutputStream out = new FileOutputStream(path);

        byte buf[] = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) {
          out.write(buf, 0, len);
        }

        out.close();
        in.close();

        Runtime.getRuntime().exec("chmod 755 " + path).waitFor();
      } catch (Exception e) {
        Resources res = context.getResources();
        showError(context, res.getString(R.string.error_default_title), String.format(res.getString(R.string.error_install_binary_text), binary) + e.getMessage());
        return false;
      }
    } else {
      MyLog.d(binary + " found at " + path);
    }

    return true;
  }

  public static boolean installBinaries(Context context) {
    if(!getBinariesIdentifiers()) {
      Resources res = context.getResources();
      showError(context, res.getString(R.string.error_unsupported_system_title), String.format(res.getString(R.string.error_unsupported_system_text), Build.CPU_ABI));
      return false;
    }

    String iptablesPath = context.getFilesDir().getAbsolutePath() + File.separator + iptablesBinary;
    if(!installBinary(context, iptablesBinary, iptablesMd5, iptablesResource, iptablesPath)) {
      return false;
    }

    String grepPath  = context.getFilesDir().getAbsolutePath() + File.separator + grepBinary;
    if(!installBinary(context, grepBinary, grepMd5, grepResource, grepPath)) {
      return false;
    }

    String nflogPath  = context.getFilesDir().getAbsolutePath() + File.separator + nflogBinary;
    if(!installBinary(context, nflogBinary, nflogMd5, nflogResource, nflogPath)) {
      return false;
    }

    return true;
  }

  public static boolean checkRoot(Context context) {
    synchronized(NetworkLog.SCRIPT) {
      String scriptFile = context.getFilesDir().getAbsolutePath() + File.separator + NetworkLog.SCRIPT;

      try {
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(scriptFile)));
        script.println("exit 0");
        script.flush();
        script.close();
      } catch(java.io.IOException e) {
        Log.e("NetworkLog", "Check root error", e);
        return false;
      }

      ShellCommand cmd = new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "checkRoot");
      cmd.start(true);

      if(cmd.error != null) {
        Log.e("NetworkLog", "Failed check root (exit " + cmd.exitval + "): " + cmd.error);
        return false;
      } else if(cmd.exitval != 0) {
        Log.e("NetworkLog", "Failed check root (exit " + cmd.exitval + ")");
        return false;
      } else {
        Log.e("NetworkLog", "Check root passed");
        return true;
      }
    }
  }

  public static void showError(final Context context, final String title, final String message) {
    Log.d("NetworkLog", "Got error: [" + title + "] [" + message + "]");

    context.startActivity(new Intent(context, ErrorDialogActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra("title", title)
        .putExtra("message", message));
  }
}
