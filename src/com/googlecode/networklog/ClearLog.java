/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
   */

package com.googlecode.networklog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.lang.Thread;
import java.lang.Runnable;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class ClearLog
{
  FixedSpinnerAlertDialog dialog = null;
  int spinnerInit = 0;
  ProgressDialog progressDialog = null;
  int progress = 0;
  int progress_max = 0;

  public void showProgressDialog(final Context context) {
    NetworkLog.handler.post(new Runnable() {
      public void run() {
        progressDialog = new ProgressDialog(context);

        if(progress_max == 0) {
          progressDialog.setIndeterminate(true);
        } else {
          progressDialog.setIndeterminate(false);
        }

        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("");
        progressDialog.setMessage(context.getResources().getString(R.string.clear_progress_message));
        progressDialog.setMax(progress_max);
        progressDialog.setProgress(progress);
        progressDialog.show();
      }
    });
  }

  public void clearLogFileEntriesOlderThan(final Context context, final long timerange) {
    LogfileLoader loader = new LogfileLoader();
    long start = System.currentTimeMillis();

    try {
      loader.openLogfile(NetworkLog.settings.getLogFile());
      long length = loader.getLength();
      long starting_pos = loader.seekToTimestampPosition(System.currentTimeMillis() - timerange);

      File logfile = new File(NetworkLog.settings.getLogFile());
      File file = new File(logfile.getParent(), logfile.getName() + ".clear");

      Log.d("NetworkLog", "Opened " + logfile + " and " + file + " for clearing");
      Log.d("NetworkLog", "starting pos: " + starting_pos);

      BufferedOutputStream fileWriter = new BufferedOutputStream(new FileOutputStream(file));

      if(starting_pos != -1) {
        progress_max = (int)(length - starting_pos);
        progress = 0;
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(progress_max);

        long processed_so_far = 0;
        long progress_increment_size = (long)((length - starting_pos) * 0.01);
        long next_progress_increment = progress_increment_size;

        while(true) {
          if(loader.readChunk() == false) {
            // end of file
            MyLog.d("[clearlogfile] Reached end of file");
            break;
          }

          processed_so_far = loader.getReadSoFar();

          if(processed_so_far >= next_progress_increment) {
            next_progress_increment += progress_increment_size;
            progress = (int)processed_so_far;
            if(progressDialog != null && progressDialog.isShowing()) {
              progressDialog.setProgress(progress);
            }
          }

          fileWriter.write(loader.getBuffer(), 0, loader.getBufferLength());
        }

        loader.closeLogfile();
      }

      fileWriter.close();

      if(logfile.delete()) {
        if(!file.renameTo(logfile)) {
          Log.w("NetworkLog", "Failed to rename " + file + " to " + logfile);
        }
      } else {
        Log.w("NetworkLog", "Failed to delete " + logfile);
      }
    } catch (Exception e) {
      Log.w("NetworkLog", "clearLogFileEntriesOlderThan", e);
    } finally {
      long elapsed = System.currentTimeMillis() - start;
      Log.d("NetworkLog", "Clear logfile history elapsed: " + elapsed);

      NetworkLog.handler.post(new Runnable() {
        public void run() {
          NetworkLog.updateStatusText();
        }
      });
    }
  }

  public void clearLogEntriesOlderThan(final Context context, final long timerange, final boolean clearLogfile) {
    new Thread(new Runnable() {
      public void run() {
        Log.d("NetworkLog", "Clearing entries older than " + timerange + "; logfile: " + clearLogfile);

        progress_max = 0;
        progress = 0;
        showProgressDialog(context);

        NetworkLog.logFragment.clearLogEntriesOlderThan(timerange);
        NetworkLog.appFragment.rebuildLogEntries();

        if(clearLogfile) {
          boolean serviceRunning = false;
          if(NetworkLog.isServiceRunning(context, NetworkLogService.class.getName())) {
            serviceRunning = true;
            Log.d("NetworkLog", "Stopping logging to clear log");
            NetworkLogService.instance.stopLogging();
          }

          Log.d("NetworkLog", "Clearing logfile...");
          clearLogFileEntriesOlderThan(context, timerange);

          if(serviceRunning) {
            Log.d("NetworkLog", "Resuming logging");
            NetworkLogService.instance.startLogging();
          }
        }

        NetworkLog.handler.post(new Runnable() {
          public void run() {
            if(progressDialog != null) {
              progressDialog.dismiss();
              progressDialog = null;
            }
          }
        });

        Log.d("NetworkLog", "Done clearing log entries.");
      }
    }).start();
  }

  public void showClearLogDialog(final Context context) {
    LinearLayout view = new LinearLayout(context);
    view.setOrientation(LinearLayout.VERTICAL);

    LinearLayout layout = new LinearLayout(context);
    layout.setOrientation(LinearLayout.HORIZONTAL);

    TextView tv = new TextView(context);
    tv.setText(context.getResources().getString(R.string.clear_dialog_prompt));
    layout.addView(tv);

    final String[] timerangeValues = context.getResources().getStringArray(R.array.clearlog_timerange_values);

    final Spinner spinner = new Spinner(context);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        context, R.array.clearlog_timerange_entries, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    spinner.setPrompt(context.getResources().getString(R.string.clear_dialog_prompt));
    spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Log.d("NetworkLog", "clearlog spinner selected " + id + " [init: " + spinnerInit + "]");
        if(spinnerInit > 0) {
          // Don't process selection events if spinner is initializing
          spinnerInit--;
          return;
        }

        NetworkLog.settings.setClearLogTimerange(timerangeValues[pos]);
      }

      @Override
      public void onNothingSelected(AdapterView parent) {
        // do nothing
      }
    });

    // Initialize spinner
    String timerange = NetworkLog.settings.getClearLogTimerange();
    int length = timerangeValues.length;
    for(int i = 0; i < length; i++) {
      if(timerange.equals(timerangeValues[i])) {
        spinnerInit++;
        spinner.setSelection(i);
        break;
      }
    }

    layout.addView(spinner);
    view.addView(layout);

    final CheckBox checkbox = new CheckBox(context);
    Resources res = context.getResources();
    checkbox.setChecked(false);
    checkbox.setText(res.getString(R.string.clear_dialog_delete_from_logfile));

    view.addView(checkbox);

    dialog = new FixedSpinnerAlertDialog(context);
    dialog.setTitle(res.getString(R.string.clear_dialog_title));
    dialog.setCancelable(true);
    dialog.setView(view);
    dialog.setLayout(view); /* workaround to dismiss Spinner pop-up dialog when changing orientation */
    dialog.setButton(DialogInterface.BUTTON_POSITIVE, res.getString(R.string.clear_dialog_button_positive), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        dialog.dismiss();
        clearLogEntriesOlderThan(context, Long.parseLong(timerangeValues[spinner.getSelectedItemPosition()]), checkbox.isChecked());
      }
    });
    dialog.setButton(DialogInterface.BUTTON_NEGATIVE, res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        dialog.cancel();
      }
    });
    dialog.show();
  }

  /* workaround to dismiss Spinner pop-up dialog when changing orientation */
  class FixedSpinnerAlertDialog extends AlertDialog {
    LinearLayout layout;

    public FixedSpinnerAlertDialog(final Context context) {
      super(context);
    }

    public FixedSpinnerAlertDialog(final Context context, final int theme) {
      super(context, theme);
    }

    public FixedSpinnerAlertDialog(final Context context, final boolean cancelable, final OnCancelListener cancelListener) {
      super(context, cancelable, cancelListener);
    }

    public void setLayout(LinearLayout layout) {
      this.layout = layout;
    }

    @Override
      public void dismiss() {
        // Workaround for Spinner's dialog leaking current window during rotation
        // See issue #4936 : http://code.google.com/p/android/issues/detail?id=4936
        layout.removeAllViewsInLayout();
        super.dismiss();
      }
  }
}
