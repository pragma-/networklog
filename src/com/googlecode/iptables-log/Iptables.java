package com.googlecode.iptableslog;

public class Iptables {
  public static boolean startLog() {
    if(checkLog() == true)
      return false;

    ShellCommand command = new ShellCommand(new String[] { "su", "-c", "iptables -I OUTPUT 1 -o rmnet+ -j LOG --log-prefix \"[IptablesLogEntry]\" --log-uid" });
    command.start();
    while(command.checkForExit() == false)
      try { Thread.sleep(100); } catch (Exception e) { e.printStackTrace(); }

    command = new ShellCommand(new String[] { "su", "-c", "iptables -I INPUT 1 -i rmnet+ -j LOG --log-prefix \"[IptablesLogEntry]\" --log-uid" });
    command.start();
    while(command.checkForExit() == false)
      try { Thread.sleep(100); } catch (Exception e) { e.printStackTrace(); }

    return true;
  }

  public static boolean stopLog() {
    if(checkLog() == false)
      return false;

    ShellCommand command = new ShellCommand(new String[] { "su", "-c", "iptables -D INPUT 1" });
    command.start();
    while(command.checkForExit() == false)
      try { Thread.sleep(100); } catch (Exception e) { e.printStackTrace(); }

    command = new ShellCommand(new String[] { "su", "-c", "iptables -D OUTPUT 1" });
    command.start();
    while(command.checkForExit() == false)
      try { Thread.sleep(100); } catch (Exception e) { e.printStackTrace(); }

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
