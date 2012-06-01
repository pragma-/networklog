package com.googlecode.networklog;

import android.content.Context;
import android.util.Log;
import android.content.Intent;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.BufferedWriter;


public class Iptables {
  public static final String SCRIPT = "networklog.sh";

  public static final String[] CELL_INTERFACES = {
    "rmnet+", "ppp+", "pdp+", "pnp+", "rmnet_sdio+", "uwbr+", "wimax+", "vsnet+", "usb+", "ccmni+"
  };

  public static final String[] WIFI_INTERFACES = {
    "eth+", "wlan+", "tiwlan+", "athwlan+", "ra+"
  };

  public static boolean checkRoot(Context context) {
    synchronized(NetworkLog.scriptLock) {
      String scriptFile = context.getFilesDir().getAbsolutePath() + File.separator + SCRIPT;

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

  public static void installBinaries(Context context) {
    String iptablesPath = context.getFilesDir().getAbsolutePath() + File.separator + "iptables_armv5";
    String busyboxPath  = context.getFilesDir().getAbsolutePath() + File.separator + "busybox_g1";

    if(!new File(iptablesPath).isFile()) {
      try {
        MyLog.d("iptables_armv5 not found: installing to " + iptablesPath);

        final FileOutputStream out = new FileOutputStream(iptablesPath);
        final InputStream in = context.getResources().openRawResource(R.raw.iptables_armv5);
        byte buf[] = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }
        out.close();
        in.close();
        Runtime.getRuntime().exec("chmod 755 " + iptablesPath).waitFor();
      } catch (Exception e) {
        showError(context, "Network Log", "Install iptables error: " + e.getMessage());
      }
    } else {
      MyLog.d("iptables_armv5 found at " + iptablesPath);
    }

    if(!new File(busyboxPath).isFile()) {
      MyLog.d("busybox_g1 not found: installing to " + busyboxPath);

      try {
        final FileOutputStream out = new FileOutputStream(busyboxPath);
        final InputStream in = context.getResources().openRawResource(R.raw.busybox_g1);
        byte buf[] = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }
        out.close();
        in.close();
        Runtime.getRuntime().exec("chmod 755 " + busyboxPath).waitFor();
      } catch (Exception e) {
        showError(context, "Network Log", "Install busybox error: " + e.getMessage());
      }
    } else {
      MyLog.d("busybox_g1 found at " + busyboxPath);
    }
  }

  public static boolean addRules(Context context) {
    if(checkRules(context) == true) {
      removeRules(context);
    }

    synchronized(NetworkLog.scriptLock) {
      String scriptFile = context.getFilesDir().getAbsolutePath() + File.separator + SCRIPT;
      String iptables  = context.getFilesDir().getAbsolutePath() + File.separator + "iptables_armv5";

      try {
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(scriptFile)));

        for(String iface : CELL_INTERFACES) {
          script.println(iptables + " -I OUTPUT 1 -o " + iface + " -j LOG --log-prefix \"[NetworkLogEntry]\" --log-uid");

          script.println(iptables + " -I INPUT 1 -i " + iface + " -j LOG --log-prefix \"[NetworkLogEntry]\" --log-uid");
        }

        for(String iface : WIFI_INTERFACES) {
          script.println(iptables + " -I OUTPUT 1 -o " + iface + " -j LOG --log-prefix \"[NetworkLogEntry]\" --log-uid");

          script.println(iptables + " -I INPUT 1 -i " + iface + " -j LOG --log-prefix \"[NetworkLogEntry]\" --log-uid");
        }

        script.flush();
        script.close();
      } catch(java.io.IOException e) {
        Log.e("NetworkLog", "addRules error", e);
      }

      String error = new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "addRules").start(true);

      if(error != null) {
        showError(context, "Add rules error", error);
        return false;
      }
    }

    return true;
  }

  public static boolean removeRules(Context context) {
    String iptables  = context.getFilesDir().getAbsolutePath() + File.separator + "iptables_armv5";
    int tries = 0;

    while(checkRules(context) == true) {
      synchronized(NetworkLog.scriptLock) {
        String scriptFile = context.getFilesDir().getAbsolutePath() + File.separator + SCRIPT;

        try {
          PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(scriptFile)));

          for(String iface : CELL_INTERFACES) {
            script.println(iptables + " -D OUTPUT -o " + iface + " -j LOG --log-prefix \"[NetworkLogEntry]\" --log-uid");

            script.println(iptables + " -D INPUT -i " + iface + " -j LOG --log-prefix \"[NetworkLogEntry]\" --log-uid");
          }

          for(String iface : WIFI_INTERFACES) {
            script.println(iptables + " -D OUTPUT -o " + iface + " -j LOG --log-prefix \"[NetworkLogEntry]\" --log-uid");

            script.println(iptables + " -D INPUT -i " + iface + " -j LOG --log-prefix \"[NetworkLogEntry]\" --log-uid");
          }

          script.flush();
          script.close();
        } catch(java.io.IOException e) {
          Log.e("NetworkLog", "removeRules error", e);
        }

        String error = new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "removeRules").start(true);

        if(error != null) {
          showError(context, "Remove rules error", error);
          return false;
        }

        tries++;

        if(tries > 3) {
          MyLog.d("Too many attempts to remove rules, moving along...");
          return false;
        }
      }
    }

    return true;
  }

  public static boolean checkRules(Context context) {
    String iptables  = context.getFilesDir().getAbsolutePath() + File.separator + "iptables_armv5";
    synchronized(NetworkLog.scriptLock) {
      String scriptFile = context.getFilesDir().getAbsolutePath() + File.separator + SCRIPT;

      try {
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(scriptFile)));
        script.println(iptables + " -L");
        script.flush();
        script.close();
      } catch(java.io.IOException e) {
        Log.e("NetworkLog", "checkRules error", e);
      }

      ShellCommand command = new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "checkRules");
      String error = command.start(false);

      if(error != null) {
        showError(context, "Check rules error", error);
        return false;
      }

      StringBuilder result = new StringBuilder();

      while(true) {
        String line = command.readStdoutBlocking();

        if(line == null) {
          break;
        }

        result.append(line);
      }

      if(result == null) {
        return true;
      }

      command.checkForExit();
      if(command.exit != 0) {
        showError(context, "Check rules error", result.toString());
        return false;
      }

      MyLog.d("checkRules result: [" + result + "]");

      return result.indexOf("[NetworkLogEntry]", 0) == -1 ? false : true;
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
