package com.googlecode.iptableslog;

public class Iptables {
  public static final String[] CELL_INTERFACES = {
    "rmnet+", "ppp+", "pdp+"
  };

  public static final String[] WIFI_INTERFACES = {
    "eth+", "wlan+", "tiwlan+", "athwlan+"
  };

  public static boolean startLog() {
    if(checkLog() == true)
      return false;

    for(String iface : CELL_INTERFACES) {
      ShellCommand command = new ShellCommand(new String[] { "su", "-c", "iptables -I OUTPUT 1 -o " + iface + " -j LOG --log-prefix \"[IptablesLogEntry]\" --log-uid" });
      command.start();
      while(command.checkForExit() == false)
        try { Thread.sleep(100); } catch (Exception e) { e.printStackTrace(); }

      command = new ShellCommand(new String[] { "su", "-c", "iptables -I INPUT 1 -i " + iface + " -j LOG --log-prefix \"[IptablesLogEntry]\" --log-uid" });
      command.start();
      while(command.checkForExit() == false)
        try { Thread.sleep(100); } catch (Exception e) { e.printStackTrace(); }
    }

    for(String iface : WIFI_INTERFACES) {
      ShellCommand command = new ShellCommand(new String[] { "su", "-c", "iptables -I OUTPUT 1 -o " + iface + " -j LOG --log-prefix \"[IptablesLogEntry]\" --log-uid" });
      command.start();
      while(command.checkForExit() == false)
        try { Thread.sleep(100); } catch (Exception e) { e.printStackTrace(); }

      command = new ShellCommand(new String[] { "su", "-c", "iptables -I INPUT 1 -i " + iface + " -j LOG --log-prefix \"[IptablesLogEntry]\" --log-uid" });
      command.start();
      while(command.checkForExit() == false)
        try { Thread.sleep(100); } catch (Exception e) { e.printStackTrace(); }
    }

    return true;
  }

  public static boolean stopLog() {
    if(checkLog() == false)
      return false;

    for(String iface : CELL_INTERFACES) {
      ShellCommand command = new ShellCommand(new String[] { "su", "-c", "iptables -D INPUT 1" });
      command.start();
      while(command.checkForExit() == false)
        try { Thread.sleep(100); } catch (Exception e) { e.printStackTrace(); }

      command = new ShellCommand(new String[] { "su", "-c", "iptables -D OUTPUT 1" });
      command.start();
      while(command.checkForExit() == false)
        try { Thread.sleep(100); } catch (Exception e) { e.printStackTrace(); }
    }

    for(String iface : WIFI_INTERFACES) {
      ShellCommand command = new ShellCommand(new String[] { "su", "-c", "iptables -D INPUT 1" });
      command.start();
      while(command.checkForExit() == false)
        try { Thread.sleep(100); } catch (Exception e) { e.printStackTrace(); }

      command = new ShellCommand(new String[] { "su", "-c", "iptables -D OUTPUT 1" });
      command.start();
      while(command.checkForExit() == false)
        try { Thread.sleep(100); } catch (Exception e) { e.printStackTrace(); }
    }
    return true;
  }

  public static boolean checkLog() {
    ShellCommand command = new ShellCommand(new String[] { "su", "-c", "iptables -L" });
    command.start();
    String result = command.readStdout();
    command.finish();

    return result.indexOf("[IptablesLogEntry]", 0) == -1 ? false : true;
  }
}
