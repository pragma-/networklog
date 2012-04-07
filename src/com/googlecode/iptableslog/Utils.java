package com.googlecode.iptableslog;

import android.util.Log;

import java.lang.StringBuilder;
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

        // nearest match binary search to find timestamp within logfile
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
            // found exact match
            MyLog.d("Found at " + mid);
            result = mid;
            break;
          }

          // remember last nearest match
          result = mid;
        }

        logfile.seek(result);
      }

      int buffer_size = (int)(length * 0.05f); // 50K buffer for 1MB, 2.4MB buffer for 48MB
      MyLog.d("Using " + buffer_size + " byte buffer to read history");

      byte[] buffer = new byte[buffer_size]; // read a nice sized chunk of data
      byte[] partial_buffer = new byte[128]; // for holding partial lines from end of buffer
      byte[] line = new byte[128]; // a single line in the log file
      long buffer_length = 0;
      int buffer_pos = 0;
      short partial_buffer_length = 0;
      short line_length = 0;
      short line_pos = 0;
      long read_so_far = 0;

      LogEntry entry = new LogEntry();
      StringBuilder sb = new StringBuilder(128);
      char[] chars = new char[128];

      while(true) {
        buffer_length = logfile.read(buffer);
        buffer_pos = 0;

        read_so_far += buffer_length;
        MyLog.d("[history] read " + buffer_length + "; so far: " + read_so_far + " out of " + length);

        if(buffer_length == -1) {
          // end of file
          break;
        }

        // reset line
        line_length = 0;

        // start line with previous unfinished line
        if(partial_buffer_length > 0) {
          for(int i = 0; i < partial_buffer_length; i++) {
            line[line_length++] = partial_buffer[i];
          }

          // reset partial buffer
          partial_buffer_length = 0;
        }

        // extract and parse lines
        while(buffer_pos < buffer_length) {
          if(buffer[buffer_pos] != '\n') {
            line[line_length++] = buffer[buffer_pos++];
          } else {
            // got line
            buffer_pos++;

            for(int i = 0; i < line_length; i++) {
              chars[i] = (char)line[i];
            }

            sb.setLength(0);
            sb.append(chars, 0, line_length);

            // todo: optimization: use indexOf/substring instead of split?
            String[] entries = sb.toString().split(" ");

            if(entries.length != 7) {
              MyLog.d("[history] Bad entry: [" + sb.toString() + "]");
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

            // reset line
            line_length = 0;
          }
        }

        if(buffer[buffer_pos - 1] != '\n') {
          // no newline; must be last line of buffer
          partial_buffer_length = 0;

          for(int i = 0; i < line_length; i++) {
            partial_buffer[partial_buffer_length++] = line[i];
          }
        }
      }

      logfile.close();
    } catch (Exception e) {
      Log.w("IptablesLog", "loadEntriesFromFile", e);
    } 
  }
}
