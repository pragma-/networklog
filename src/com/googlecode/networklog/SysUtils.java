/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.content.Context;
import android.content.Intent;
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
  public static String busyboxBinary;
  public static int busyboxResource;

  public static boolean getBinariesIdentifiers() {
    String cpu_abi = Build.CPU_ABI.toLowerCase();

    if(cpu_abi.contains("armeabi")) {
      iptablesBinary = "iptables_armv5";
      iptablesResource = R.raw.iptables_armv5;
      busyboxBinary = "busybox_g1";
      busyboxResource = R.raw.busybox_g1;
    } else if(cpu_abi.contains("x86")) {
      iptablesBinary = "iptables_x86";
      iptablesResource = R.raw.iptables_x86;
      busyboxBinary = "busybox_x86";
      busyboxResource = R.raw.busybox_x86;
    } else if(cpu_abi.contains("mips")) {
      iptablesBinary = "iptables_mips";
      iptablesResource = R.raw.iptables_mips;
      busyboxBinary = "busybox_mips";
      busyboxResource = R.raw.busybox_mips;
    } else {
      iptablesBinary = null;
      busyboxBinary = null;
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

  public static String getBusyboxBinary() {
    if(busyboxBinary == null) {
      getBinariesIdentifiers();
    }
    return busyboxBinary;
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
        showError(context, "Network Log", "Install " + binary + " error: " + e.getMessage());
        return false;
      }
    } else {
      MyLog.d(binary + " found at " + path);
    }

    return true;
  }

  public static boolean installBinaries(Context context) {
    if(!getBinariesIdentifiers()) {
      showError(context, "Unsupported system", "The CPU type '" + Build.CPU_ABI + "' is currently unsupported. Please use the Bug Report/Feedback option to request support.");
      return false;
    }

    String iptablesPath = context.getFilesDir().getAbsolutePath() + File.separator + iptablesBinary;
    if(!installBinary(context, iptablesBinary, iptablesResource, iptablesPath)) {
      return false;
    }

    String busyboxPath  = context.getFilesDir().getAbsolutePath() + File.separator + busyboxBinary;
    if(!installBinary(context, busyboxBinary, busyboxResource, busyboxPath)) {
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
