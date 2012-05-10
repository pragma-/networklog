package com.googlecode.networklog;

import android.util.Log;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.lang.StringBuilder;
import java.io.RandomAccessFile;

public class HistoryLoader {
  static boolean canceled = false;
  static boolean dialog_showing = false;
  static int dialog_max = 0;
  static int dialog_progress = 0;
  static ProgressDialog dialog = null;

  public void createProgressDialog(Context context) {
    dialog = new ProgressDialog(context);
    dialog.setIndeterminate(false);
    dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    dialog.setMax(dialog_max);
    dialog.setCancelable(false);
    dialog.setTitle("");
    dialog.setMessage("Loading history");

    dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Cancel", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        canceled = true;
      }
    });

    dialog_showing = true;
    NetworkLog.handler.post(new Runnable() {
      public void run() {
        dialog.show();
        dialog.setProgress(dialog_progress);
      }
    });
  }

  public void loadEntriesFromFile(Context context, String historySize) {
    canceled = false;

    try {
      long history_size = Long.parseLong(historySize);

      MyLog.d("History size: " + history_size);

      if(history_size == 0) {
        return;
      }

      final RandomAccessFile logfile = new RandomAccessFile(NetworkLog.settings.getLogFile(), "r");

      final long length = logfile.length();

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
          if(NetworkLog.state == NetworkLog.State.EXITING) {
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

          MyLog.d("[history] Testing line [" + line + "]");

          long timestamp = Long.parseLong(line.split("[^0-9-]+", 2)[0]);

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

      final long starting_pos = result;

      NetworkLog.logView.stopUpdater();
      NetworkLog.appView.stopUpdater();

      final Context context_final = context;
      new Thread(new Runnable() {
        public void run() {
          int buffer_size = 1024 * 16;
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
          long processed_so_far = 0;
          long next_progress_increment = 0;

          LogEntry entry = new LogEntry();
          StringBuilder sb = new StringBuilder(128);
          char[] chars = new char[128];

          NetworkLog.handler.post(new Runnable() {
            public void run() {
              dialog_max = (int)(length - starting_pos);
              dialog_progress = 0;
              createProgressDialog(context_final);
            }
          });

          long progress_increment_size = (long)((length - starting_pos) * 0.01);
          next_progress_increment = progress_increment_size;

          MyLog.d("[history] increment size: " + progress_increment_size);

          int token, pos, delim, i;
          String string;
          boolean done;

          // android.os.Debug.startMethodTracing("networklog", 16 * 1024 * 1024);
          // long start = System.currentTimeMillis();

          try {
            while(!canceled) {
              buffer_length = logfile.read(buffer);
              buffer_pos = 0;

              read_so_far += buffer_length;

              if(MyLog.enabled) {
                MyLog.d("[history] read " + buffer_length + "; so far: " + read_so_far + " out of " + length);
              }

              if(buffer_length == -1) {
                // end of file
                break;
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

              // extract and parse lines
              while(buffer_pos < buffer_length && !canceled) {
                if(buffer[buffer_pos] != '\n') {
                  line[line_length++] = buffer[buffer_pos++];
                } else {
                  // got line
                  buffer_pos++;

                  processed_so_far += line_length;

                  if(processed_so_far >= next_progress_increment) {
                    next_progress_increment += progress_increment_size;
                    dialog_progress = (int)processed_so_far;
                    if(dialog_showing && dialog != null) {
                      dialog.setProgress(dialog_progress);
                    }
                  }

                  for(i = 0; i < line_length; i++) {
                    chars[i] = (char)line[i];
                  }

                  sb.setLength(0);
                  sb.append(chars, 0, line_length);
                  string = sb.toString();

                  token = 0;
                  pos = 0;
                  delim = 0;
                  done = false;

                  if(string.length() == 0) {
                    line_length = 0;
                    continue;
                  }

                  while(!done) {
                    delim = string.indexOf(',', pos);

                    if(delim == -1) {
                      delim = string.length();
                      done = true;
                    }

                    // MyLog.d("Got token " + token + " (" + pos + "," + delim + ") [" + string.substring(pos, delim) + "]"); 

                    switch(token) {
                      case 0:
                        entry.timestamp = Long.parseLong(string.substring(pos, delim));
                        break;
                      case 1:
                        if((delim - pos) != 0) {
                          entry.in = StringPool.get(string.substring(pos, delim));
                        } else {
                          entry.in = null;
                        }
                        break;
                      case 2:
                        if((delim - pos) != 0) {
                          entry.out = StringPool.get(string.substring(pos, delim));
                        } else {
                          entry.out = null;
                        }
                        break;
                      case 3:
                        entry.uid = Integer.parseInt(string.substring(pos, delim));
                        break;
                      case 4:
                        entry.src = StringPool.get(string.substring(pos, delim));
                        break;
                      case 5:
                        entry.spt = Integer.parseInt(string.substring(pos, delim));
                        break;
                      case 6:
                        entry.dst = StringPool.get(string.substring(pos, delim));
                        break;
                      case 7:
                        entry.dpt = Integer.parseInt(string.substring(pos, delim));
                        break;
                      case 8:
                        entry.len = Integer.parseInt(string.substring(pos, delim));
                        break;
                    }

                    token++;
                    pos = delim + 1;
                  }

                  if(token != 9) {
                    MyLog.d("Skipping malformed entry");
                    line_length = 0;
                    continue;
                  }

                  NetworkLog.logView.onNewLogEntry(entry);
                  NetworkLog.appView.onNewLogEntry(entry);

                  // reset line
                  line_length = 0;
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

            logfile.close();
          } catch(Exception e) {
            Log.w("NetworkLog", "loadEntriesFromFile", e);
          } finally {
            // long elapsed = System.currentTimeMillis() - start;
            // android.util.Log.d("[IptablesLog]", "Load history elapsed: " + elapsed);
            // android.os.Debug.stopMethodTracing();

            if(dialog_showing) {
              dialog_showing = false;

              if(dialog != null) {
                dialog.dismiss();
                dialog = null;
              }
            }

            NetworkLog.logView.startUpdater();
            NetworkLog.appView.startUpdater();
          }
        }
      }, "LoadHistory").start();
    } catch (Exception e) {
      Log.w("NetworkLog", "loadEntriesFromFile", e);
    }
  }
}
