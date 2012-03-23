package com.googlecode.iptableslog;

import android.util.Log;
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
  ArrayList<IptablesLogListener> listenerList;
  static String localIpAddr;
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
    String timestamp;
    boolean dirty;
  }

  public IptablesLogTracker() {
    listenerList = new ArrayList<IptablesLogListener>();

    if(IptablesLog.data == null) {
      logEntriesHash = new Hashtable<String, LogEntry>();
      StringBuilder buffer = new StringBuilder(8192 * 2);
    } else {
      logEntriesHash = IptablesLog.data.iptablesLogTrackerLogEntriesHash;
      buffer = IptablesLog.data.iptablesLogTrackerBuffer;
      command = IptablesLog.data.iptablesLogTrackerCommand;
    }
  }

  public static String getTimestamp() {
    return format.format(new Date());
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

  public void parseResult(String result) {
    MyLog.d("--------------- parsing result --------------");
    int pos = 0, buffer_pos = 0; 
    String src, dst, len, spt, dpt, uid;

    /*
    MyLog.d("buffer length: " + buffer.length() + "; result length: " + result.length());
    MyLog.d("buffer: [" + buffer + "] <--> [" + result + "]");
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

      // MyLog.d("pos: " + pos + "; buffer length: " + buffer.length());
      // String line = "no newline: " + buffer.substring(pos, buffer.length());

      /*

      if(newline != -1) {
        line = buffer.substring(pos, newline - 1);
      }

      MyLog.d("parsing line [" + line + "]");
      */


      pos = result.indexOf("SRC=", pos);
      if(pos == -1 || pos > newline) { /* MyLog.d("buffering [" + line + "] for SRC"); */ break; };
      int space = result.indexOf(" ", pos);
      if(space == -1 || space > newline) { /* MyLog.d("buffering [" + line + "] for SRC space"); */ break; };
      src = result.substring(pos + 4, space);

      pos = result.indexOf("DST=", pos);
      if(pos == -1 || pos > newline) { /* MyLog.d("buffering [" + line + "] for DST"); */ break; };
      space = result.indexOf(" ", pos);
      if(space == -1 || space > newline) { /* MyLog.d("buffering [" + line + "] for DST space"); */ break; };
      dst = result.substring(pos + 4, space);
      
      pos = result.indexOf("LEN=", pos);
      if(pos == -1 || pos > newline) { /* MyLog.d("buffering [" + line + "] for LEN"); */ break; };
      space = result.indexOf(" ", pos);
      if(space == -1 || space > newline) { /* MyLog.d("buffering [" + line + "] for LEN space"); */ break; };
      len = result.substring(pos + 4, space);
     
      pos = result.indexOf("SPT=", pos);
      if(pos == -1 || pos > newline) { /* MyLog.d("buffering [" + line + "] for SPT"); */ break; };
      space = result.indexOf(" ", pos);
      if(space == -1 || space > newline) { /* MyLog.d("buffering [" + line + "] for SPT space"); */ break; };
      spt = result.substring(pos + 4, space);
    
      pos = result.indexOf("DPT=", pos);
      if(pos == -1 || pos > newline) { /* MyLog.d("buffering [" + line + "] for DPT"); */ break; };
      space = result.indexOf(" ", pos);
      if(space == -1 || space > newline) { /* MyLog.d("buffering [" + line + "] for DPT space"); */ break; };
      dpt = result.substring(pos + 4, space);

      int lastpos = pos;
      pos = result.indexOf("UID=", pos);
      // MyLog.d("newline pos: " + newline + "; UID pos: " + pos);
      if(pos == -1 || (pos > newline && newline != -1)) {
        ///* MyLog.d("Setting UID unspecified");
        uid = "-1";
        pos = lastpos;
      } else {
        /* MyLog.d("Looking for UID newline"); */
        if(newline == -1) { /* MyLog.d("buffering [" + line + "] for UID newline"); */ break; };
        uid = result.substring(pos + 4, newline);
      }

      LogEntry entry = logEntriesHash.get(uid);

      if(entry == null)
        entry = new LogEntry();

      try {
        entry.uid = Integer.parseInt(uid.split("[^0-9-]+")[0]);
      } catch (Exception e) {
        Log.e("IptablesLog", "Bad data for uid: " + uid, e);
        entry.uid = -13;
      }

      entry.src = src;
      entry.dst = dst;

      try {
        entry.spt = Integer.parseInt(spt.split("[^0-9-]+")[0]);
      } catch (Exception e) {
        Log.e("IptablesLog", "Bad data for spt: " + spt, e);
        entry.spt = -1;
      }

      try {
        entry.dpt = Integer.parseInt(dpt.split("[^0-9-]+")[0]);
      } catch (Exception e) {
        Log.e("IptablesLog", "Bad data for dpt: " + dpt, e);
        entry.dpt = -1;
      }

      try {
        entry.len = Integer.parseInt(len.split("[^0-9-]+")[0]);
      } catch (Exception e) {
        Log.e("IptablesLog", "Bad data for len: " + len, e);
        entry.len = -1;
      }

      entry.packets++;
      if(entry.len > 0) {
        entry.bytes += entry.len;
      }
      entry.timestamp = getTimestamp();

      logEntriesHash.put(uid, entry);

      MyLog.d("+++ entry uid: " + entry.uid + " " + entry.src + " " + entry.spt + " " + entry.dst + " " + entry.dpt + " " + entry.len + " " + entry.bytes + " " + entry.timestamp);

      notifyNewEntry(entry);

      //buffer_pos = pos;
    }
    /*
    MyLog.d("truncating buffer_pos: " + buffer_pos + "; length: " + buffer.length());
    if(buffer_pos > buffer.length()) {
      MyLog.d("WTF buffer: [" + buffer + "]");
    } else {
      buffer.replace(0, buffer.length(), buffer.substring(buffer_pos, buffer.length()));
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
      while(!command.checkForExit()) {
        result += command.readStdoutBlocking();
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
    localIpAddr = getLocalIpAddress();

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
        // MyLog.d("LogTracker " + this + " checking stdout");

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
