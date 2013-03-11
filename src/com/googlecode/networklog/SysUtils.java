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
  public static int iptablesResource;
  public static String grepBinary;
  public static int grepResource;
  public static String nflogBinary;
  public static int nflogResource;

  public static boolean getBinariesIdentifiers() {
    String cpu_abi = Build.CPU_ABI.toLowerCase();

    if(cpu_abi.contains("armeabi-v7")) {
      iptablesBinary = "iptables_armv7";
      iptablesResource = R.raw.iptables_armv7;
      grepBinary = "grep_armv7";
      grepResource = R.raw.grep_armv7;
      nflogBinary = "nflog_armv7";
      nflogResource = R.raw.nflog_armv7;
    } else if(cpu_abi.contains("armeabi")) {
      iptablesBinary = "iptables_armv5";
      iptablesResource = R.raw.iptables_armv5;
      grepBinary = "grep_armv5";
      grepResource = R.raw.grep_armv5;
      nflogBinary = "nflog_armv5";
      nflogResource = R.raw.nflog_armv5;
    } else if(cpu_abi.contains("x86")) {
      iptablesBinary = "iptables_x86";
      iptablesResource = R.raw.iptables_x86;
      grepBinary = "grep_x86";
      grepResource = R.raw.grep_x86;
      nflogBinary = "nflog_x86";
      nflogResource = R.raw.nflog_x86;
    } else if(cpu_abi.contains("mips")) {
      iptablesBinary = "iptables_mips";
      iptablesResource = R.raw.iptables_mips;
      grepBinary = "grep_mips";
      grepResource = R.raw.grep_mips;
      nflogBinary = "nflog_mips";
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

  public static boolean installBinary(Context context, String binary, int resource, String path) {
    if(!new File(path).isFile()) {
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
    if(!installBinary(context, iptablesBinary, iptablesResource, iptablesPath)) {
      return false;
    }

    String grepPath  = context.getFilesDir().getAbsolutePath() + File.separator + grepBinary;
    if(!installBinary(context, grepBinary, grepResource, grepPath)) {
      return false;
    }

    String nflogPath  = context.getFilesDir().getAbsolutePath() + File.separator + nflogBinary;
    if(!installBinary(context, nflogBinary, nflogResource, nflogPath)) {
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
      }

      String error = new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "checkRoot").start(true);

      if(error != null) {
        Log.d("[NetworkLog]", "Failed check root: " + error);
        return false;
      } else {
        Log.d("[NetworkLog]", "Check root passed");
        return true;
      }
    }
  }

  public static void showError(final Context context, final String title, final String message) {
    MyLog.d("Got error: [" + title + "] [" + message + "]");

    context.startActivity(new Intent(context, ErrorDialogActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra("title", title)
        .putExtra("message", message));
  }
}
