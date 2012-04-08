package com.googlecode.iptableslog;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;


public class Iptables {
  public static final String SCRIPT = "iptableslog.sh";

  public static final String[] CELL_INTERFACES = {
    "rmnet+", "ppp+", "pdp+"
  };

  public static final String[] WIFI_INTERFACES = {
    "eth+", "wlan+", "tiwlan+", "athwlan+"
  };

  public static boolean addRules(Context context) {
    if(checkRules(context) == true) {
      removeRules(context);
    }

    synchronized(IptablesLog.scriptLock) {
      String scriptFile = new ContextWrapper(context).getFilesDir().getAbsolutePath() + File.separator + SCRIPT;

      try {
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(scriptFile)));

        for(String iface : CELL_INTERFACES) {
          script.println("iptables -I OUTPUT 1 -o " + iface + " -j LOG --log-prefix \"[IptablesLogEntry]\" --log-uid");

          script.println("iptables -I INPUT 1 -i " + iface + " -j LOG --log-prefix \"[IptablesLogEntry]\" --log-uid");
        }

        for(String iface : WIFI_INTERFACES) {
          script.println("iptables -I OUTPUT 1 -o " + iface + " -j LOG --log-prefix \"[IptablesLogEntry]\" --log-uid");

          script.println("iptables -I INPUT 1 -i " + iface + " -j LOG --log-prefix \"[IptablesLogEntry]\" --log-uid");
        }

        script.flush();
        script.close();
      } catch(java.io.IOException e) {
        Log.d("IptablesLog", "addRules error", e);
      }

      new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "addRules").start(true);
    }

    return true;
  }

  public static boolean removeRules(Context context) {
    int tries = 0;

    while(checkRules(context) == true) {
      synchronized(IptablesLog.scriptLock) {
        String scriptFile = new ContextWrapper(context).getFilesDir().getAbsolutePath() + File.separator + SCRIPT;

        try {
          PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(scriptFile)));

          for(String iface : CELL_INTERFACES) {
            script.println("iptables -D OUTPUT -o " + iface + " -j LOG --log-prefix \"[IptablesLogEntry]\" --log-uid");

            script.println("iptables -D INPUT -i " + iface + " -j LOG --log-prefix \"[IptablesLogEntry]\" --log-uid");
          }

          for(String iface : WIFI_INTERFACES) {
            script.println("iptables -D OUTPUT -o " + iface + " -j LOG --log-prefix \"[IptablesLogEntry]\" --log-uid");

            script.println("iptables -D INPUT -i " + iface + " -j LOG --log-prefix \"[IptablesLogEntry]\" --log-uid");
          }

          script.flush();
          script.close();
        } catch(java.io.IOException e) {
          Log.d("IptablesLog", "removeRules error", e);
        }

        new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "removeRules").start(true);

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
    synchronized(IptablesLog.scriptLock) {
      String scriptFile = new ContextWrapper(context).getFilesDir().getAbsolutePath() + File.separator + SCRIPT;

      try {
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(scriptFile)));
        script.println("iptables -L");
        script.flush();
        script.close();
      } catch(java.io.IOException e) {
        Log.d("IptablesLog", "checkRules error", e);
      }

      ShellCommand command = new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "checkRules");
      command.start(false);
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

      MyLog.d("checkRules result: [" + result + "]");

      return result.indexOf("[IptablesLogEntry]", 0) == -1 ? false : true;
    }
  }
}
