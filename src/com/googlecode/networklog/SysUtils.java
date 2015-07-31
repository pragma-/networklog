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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
  public static String run_pieBinary;
  public static String run_pieMd5;
  public static int run_pieResource;

  public static boolean getBinariesIdentifiers() {
    String cpu_abi = Build.CPU_ABI.toLowerCase();

    if(cpu_abi.contains("armeabi-v7") || cpu_abi.contains("arm64")) {
      iptablesBinary = "iptables_armv7";
      iptablesMd5 = "5515873b7ce1617f3d724a3332c2b947";  // iptables_armv7
      iptablesResource = R.raw.iptables_armv7;
      grepBinary = "grep_armv7";
      grepMd5 = "6d5f2d3b8d50cb4db918e8530f975b08";  // grep_armv7
      grepResource = R.raw.grep_armv7;
      nflogBinary = "nflog_armv7";
      nflogMd5 = "9d72441239afa8684e479d30d14feb75";  // nflog_armv7
      nflogResource = R.raw.nflog_armv7;
      run_pieBinary = "run_pie_armv7";
      run_pieMd5 = "2a2d61479adb6182f13b5363c5a59895";  // run_pie_armv7
      run_pieResource = R.raw.run_pie_armv7;
    } else if(cpu_abi.contains("armeabi")) {
      iptablesBinary = "iptables_armv5";
      iptablesMd5 = "50e39f66369344b692084a9563c185d4";  // iptables_armv5
      iptablesResource = R.raw.iptables_armv5;
      grepBinary = "grep_armv5";
      grepMd5 = "5200a181e0835a82e8c51790c2934353";  // grep_armv5
      grepResource = R.raw.grep_armv5;
      nflogBinary = "nflog_armv5";
      nflogMd5 = "aa700f638adcfc09dc03ee8b23cad43d";  // nflog_armv5
      nflogResource = R.raw.nflog_armv5;
      run_pieBinary = "run_pie_armv5";
      run_pieMd5 = "3aad21f6fb4933c6398ff92c008400c5";  // run_pie_armv5
      run_pieResource = R.raw.run_pie_armv5;
    } else if(cpu_abi.contains("x86")) {
      iptablesBinary = "iptables_x86";
      iptablesMd5 = "3e7090f93ae3964c98e16016b742acbc";  // iptables_x86
      iptablesResource = R.raw.iptables_x86;
      grepBinary = "grep_x86";
      grepMd5 = "1ce10f593f5824daf3f91dae29c2e6d6";  // grep_x86
      grepResource = R.raw.grep_x86;
      nflogBinary = "nflog_x86";
      nflogMd5 = "ce661bc02b64b6090d03807738020c98";  // nflog_x86
      nflogResource = R.raw.nflog_x86;
      run_pieBinary = "run_pie_x86";
      run_pieMd5 = "dc4699e92868d0da1cb35af89033c5df";  // run_pie_x86
      run_pieResource = R.raw.run_pie_x86;
    } else if(cpu_abi.contains("mips")) {
      iptablesBinary = "iptables_mips";
      iptablesMd5 = "c208f8f9a6fa8d7b436c069b71299668";  // iptables_mips
      iptablesResource = R.raw.iptables_mips;
      grepBinary = "grep_mips";
      grepMd5 = "8887e3bfb42ba335510d0adb739a0329";  // grep_mips
      grepResource = R.raw.grep_mips;
      nflogBinary = "nflog_mips";
      nflogMd5 = "534ce71da7e383183697c4090c295624";  // nflog_mips
      nflogResource = R.raw.nflog_mips;
      run_pieBinary = "run_pie_mips";
      run_pieMd5 = "ec8123a69e7527d243575d779a080b4a";  // run_pie_mips
      run_pieResource = R.raw.run_pie_mips;
    } else {
      iptablesBinary = null;
      grepBinary = null;
      nflogBinary = null;
      run_pieBinary = null;
      return false;
    }
    return true;
  }

  public static String getIptablesBinary(Context context) {
    if(Build.VERSION.SDK_INT >= 14) {
      // use system built-in binaries on >= ICS due to SELinux
      return "iptables";
    } else {
      if(iptablesBinary == null) {
        getBinariesIdentifiers();
      }
      return context.getFilesDir().getAbsolutePath() + File.separator + iptablesBinary;
    }
  }

  public static String getGrepBinary(Context context) {
    if(Build.VERSION.SDK_INT >= 14) {
      // use system built-in binaries on >= ICS due to SELinux
      return "grep";
    } else {
      if(grepBinary == null) {
        getBinariesIdentifiers();
      }
      if (Build.VERSION.SDK_INT < 16) {
        return getRun_pieBinary(context) + " " + context.getFilesDir().getAbsolutePath() + File.separator + grepBinary;
      } else {
        return context.getFilesDir().getAbsolutePath() + File.separator + grepBinary;
      }
    }
  }

  public static String getNflogBinary(Context context) {
    // FIXME: need some way to put nflog on system partition or
    // some other workaround for SELinux on >= ICS
    if(nflogBinary == null) {
      getBinariesIdentifiers();
    }
    if (Build.VERSION.SDK_INT < 16) {
      return getRun_pieBinary(context) + " " + context.getFilesDir().getAbsolutePath() + File.separator + nflogBinary;
    } else {
      return context.getFilesDir().getAbsolutePath() + File.separator + nflogBinary;
    }
  }

  public static String getRun_pieBinary(Context context) {
    // FIXME: need some way to put run_pie on system partition or
    // some other workaround for SELinux on >= ICS
    if(run_pieBinary == null) {
      getBinariesIdentifiers();
    }
    return context.getFilesDir().getAbsolutePath() + File.separator + run_pieBinary;
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

    String run_piePath  = context.getFilesDir().getAbsolutePath() + File.separator + run_pieBinary;
    if(!installBinary(context, run_pieBinary, run_pieMd5, run_pieResource, run_piePath)) {
      return false;
    }

    return true;
  }

  public static boolean checkRoot(Context context) {
    if(NetworkLog.shell == null || NetworkLog.shell.checkForExit()) {
      NetworkLog.shell = createRootShell(context, "CheckRootShell", false);

      if(NetworkLog.shell.hasError()) {
        Log.e("NetworkLog", "[check-root] Check root failed: " + NetworkLog.shell.getError(true));
        return false;
      }
    } 

    NetworkLog.shell.sendCommand("id");

    List<String> output = new ArrayList<String>();
    NetworkLog.shell.waitForCommandExit(output);

    if(NetworkLog.shell.exitval == 0) {
      for(String line : output) {
        line.trim();
        Log.d("NetworkLog", "[check-root] Got id output: [" + line + "]");
        if(line.startsWith("uid=0")) {
          Log.d("NetworkLog", "[check-root] Check root passed (uid=0)");
          return true;
        }
      }
      Log.e("NetworkLog", "[check-root] Check root failed (uid != 0)");
      return false;
    }

    Log.d("NetworkLog", "[check-root] Check root tentatively passed (no id command, but su succeeded)");
    return true;
  }

  public static InteractiveShell createRootShell(Context context, String tag, boolean showError) {
    InteractiveShell shell = new InteractiveShell("su", tag);
    shell.start();

    if(shell.hasError()) {
      if(showError) {
        Resources res = context.getResources();
        showError(context, res.getString(R.string.error_default_title), res.getString(R.string.error_noroot) + "\n\n" + shell.getError(false));
      }
    }
    return shell;
  }

  public static void showError(final Context context, final String title, final String message) {
    Log.d("NetworkLog", "Got error: [" + title + "] [" + message + "]");

    context.startActivity(new Intent(context, ErrorDialogActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra("title", title)
        .putExtra("message", message));
  }

  public static void applySamsungFix(final Context context) {
    if(Build.BRAND.toLowerCase().contains("samsung") || Build.MANUFACTURER.toLowerCase().contains("samsung")) {
      try {
        FileInputStream fis = context.openFileInput("samsung_fixed");
        // fix already applied
        fis.close();
        return;
      } catch (Exception e) { /* ignored */ }

      ShellCommand command = new ShellCommand(new String[] { "grep" }, "TestForGrep");
      command.start(true);

      Log.d("NetworkLog", "Test for grep exit val: " + command.exitval);

      if(command.exitval < 0 || command.exitval > 2) {
        // use cat if grep not found
        NetworkLog.settings.setLogMethod(2);
      } else {
        // use grep
        NetworkLog.settings.setLogMethod(1);
      }

      try {
        FileOutputStream fos = context.openFileOutput("samsung_fixed", Context.MODE_PRIVATE);
        fos.close();
        return;
      } catch (Exception e) {
        Log.w("NetworkLog", "Exception saving record of applying Samsung fix", e);
      }
    }
  }
}
