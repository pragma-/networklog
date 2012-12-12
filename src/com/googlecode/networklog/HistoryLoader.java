/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.util.Log;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

public class HistoryLoader {
  static boolean canceled = false;
  static boolean dialog_showing = false;
  static int dialog_max = 0;
  static int dialog_progress = 0;

  static ProgressDialog dialog = null;

  public void createProgressDialog(final Context context) {
    NetworkLog.handler.post(new Runnable() {
      public void run() {
        synchronized(this) {
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

          dialog.show();
          dialog.setProgress(dialog_progress);
          dialog_showing = true;
        }
      }
    });
  }

  public void loadEntriesFromFile(Context context, String historySize) {
    final LogfileLoader loader = new LogfileLoader();
    canceled = false;

    try {
      long history_size = Long.parseLong(historySize);

      MyLog.d("History size: " + history_size);

      if(history_size == 0) {
        return;
      }

      loader.openLogfile(NetworkLog.settings.getLogFile());
      final long length = loader.getLength();

      if(length == 0) {
        return;
      }

      final long starting_pos = loader.seekToTimestampPosition(System.currentTimeMillis() - history_size);

      if(starting_pos == -1) {
        // nothing to read
        return;
      }

      NetworkLog.logFragment.stopUpdater();
      NetworkLog.logFragment.setDoNotRefresh(true);
      NetworkLog.appFragment.stopUpdater();
      NetworkLog.appFragment.setDoNotRefresh(true);

      final Context context_final = context;
      new Thread(new Runnable() {
        public void run() {
          LogEntry entry;
          dialog_max = (int)(length - starting_pos);
          dialog_progress = 0;
          createProgressDialog(context_final);
          long processed_so_far = 0;
          long next_progress_increment = 0;

          long progress_increment_size = (long)((length - starting_pos) * 0.01);
          next_progress_increment = progress_increment_size;

          MyLog.d("[history] increment size: " + progress_increment_size);

          long start = System.currentTimeMillis();
          // android.os.Debug.startMethodTracing("networklog", 32 * 1024 * 1024);

          try {
            while(!canceled) {
              entry = loader.readEntry();

              if(entry == null) {
                // end of file
                MyLog.d("[history] Reached end of file");
                break;
              }

              processed_so_far = loader.getProcessedSoFar();

              if(processed_so_far >= next_progress_increment) {
                next_progress_increment += progress_increment_size;
                dialog_progress = (int)processed_so_far;
                if(dialog_showing && dialog != null) {
                  dialog.setProgress(dialog_progress);
                }
              }

              NetworkLog.logFragment.onNewLogEntry(entry);
              NetworkLog.appFragment.onNewLogEntry(entry);
            }

            loader.closeLogfile();
          } catch(Exception e) {
            Log.w("NetworkLog", "loadEntriesFromFile", e);
          } finally {
            // android.os.Debug.stopMethodTracing();
            long elapsed = System.currentTimeMillis() - start;
            Log.d("NetworkLog", "Load history elapsed: " + elapsed);

            MyLog.d("[history] Dismissing progress dialog");
            NetworkLog.handler.post(new Runnable() {
              public void run() {
                if(dialog_showing) {
                  dialog_showing = false;

                  if(dialog != null) {
                    MyLog.d("[history] Dismissed progress dialog");
                    dialog.dismiss();
                    dialog = null;
                  }
                }
              }
            });

            NetworkLog.logFragment.startUpdater();
            NetworkLog.logFragment.setDoNotRefresh(false);
            NetworkLog.appFragment.startUpdater();
            NetworkLog.appFragment.setDoNotRefresh(false);
          }
        }
      }, "LoadHistory").start();
    } catch (Exception e) {
      Log.w("NetworkLog", "loadEntriesFromFile", e);
    }
  }
}
