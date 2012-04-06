package com.googlecode.iptableslog;

import android.util.Log;

import java.io.RandomAccessFile;

public class Utils {
  public void loadEntriesFromFile() {
    try {
      long history_size = Long.parseLong(IptablesLog.settings.getHistorySize());

      MyLog.d("History size: " + history_size);

      if(history_size == 0) {
        return;
      }

      RandomAccessFile logfile = new RandomAccessFile(IptablesLog.settings.getLogFile(), "r");

      long length = logfile.length();

      if(length == 0) {
        return;
      }

      long result = 0;

      if(history_size > 0) {
        long min = 0;
        long max = length;
        long history_target = System.currentTimeMillis() - history_size;

        while(max >= min) {
          if(IptablesLog.state == IptablesLog.State.EXITING) {
            logfile.close();
            return;
          }

          long mid = (max + min) / 2;

          MyLog.d("[history] testing position " + mid);

          logfile.seek(mid);

          // discard line as we may be anywhere within it
          logfile.readLine();

          String line = logfile.readLine();
          if(line == null) {
            MyLog.d("[history] No packets found within time range");
            logfile.close();
            return;
          }

          long timestamp = Long.parseLong(line.split("[^0-9-]+")[0]);

          MyLog.d("[history] comparing timestamp " + timestamp + " <=> " + history_target);

          if(timestamp < history_target) {
            min = mid + 1;
          } else if(timestamp > history_target) {
            max = mid - 1;
          } else {
            MyLog.d("Found at " + mid);
            result = mid;
            break;
          }

          // remember last nearest match
          result = mid;
        }

        logfile.seek(result);
      }

      String line;
      while((line = logfile.readLine()) != null) {
        LogEntry entry = new LogEntry();

        String[] entries = line.split(" ");

        if(entries.length != 7) {
          MyLog.d("Bad entry: [" + line + "]");
          continue;
        }

        entry.timestamp = Long.parseLong(entries[0]);
        entry.timestampString = IptablesLogService.getTimestamp(entry.timestamp);
        entry.uid = Integer.parseInt(entries[1]);
        entry.src = entries[2];
        entry.spt = Integer.parseInt(entries[3]);
        entry.dst = entries[4];
        entry.dpt = Integer.parseInt(entries[5]);
        entry.len = Integer.parseInt(entries[6]);

        IptablesLog.logView.onNewLogEntry(entry);
        IptablesLog.appView.onNewLogEntry(entry);
      }
      logfile.close();
    } catch (Exception e) {
      Log.w("IptablesLog", "loadEntriesFromFile", e);
    } 
  }
}
