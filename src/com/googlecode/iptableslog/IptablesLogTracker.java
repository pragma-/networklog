package com.googlecode.iptableslog;

import android.util.Log;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.lang.Thread;
import java.lang.Runnable;

public class IptablesLogTracker {
  Hashtable<String, LogEntry> logEntriesHash;
  HashMap<String, Integer> logEntriesMap;
  ArrayList<IptablesLogListener> listenerList;
  static ArrayList<String> localIpAddrs;
  ShellCommand command;
  StringBuilder buffer;
  LogTracker logTrackerRunner;

  final static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
  static SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);

  public class LogEntry {
    int uid;
    String src;
    String dst;
    int len;
    int spt;
    int dpt;
    int packets;
    int bytes;
    String timestampString;
    long timestamp;
    boolean dirty;
  }

  public IptablesLogTracker() {
    listenerList = new ArrayList<IptablesLogListener>();

    if(IptablesLog.data == null) {
      logEntriesHash = new Hashtable<String, LogEntry>();
      logEntriesMap = new HashMap<String, Integer>();
      buffer = new StringBuilder(8192 * 2);

      initEntriesMap();
    } else {
      logEntriesHash = IptablesLog.data.iptablesLogTrackerLogEntriesHash;
      logEntriesMap = IptablesLog.data.iptablesLogTrackerLogEntriesMap;
      buffer = IptablesLog.data.iptablesLogTrackerBuffer;
      command = IptablesLog.data.iptablesLogTrackerCommand;
    }
  }

  public static String getTimestamp() {
    return format.format(new Date());
  }

  public void initEntriesMap() {
    NetStat netstat = new NetStat();
    ArrayList<NetStat.Connection> connections = netstat.getConnections();

    for(NetStat.Connection connection : connections) {
      String mapKey = connection.src + ":" + connection.spt + " -> " + connection.dst + ":" + connection.dpt;
      MyLog.d("[netstat src-dst] New entry " + connection.uid + " for [" + mapKey + "]");
      logEntriesMap.put(mapKey, Integer.valueOf(connection.uid));

      mapKey = connection.dst + ":" + connection.dpt + " -> " + connection.src + ":" + connection.spt;
      MyLog.d("[netstat dst-src] New entry " + connection.uid + " for [" + mapKey + "]");
      logEntriesMap.put(mapKey, Integer.valueOf(connection.uid));
    }
  }

  public static void getLocalIpAddresses() {
    MyLog.d("getLocalIpAddresses");
    localIpAddrs = new ArrayList<String>();

    try 
    {
      for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
      {
        NetworkInterface intf = en.nextElement();
        MyLog.d(intf.toString());
        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
        {
          InetAddress inetAddress = enumIpAddr.nextElement();
          MyLog.d(inetAddress.toString());
          if (!inetAddress.isLoopbackAddress())
          {
            MyLog.d("Adding local IP address: [" + inetAddress.getHostAddress().toString() + "]");
            localIpAddrs.add(inetAddress.getHostAddress().toString());
          }
        }
      }
    } catch (SocketException ex) {
      Log.e("IptablesLog", ex.toString());
    }
  }

  public void parseResult(String result) {
    MyLog.d("--------------- parsing result --------------");
    int pos = 0 /* , buffer_pos = 0 */; 
    String src, dst, lenString, sptString, dptString, uidString;

    /*
    if(MyLog.enabled) {
      MyLog.d("buffer length: " + buffer.length() + "; result length: " + result.length());
      MyLog.d("buffer: [" + buffer + "] <--> [" + result + "]");
    }

    buffer.append(result);
    */

    while((pos = result.indexOf("[IptablesLogEntry]", pos)) > -1) {
      MyLog.d("---- got [IptablesLogEntry] at " + pos + " ----");
      // buffer_pos = pos;

      pos += 18; // skip past "[IptablesLogEntry]"

      int newline = result.indexOf("\n", pos);
      int nextEntry = result.indexOf("[IptablesLogEntry]", pos);

      if(nextEntry != -1 && nextEntry < newline) {
        MyLog.d("Skipping corrupted entry");
        continue;
      }

      /*
      MyLog.d("pos: " + pos + "; buffer length: " + buffer.length());
      String line = "no newline: " + buffer.substring(pos, buffer.length());

      if(newline != -1) {
        line = buffer.substring(pos, newline - 1);
      }

      MyLog.d("parsing line [" + line + "]");
      */

      pos = result.indexOf("SRC=", pos);
      if(pos == -1 || pos > newline) { /* MyLog.d("buffering [" + line + "] for SRC");  */ break; };
      int space = result.indexOf(" ", pos);
      if(space == -1 || space > newline) { /* MyLog.d("buffering [" + line + "] for SRC space");  */ break; };
      src = result.substring(pos + 4, space);

      pos = result.indexOf("DST=", pos);
      if(pos == -1 || pos > newline) { /* MyLog.d("buffering [" + line + "] for DST");  */ break; };
      space = result.indexOf(" ", pos);
      if(space == -1 || space > newline) { /* MyLog.d("buffering [" + line + "] for DST space");  */ break; };
      dst = result.substring(pos + 4, space);
      
      pos = result.indexOf("LEN=", pos);
      if(pos == -1 || pos > newline) { /* MyLog.d("buffering [" + line + "] for LEN");  */ break; };
      space = result.indexOf(" ", pos);
      if(space == -1 || space > newline) { /* MyLog.d("buffering [" + line + "] for LEN space");  */ break; };
      lenString = result.substring(pos + 4, space);
     
      pos = result.indexOf("SPT=", pos);
      if(pos == -1 || pos > newline) { /* MyLog.d("buffering [" + line + "] for SPT");  */ break; };
      space = result.indexOf(" ", pos);
      if(space == -1 || space > newline) { /* MyLog.d("buffering [" + line + "] for SPT space");  */ break; };
      sptString = result.substring(pos + 4, space);
    
      pos = result.indexOf("DPT=", pos);
      if(pos == -1 || pos > newline) { /* MyLog.d("buffering [" + line + "] for DPT");  */ break; };
      space = result.indexOf(" ", pos);
      if(space == -1 || space > newline) { /* MyLog.d("buffering [" + line + "] for DPT space");  */ break; };
      dptString = result.substring(pos + 4, space);

      int lastpos = pos;
      pos = result.indexOf("UID=", pos);
      // MyLog.d("newline pos: " + newline + "; UID pos: " + pos);
      if(pos == -1 || (pos > newline && newline != -1)) {
        uidString = "-1";
        pos = lastpos;
      } else {
        MyLog.d("Looking for UID newline"); 
        if(newline == -1) { /* MyLog.d("buffering [" + line + "] for UID newline");  */ break; };
        uidString = result.substring(pos + 4, newline);
      }

      int uid;
      int spt;
      int dpt;
      int len;

      try {
        uid = Integer.parseInt(uidString.split("[^0-9-]+")[0]);
      } catch (Exception e) {
        Log.e("IptablesLog", "Bad data for uid: [" + uidString + "]", e);
        uid = -13;
      }

      try {
        spt = Integer.parseInt(sptString.split("[^0-9-]+")[0]);
      } catch (Exception e) {
        Log.e("IptablesLog", "Bad data for spt: [" + sptString + "]", e);
        spt = -1;
      }

      try {
        dpt = Integer.parseInt(dptString.split("[^0-9-]+")[0]);
      } catch (Exception e) {
        Log.e("IptablesLog", "Bad data for dpt: [" + dptString + "]", e);
        dpt = -1;
      }

      try {
        len = Integer.parseInt(lenString.split("[^0-9-]+")[0]);
      } catch (Exception e) {
        Log.e("IptablesLog", "Bad data for len: [" + lenString + "]", e);
        len = -1;
      }

      String srcDstMapKey = src + ":" + spt + " -> " + dst + ":" + dpt; 
      String dstSrcMapKey = dst + ":" + dpt + " -> " + src + ":" + spt; 

      MyLog.d("Checking entry for " + uid + " " + srcDstMapKey + " and " + dstSrcMapKey);
      Integer srcDstMapUid = logEntriesMap.get(srcDstMapKey);
      Integer dstSrcMapUid = logEntriesMap.get(dstSrcMapKey);

      if(uid < 0) {
        // Unknown uid, retrieve from entries map
        MyLog.d("Unknown uid");

        if(srcDstMapUid == null || dstSrcMapUid == null) {
          // refresh netstat and try again
          MyLog.d("Refreshing netstat ...");
          initEntriesMap();
          srcDstMapUid = logEntriesMap.get(srcDstMapKey);
          dstSrcMapUid = logEntriesMap.get(dstSrcMapKey);
        }

        if(srcDstMapUid == null) {
          MyLog.d("[src-dst] No entry uid for " + uid + " [" + srcDstMapKey + "]");
          if(uid == -1) {
            if(dstSrcMapUid != null) {
              MyLog.d("[dst-src] Reassigning kernel packet -1 to " + dstSrcMapUid);
              uid = dstSrcMapUid;
            } else {
              MyLog.d("[src-dst] New kernel entry -1 for [" + srcDstMapKey + "]");
              srcDstMapUid = uid;
              logEntriesMap.put(srcDstMapKey, srcDstMapUid);
            }
          } else {
            MyLog.d("[src-dst] New entry " + uid + " for [" + srcDstMapKey + "]");
            srcDstMapUid = uid;
            logEntriesMap.put(srcDstMapKey, srcDstMapUid);
          }
        } else {
          MyLog.d("[src-dst] Found entry uid " + srcDstMapUid + " for " + uid + " [" + srcDstMapKey + "]");
          uid = srcDstMapUid;
        }

        if(dstSrcMapUid == null) {
          MyLog.d("[dst-src] No entry uid for " + uid + " [" + dstSrcMapKey + "]");
          if(uid == -1) {
            if(srcDstMapUid != null) {
              MyLog.d("[src-dst] Reassigning kernel packet -1 to " + srcDstMapUid);
              uid = srcDstMapUid;
            } else {
              MyLog.d("[dst-src] New kernel entry -1 for [" + dstSrcMapKey + "]");
              dstSrcMapUid = uid;
              logEntriesMap.put(dstSrcMapKey, dstSrcMapUid);
            }
          } else {
            MyLog.d("[dst-src] New entry " + uid + " for [" + dstSrcMapKey + "]");
            dstSrcMapUid = uid;
            logEntriesMap.put(dstSrcMapKey, dstSrcMapUid);
          }
        } else {
          MyLog.d("[dst-src] Found entry uid " + dstSrcMapUid + " for " + uid + " [" + dstSrcMapKey + "]");
          uid = dstSrcMapUid;
        }
      } else {
        MyLog.d("Known uid");
        if(srcDstMapUid == null || dstSrcMapUid == null) {
          MyLog.d("Adding missing uid " + uid + " to netstat map for " + srcDstMapKey + " and " + dstSrcMapKey);
          logEntriesMap.put(srcDstMapKey, uid);
          logEntriesMap.put(dstSrcMapKey, uid);
        }
      }

      // get packet and byte counters
      LogEntry entry = logEntriesHash.get(String.valueOf(uid));

      if(entry == null)
        entry = new LogEntry();

      entry.uid = uid;
      entry.src = src;
      entry.spt = spt;
      entry.dst = dst;
      entry.dpt = dpt;
      entry.len = len;
      entry.packets++;

      if(entry.len > 0) {
        entry.bytes += entry.len;
      }

      entry.timestampString = getTimestamp();
      entry.timestamp = System.currentTimeMillis();

      logEntriesHash.put(String.valueOf(uid), entry);

      if(MyLog.enabled)
        MyLog.d("+++ entry: (" + entry.uid + ") " + entry.src + ":" + entry.spt + " -> " + entry.dst + ":" + entry.dpt + " [" + entry.len + "] " + entry.bytes + " " + entry.timestampString);

      notifyNewEntry(entry);

      // buffer_pos = pos;
    }
    /*
    MyLog.d("truncating buffer_pos: " + buffer_pos + "; length: " + buffer.length());
    if(buffer_pos > buffer.length()) {
      MyLog.d("WTF buffer: [" + buffer + "]");
    } else {
      buffer.delete(0, buffer_pos - 1);
    }
    */
  }

  public void notifyNewEntry(LogEntry entry) {
    synchronized(listenerList) {
      int i = 0;
      for(IptablesLogListener listener : listenerList) {
        i++;
        MyLog.d("Notifying listener " + i);
        listener.onNewLogEntry(entry);
      }
    }
  }

  public void addListener(IptablesLogListener listener) {
    synchronized(listenerList) {
      MyLog.d("Adding listener");
      listenerList.add(listener);
    }
  }

  public void stop() {
    if(logTrackerRunner != null) {
      logTrackerRunner.stop();
    }
  }

  public void kill() {
    synchronized(IptablesLog.scriptLock) {
      try {
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(Iptables.SCRIPT)));
        script.println("ps");
        script.close();
      } catch (java.io.IOException e) { e.printStackTrace(); }

      ShellCommand command = new ShellCommand(new String[] { "su", "-c", "sh " + Iptables.SCRIPT }, "FindLogTracker");
      command.start(false);
      String result = "";
      while(true) {
        String line = command.readStdoutBlocking();
        if(line == null)
          break;
        result += line;
      }

      if(result == null)
        return;

      int iptableslog_pid = -1;

      for(String line : result.split("\n")) {
        // MyLog.d("ps - parsing line [" + line + "]");
        String tokens[] = line.split("\\s+");
        String cmd = tokens[tokens.length - 1];
        int pid, ppid;

        try {
          pid = Integer.parseInt(tokens[1]);
          ppid = Integer.parseInt(tokens[2]);
        } catch (NumberFormatException e) {
          // ignored
          continue;
        } catch (ArrayIndexOutOfBoundsException e) {
          // ignored
          continue;
        } catch (Exception e) {
          Log.d("IptablesLog", "Unexpected exception", e);
          continue;
        }

        // MyLog.d("cmd: " + cmd + "; pid: " + pid + "; ppid: " + ppid);

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
  }

  public void start(final boolean resumed) {
    getLocalIpAddresses();

    if(resumed == false) {
      MyLog.d("adding logging rules");
      Iptables.addRules();

      synchronized(IptablesLog.scriptLock) {
        try {
          PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(Iptables.SCRIPT)));
          script.println("grep IptablesLogEntry /proc/kmsg");
          script.close();
        } catch (java.io.IOException e) { e.printStackTrace(); }

        MyLog.d("Starting iptables log tracker");

        command = new ShellCommand(new String[] { "su", "-c", "sh " + Iptables.SCRIPT }, "IptablesLogTracker");
        command.start(false);
      }
    }
    logTrackerRunner = new LogTracker();
    new Thread(logTrackerRunner, "LogTracker").start();
  }

  public class LogTracker implements Runnable {
    boolean running = false;

    public void stop() {
      running = false;
    }

    public void run() {
      MyLog.d("LogTracker " + this + " starting");
      String result;
      running = true;
      while(running && command.checkForExit() == false) {
        MyLog.d("LogTracker " + this + " checking stdout");

        if(command.stdoutAvailable()) {
          result = command.readStdout();
        } else {
          try { Thread.sleep(750); } catch (Exception e) { Log.d("IptablesLog", "LogTracker exception while sleeping", e); }
          continue;
        }

        if(running == false)
          break;

        if(result == null) {
          MyLog.d("result == null");
          MyLog.d("LogTracker " + this + " exiting [returned null]");
          return;
        }

        MyLog.d("result == [" + result + "]");
        parseResult(result);
      }

      MyLog.d("LogTracker " + this + " exiting [end of loop]");
    }
  }
}
