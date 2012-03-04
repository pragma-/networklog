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

public class IptablesLogTracker {
  static Hashtable<String, LogEntry> logEntriesHash = new Hashtable<String, LogEntry>();
  static ArrayList<LogEntry> logEntriesList = new ArrayList<LogEntry>();
  static ArrayList<IptablesLogListener> listenerList = new ArrayList<IptablesLogListener>();
  static String localIpAddr;

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
    Log.d("IptablesLog", "getLocalIpAddress");
    try {
      for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
        NetworkInterface intf = en.nextElement();
        Log.d("IptablesLog", intf.toString());
        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          Log.d("IptablesLog", inetAddress.toString());
          if (!inetAddress.isLoopbackAddress()) {
            Log.d("IptablesLog", inetAddress.getHostAddress().toString());
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
    Log.d("IptablesLog", "parsing result");
    int pos = 0;
    String src, dst, len, spt, dpt, uid;

    while((pos = result.indexOf("[IptablesLogEntry]", pos)) > -1) {
      int newline = result.indexOf("\n", pos);

      Log.d("IptablesLog", "got [IptablesLogEntry] at " + pos);

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

      Log.d("IptablesLog", "entry uid: " + entry.uid + " " + entry.src + " " + entry.spt + " " + entry.dst + " " + entry.dpt + " " + entry.len + " " + entry.bytes + " " + entry.timestamp);

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

  public static void start() {
    Thread mainLoop = new Thread() {
      public void run() {
        Log.d("IptablesLog", "adding logging rules");
        Iptables.startLog();

        localIpAddr = getLocalIpAddress();

        try {
          PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(Iptables.SCRIPT)));
          script.println("cat /proc/kmsg");
          script.close();
        } catch (java.io.IOException e) { e.printStackTrace(); }

        Log.d("IptablesLog", "starting cat /proc/kmsg");
        ShellCommand command = new ShellCommand(new String[] { "su", "-c", "sh " + Iptables.SCRIPT });
        command.start(false);

        String result;
        while(command.checkForExit() == false) {
          Log.d("IptablesLog", "reading stdout");
          result = command.readStdout();
          if(result == null) {
            Log.d("IptablesLog", "result == null");
            return;
          }

          Log.d("IptablesLog", "result == [" + result + "]");
          parseResult(result);
        }
      }
    };

    mainLoop.start();
  }
}
