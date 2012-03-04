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

  public static boolean startLog() {
    if(checkLog() == true)
      stopLog();

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

      script.close();
    } catch (java.io.IOException e) { e.printStackTrace(); }

    new ShellCommand(new String[] { "su", "-c", "sh " + SCRIPT }).start(true);
    return true;
  }

  public static boolean stopLog() {
    if(checkLog() == false)
      return false;

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

      script.close();
    } catch (java.io.IOException e) { e.printStackTrace(); } 

    new ShellCommand(new String[] { "su", "-c", "sh " + SCRIPT }).start(true);
    return true;
  }

  public static boolean checkLog() {
    try {
      PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(SCRIPT)));
      script.println("iptables -L");
      script.close();
    } catch (java.io.IOException e) { e.printStackTrace(); } 

    ShellCommand command = new ShellCommand(new String[] { "su", "-c", "sh " + SCRIPT });
    command.start(false);
    Log.d("IptablesLog", "checklog");
    String result = command.readStdout();
    command.finish();

    if(result == null)
      return true;

    return result.indexOf("[IptablesLogEntry]", 0) == -1 ? false : true;
  }
}
