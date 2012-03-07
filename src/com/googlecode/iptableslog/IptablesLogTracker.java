package com.googlecode.iptableslog;

import android.util.Log;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.lang.Thread;

public class IptablesLogTracker {
  static Hashtable<String, LogEntry> logEntriesHash = new Hashtable<String, LogEntry>();
  static ArrayList<LogEntry> logEntriesList = new ArrayList<LogEntry>();
  static ArrayList<IptablesLogListener> listenerList = new ArrayList<IptablesLogListener>();
  static String localIpAddr;
  static ShellCommand command;

  static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

  public static class LogEntry {
    String uid;
    String src;
    String dst;
    int len;
    int spt;
    int dpt;
    int packets;
    int bytes;
    String timestamp;
  }

  public static String getTimestamp() {
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
    return format.format(cal.getTime());
  }

  public static String getLocalIpAddress() {
    MyLog.d("getLocalIpAddress");
    try {
      for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
        NetworkInterface intf = en.nextElement();
        MyLog.d(intf.toString());
        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          MyLog.d(inetAddress.toString());
          if (!inetAddress.isLoopbackAddress()) {
            MyLog.d(inetAddress.getHostAddress().toString());
            return inetAddress.getHostAddress().toString();
          }
        }
      }
    } catch (SocketException ex) {
      Log.e("IptablesLog", ex.toString());
    }
    return "none";
  }

  // FIXME: Needs buffering of incomplete logs
  public static void parseResult(String result) {
    MyLog.d("parsing result");
    int pos = 0; 
    String src, dst, len, spt, dpt, uid;

    while((pos = result.indexOf("[IptablesLogEntry]", pos)) > -1) {
      int newline = result.indexOf("\n", pos);

      MyLog.d("got [IptablesLogEntry] at " + pos);

      pos = result.indexOf("SRC=", pos);
      if(pos == -1) break;
      int space = result.indexOf(" ", pos);
      if(space == -1) break;
      src = result.substring(pos + 4, space);

      pos = result.indexOf("DST=", pos);
      if(pos == -1) break;
      space = result.indexOf(" ", pos);
      if(space == -1) break;
      dst = result.substring(pos + 4, space);
      
      pos = result.indexOf("LEN=", pos);
      if(pos == -1) break;
      space = result.indexOf(" ", pos);
      if(space == -1) break;
      len = result.substring(pos + 4, space);
     
      pos = result.indexOf("SPT=", pos);
      if(pos == -1) break;
      space = result.indexOf(" ", pos);
      if(space == -1) break;
      spt = result.substring(pos + 4, space);
    
      pos = result.indexOf("DPT=", pos);
      if(pos == -1) break;
      space = result.indexOf(" ", pos);
      if(space == -1) break;
      dpt = result.substring(pos + 4, space);

      pos = result.indexOf("UID=", pos);
      if(pos == -1) break;
      space = result.indexOf(" ", pos);
      if(space == -1) break;
      uid = result.substring(pos + 4, space);

      LogEntry entry = logEntriesHash.get(uid);

      if(entry == null)
        entry = new LogEntry();

      entry.uid = uid;
      entry.src = src;
      entry.dst = dst;
      entry.spt = Integer.parseInt(spt);
      entry.dpt = Integer.parseInt(dpt);
      entry.len = Integer.parseInt(len);
      entry.packets++;
      entry.bytes += entry.len;
      entry.timestamp = new String(getTimestamp());

      logEntriesHash.put(uid, entry);
      logEntriesList.add(entry);

      MyLog.d("entry uid: " + entry.uid + " " + entry.src + " " + entry.spt + " " + entry.dst + " " + entry.dpt + " " + entry.len + " " + entry.bytes + " " + entry.timestamp);

      notifyNewEntry(entry);
    }
  }

  public static void notifyNewEntry(LogEntry entry) {
    for(IptablesLogListener listener : listenerList) {
      listener.onNewLogEntry(entry);
    }
  }

  public static void addListener(IptablesLogListener listener) {
    listenerList.add(listener);
  }

  public static void restoreData(IptablesLogData data) {
    command = data.iptablesLogTrackerCommand;
  }

  public static void stop() {
    try {
      PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(Iptables.SCRIPT)));
      script.println("ps");
      script.close();
    } catch (java.io.IOException e) { e.printStackTrace(); }

    ShellCommand command = new ShellCommand(new String[] { "su", "-c", "sh " + Iptables.SCRIPT }, "FindLogTracker");
    command.start(false);
    String result = "";
    while(!command.checkForExit()) {
      result += command.readStdoutBlocking();
    }

    if(result == null)
      return;

    int iptableslog_pid = -1;

    for(String line : result.split("\n")) {
      MyLog.d("ps - parsing line [" + line + "]");
      String tokens[] = line.split("\\s+");
      String cmd = tokens[tokens.length - 1];
      int pid, ppid;

      try {
        pid = Integer.parseInt(tokens[1]);
        ppid = Integer.parseInt(tokens[2]);
      } catch(Exception e) {
        Log.d("IptablesLog", "Ignoring exception...", e);
        continue;
      }

      MyLog.d("cmd: " + cmd + "; pid: " + pid + "; ppid: " + ppid);

      if(cmd.equals("com.googlecode.iptableslog")) {
        iptableslog_pid = pid;
        MyLog.d("IptablesLog pid: " + iptableslog_pid);
        continue;
      }

      if(ppid == iptableslog_pid) {
        MyLog.d(cmd + " is our child");
        iptableslog_pid = pid;

        if(cmd.equals("grep")) {
          MyLog.d("Killing tracker " + pid);

          try {
            PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(Iptables.SCRIPT)));
            script.println("kill " + pid);
            script.close();
          } catch (java.io.IOException e) { e.printStackTrace(); }

          new ShellCommand(new String[] { "su", "-c", "sh " + Iptables.SCRIPT }, "KillLogTracker").start(true);
          break;
        }
      }
    }
  }

  public static void start(final boolean resumed) {
    localIpAddr = getLocalIpAddress();

    if(!resumed) {
      MyLog.d("adding logging rules");
      Iptables.addRules();

      try {
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(Iptables.SCRIPT)));
        script.println("grep IptablesLogEntry /proc/kmsg");
        script.close();
      } catch (java.io.IOException e) { e.printStackTrace(); }

      MyLog.d("Starting iptables log tracker");

      command = new ShellCommand(new String[] { "su", "-c", "sh " + Iptables.SCRIPT }, "IptablesLogTracker");
      command.start(false);
    } else {
      MyLog.d("Resuming iptables log tracker");
      command = IptablesLog.data.iptablesLogTrackerCommand;
    }

    Thread mainLoop = new Thread() {
      public void run() {

        String result;
        while(command.checkForExit() == false) {
          MyLog.d("checking stdout");
          result = command.readStdoutBlocking();
          if(result == null) {
            MyLog.d("result == null");
            Iptables.removeRules();
            System.exit(0);
          }

          MyLog.d("result == [" + result + "]");
          parseResult(result);
        }
        Iptables.removeRules();
        System.exit(0);
      }
    };

    mainLoop.start();
  }
}
