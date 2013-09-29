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
import java.util.concurrent.FutureTask;

public class HistoryLoader {
  static boolean canceled = false;
  static boolean dialog_showing = false;
  static int dialog_max = 0;
  static int dialog_progress = 0;

  static ProgressDialog dialog = null;

  public FutureTask createProgressDialog(final Context context) {
    FutureTask futureTask = new FutureTask(new Runnable() {
      public void run() {
        dialog = new ProgressDialog(context);
        dialog.setIndeterminate(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(dialog_max);
        dialog.setCancelable(false);
        dialog.setTitle("");
        dialog.setMessage(context.getResources().getString(R.string.history_loading));

        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            canceled = true;
          }
        });

        dialog.show();
        dialog.setProgress(dialog_progress);
        dialog_showing = true;
      }
    }, null);

    NetworkLog.handler.post(futureTask);
    return futureTask;
  }

  public void loadEntriesFromFile(final Context context, final String historySize) {
    final LogfileLoader loader = new LogfileLoader();
    canceled = false;

    try {
      long history_size = Long.parseLong(historySize);

      MyLog.d("[HistoryLoader] History size: " + history_size);

      if(history_size == 0) {
        return;
      }

      loader.openLogfile(NetworkLog.settings.getLogFile());
      final long length = loader.getLength();

      if(length == 0) {
        return;
      }

      final long starting_pos = (history_size == -1) ? 0 : loader.seekToTimestampPosition(System.currentTimeMillis() - history_size);

      if(starting_pos == -1) {
        // nothing to read
        return;
      }

      dialog_max = (int)(length - starting_pos);
      dialog_progress = 0;
      
      FutureTask createDialog = createProgressDialog(context);
      createDialog.get(); // wait until createDialog task completes (ensure dialog is created and shown before continuing)

      new Thread(new Runnable() {
        public void run() {
          NetworkLog.logFragment.stopUpdater();
          NetworkLog.logFragment.setDoNotRefresh(true);
          NetworkLog.appFragment.stopUpdater();
          NetworkLog.appFragment.setDoNotRefresh(true);

          LogEntry entry;
          long processed_so_far = 0;
          long progress_increment_size = (long)((length - starting_pos) * 0.01);
          long next_progress_increment = progress_increment_size;

          MyLog.d("[HistoryLoader] increment size: " + progress_increment_size);

          final long start = System.currentTimeMillis();
          // android.os.Debug.startMethodTracing("networklog", 32 * 1024 * 1024);

          try {
            while(!canceled) {
              entry = loader.readEntry();

              if(entry == null) {
                // end of file
                MyLog.d("[HistoryLoader] Reached end of file");
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
            }
          } catch(Exception e) {
            Log.w("NetworkLog", "loadEntriesFromFile", e);
          } finally {
            try {
              loader.closeLogfile();
            } catch (Exception e) {
              // ignored
            }

            // android.os.Debug.stopMethodTracing();

            NetworkLog.handler.post(new Runnable() {
              public void run() {
                if(dialog != null) {
                  dialog.setMessage(context.getResources().getString(R.string.history_parsing));
                  dialog.setIndeterminate(true);
                }

                long elapsed = System.currentTimeMillis() - start;
                Log.d("NetworkLog", "Load file elapsed: " + elapsed);

                NetworkLog.logFragment.setDoNotRefresh(false);
                NetworkLog.appFragment.setDoNotRefresh(false);

                StringPool.clearCharPool();
                NetworkLog.logFragment.appFragmentNeedsRebuild = true;
                NetworkLog.logFragment.updaterRunOnce();

                NetworkLog.logFragment.startUpdater();
                NetworkLog.appFragment.startUpdater();

                elapsed = System.currentTimeMillis() - start;
                Log.d("NetworkLog", "Load history elapsed: " + elapsed);

                MyLog.d("[HistoryLoader] Dismissing progress dialog");
                if(dialog_showing) {
                  dialog_showing = false;

                  if(dialog != null) {
                    MyLog.d("[HistoryLoader] Dismissed progress dialog");
                    dialog.dismiss();
                    dialog = null;
                  }
                }
              }
            });
          }
        }
      }, "LoadHistory").start();
    } catch (Exception e) {
      Log.w("NetworkLog", "loadEntriesFromFile", e);
    }
  }
}
