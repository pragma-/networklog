package com.googlecode.iptableslog;

import android.util.Log;
import java.util.Hashtable;
import java.util.ArrayList;

public class IptablesLogTracker {
  static Hashtable<String, LogEntry> logEntriesHash = new Hashtable<String, LogEntry>();
  static ArrayList<LogEntry> logEntriesList = new ArrayList<LogEntry>();

  public static class LogEntry {
    String uid;
    String src;
    String dst;
    int len;
    int spt;
    int dpt;
    int packets;
    int bytes;
  }

  // FIXME: Needs buffering of incomplete logs
  public static void parseResult(String result) {
    Log.d("[Iptables Log]", "parsing result");
    int pos = 0;
    String src, dst, len, spt, dpt, uid;

    while((pos = result.indexOf("[DROIDWALL]", pos)) > -1) {
      int newline = result.indexOf("\n", pos);

      Log.d("[Iptables Log]", "got [DROIDWALL] at " + pos);

      pos = result.indexOf("SRC=", pos);
      if(pos == -1) continue;
      int space = result.indexOf(" ", pos);
      if(space == -1) continue;
      src = result.substring(pos + 4, space);
      Log.d("[Iptables Log]", "SRC is " + src);

      pos = result.indexOf("DST=", pos);
      if(pos == -1) continue;
      space = result.indexOf(" ", pos);
      if(space == -1) continue;
      dst = result.substring(pos + 4, space);
      Log.d("[Iptables Log]", "DST is " + dst);
      
      pos = result.indexOf("LEN=", pos);
      if(pos == -1) continue;
      space = result.indexOf(" ", pos);
      if(space == -1) continue;
      len = result.substring(pos + 4, space);
      Log.d("[Iptables Log]", "LEN is " + len);
     
      pos = result.indexOf("SPT=", pos);
      if(pos == -1) continue;
      space = result.indexOf(" ", pos);
      if(space == -1) continue;
      spt = result.substring(pos + 4, space);
      Log.d("[Iptables Log]", "SPT is " + spt);
    
      pos = result.indexOf("DPT=", pos);
      if(pos == -1) continue;
      space = result.indexOf(" ", pos);
      if(space == -1) continue;
      dpt = result.substring(pos + 4, space);
      Log.d("[Iptables Log]", "DPT is " + dpt);

      pos = result.indexOf("UID=", pos);
      if(pos == -1) continue;
      space = result.indexOf(" ", pos);
      if(space == -1) continue;
      uid = result.substring(pos + 4, space);
      Log.d("[Iptables Log]", "UID is " + uid);

      LogEntry entry = logEntriesHash.get(uid);

      if(entry == null)
        entry = new LogEntry();

      entry.uid = uid;
      entry.src = src;
      entry.dst = dst;
      entry.spt = new Integer(spt).intValue();
      entry.dpt = new Integer(dpt).intValue();
      entry.len = new Integer(len).intValue();
      entry.packets++;
      entry.bytes += entry.len * 8;

      logEntriesHash.put(uid, entry);
      logEntriesList.add(entry);
    }
  }

  public static void start() {
    Thread mainLoop = new Thread() {
      public void run() {
        Log.d("[Iptables Log]", "starting cat /proc/kmsg");
        ShellCommand command = new ShellCommand(new String[] { "su", "-c", "cat /proc/kmsg" });
        command.start();

        String result;
        while(command.checkForExit() == false) {
          Log.d("[Iptables Log]", "reading stdout");
          result = command.readStdout();
          if(result == null) {
            Log.d("[Iptables Log]", "result == null");
            return;
          }

          Log.d("[Iptables Log]", "result == [" + result + "]");
          parseResult(result);
        }
      }
    };
    mainLoop.start();
  }
}
