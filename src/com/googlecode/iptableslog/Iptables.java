package com.googlecode.iptableslog;

import android.util.Log;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;


public class Iptables {
  public static final String SCRIPT = "/sdcard/iptableslog.sh";
  public static final String[] CELL_INTERFACES = {
    "rmnet+", "ppp+", "pdp+"
  };

  public static final String[] WIFI_INTERFACES = {
    "eth+", "wlan+", "tiwlan+", "athwlan+"
  };

  public static boolean addRules() {
    if(checkRules() == true)
      removeRules();

    try {
      PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(SCRIPT)));

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
    } catch (java.io.IOException e) { Log.d("IptablesLog", "addRules error", e); }

    new ShellCommand(new String[] { "su", "-c", "sh " + SCRIPT }, "addRules").start(true);
    return true;
  }

  public static boolean removeRules() {
    int tries = 0;

    while(checkRules() == true) {
      try {
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(SCRIPT)));

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
      } catch (java.io.IOException e) { Log.d("IptablesLog", "removeRules error", e); } 

      new ShellCommand(new String[] { "su", "-c", "sh " + SCRIPT }, "removeRules").start(true);

      tries++;
      if(tries > 3) {
        MyLog.d("Too many attempts to remove rules, moving along...");
        return false;
      }
    }
    return true;
  }

  public static boolean checkRules() {
    try {
      PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(SCRIPT)));
      script.println("iptables -L");
      script.flush();
      script.close();
    } catch (java.io.IOException e) { Log.d("IptablesLog", "checkRules error", e); } 

    ShellCommand command = new ShellCommand(new String[] { "su", "-c", "sh " + SCRIPT }, "checkRules");
    command.start(false);
    String result = "";
    while(!command.checkForExit()) {
      result += command.readStdoutBlocking();
    }

    if(result == null)
      return true;

    MyLog.d("checkRules result: [" + result + "]");

    return result.indexOf("[IptablesLogEntry]", 0) == -1 ? false : true;
  }
}
