/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.util.Log;

import java.lang.StringBuilder;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;

public class LogfileLoader {
  RandomAccessFile logfile = null;
  LogEntry entry = new LogEntry();
  FastParser parser = new FastParser(',');
  int buffer_size = 1024 * 16;
  byte[] buffer = new byte[buffer_size]; // read a nice sized chunk of data
  byte[] partial_buffer = new byte[128]; // for holding partial lines from end of buffer
  byte[] line = new byte[128]; // a single line in the log file
  int buffer_length = 0;
  int buffer_pos = 0;
  short partial_buffer_length = 0;
  short line_length = 0;
  short line_pos = 0;
  long read_so_far = 0;
  long processed_so_far = 0;
  StringBuilder sb = new StringBuilder(128);
  char[] chars = new char[128];
  long length = 0;  // file length

  public void reset() {
    buffer_length = 0;
    buffer_pos = 0;
    partial_buffer_length = 0;
    line_length = 0;
    line_pos = 0;
    read_so_far = 0;
    processed_so_far = 0;
    length = 0;
  }

  public void openLogfile(String filename) throws FileNotFoundException, IllegalArgumentException, IOException {
    reset();
    logfile = new RandomAccessFile(filename, "r");
    getLength();
  }

  public void closeLogfile() throws IOException {
    if(logfile != null) {
      logfile.close();
      logfile = null;
    }
  }

  public long getLength() throws IOException {
    length = logfile.length(); // cache in member variable
    return length;
  }

  public long seekToTimestampPosition(long target) throws IOException {
    long result = 0;
    long min = 0;
    long max = getLength();
    long mid;
    long timestamp;

    // nearest match binary search to find timestamp within logfile
    while(max >= min) {
      if(NetworkLog.state == NetworkLog.State.EXITING) {
        closeLogfile();
        return -1;
      }

      mid = (max + min) / 2;

      if(MyLog.enabled) {
        MyLog.d("[LogfileLoader] testing position " + mid);
      }

      logfile.seek(mid);

      // discard line as we may be anywhere within it
      logfile.readLine();

      String line = logfile.readLine();
      if(line == null) {
        MyLog.d("[LogfileLoader] No packets found within time range");
        closeLogfile();
        return -1;
      }

      if(MyLog.enabled) {
        MyLog.d("[LogfileLoader] Testing line [" + line + "]");
      }

      timestamp = Long.parseLong(line.split("[^0-9-]+", 2)[0]);

      if(MyLog.enabled) {
        MyLog.d("[LogfileLoader] comparing timestamp " + timestamp + " <=> " + target);
      }

      if(timestamp < target) {
        min = mid + 1;
      } else if(timestamp > target) {
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
    return result;
  }

  public byte[] getBuffer() {
    return buffer;
  }

  public int getBufferLength() {
    return buffer_length;
  }

  public long getReadSoFar() {
    return read_so_far;
  }

  public boolean readChunk() throws IOException {
    int i;

    buffer_length = logfile.read(buffer);
    buffer_pos = 0;

    if(buffer_length != -1) {
      read_so_far += buffer_length;
    }

    if(MyLog.enabled) {
      MyLog.d("[LogfileLoader] read " + buffer_length + "; so far: " + read_so_far + " out of " + length);
    }

    if(buffer_length == -1) {
      // end of file
      MyLog.d("[LogfileLoader] Reached end of file");
      return false;
    }

    // reset line
    line_length = 0;

    // start line with previous unfinished line
    if(partial_buffer_length > 0) {
      for(i = 0; i < partial_buffer_length; i++) {
        line[line_length++] = partial_buffer[i];
      }

      // reset partial buffer
      partial_buffer_length = 0;
    }

    return true;
  }

  public LogEntry readEntry() throws IOException {
    int i;

    while(true) {
      if(buffer_pos >= buffer_length) {
        if(readChunk() == false) {
          // reached end of file
          return null;
        }
      }

      // extract and parse lines
      while(buffer_pos < buffer_length) {
        if(line_length >= line.length - 1) {
          Log.w("NetworkLog", "Skipping too long entry: [" + new String(line, 0, line.length - 1) + "]");
          line_length = 0;

          // read remainder of long line
          while(buffer_pos < buffer_length && buffer[buffer_pos] != '\n') {
            buffer_pos++;
          }
          continue;
        }

        if(buffer[buffer_pos] != '\n') {
          line[line_length++] = buffer[buffer_pos++];
        } else {
          // got line
          buffer_pos++;

          if(line_length == 0) {
            continue;
          }

          processed_so_far += line_length;

          for(i = 0; i < line_length; i++) {
            chars[i] = (char)line[i];
          }

          String value;
          parser.setLine(chars, line_length);

          try {
            entry.timestamp = parser.getLong();

            value = parser.getString();
            if(value == null) {
              entry.in = null;
            } else {
              entry.in = value;
            }

            value = parser.getString();
            if(value == null) {
              entry.out = null;
            } else {
              entry.out = value;
            }

            entry.uidString = parser.getString();
            entry.uid = Integer.parseInt(entry.uidString);
            entry.src = parser.getString();
            entry.spt = parser.getInt();
            entry.dst = parser.getString();
            entry.dpt = parser.getInt();
            entry.len = parser.getInt();

            // Check hasMore() to support legacy logfile entries that did not include a protocol field
            if(parser.hasMore()) {
              entry.proto = parser.getString();
            } else {
              entry.proto = "";
            }
          } catch (Exception e) {
            Log.w("NetworkLog", "Skipping malformed entry", e);
            line_length = 0;
            continue;
          }

          // reset line
          line_length = 0;

          return entry;
        }
      }

      if(buffer[buffer_pos - 1] != '\n') {
        // no newline; must be last line of buffer
        partial_buffer_length = 0;

        for(i = 0; i < line_length; i++) {
          partial_buffer[partial_buffer_length++] = line[i];
        }
      }
    }
  }

  public long getProcessedSoFar() {
    return processed_so_far;
  }
}
