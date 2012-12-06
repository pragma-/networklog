/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
   */

package com.googlecode.networklog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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

public class ClearLog
{
  FixedSpinnerAlertDialog dialog = null;
  int spinnerInit = 0;

  public void clearLogEntriesOlderThan(long timerange, boolean clearLogfile) {
    Log.d("NetworkLog", "Clearing entries older than " + timerange + "; logfile: " + clearLogfile);

    NetworkLog.logFragment.clearLogEntriesOlderThan(timerange);
    NetworkLog.appFragment.rebuildLogEntries();

    if(clearLogfile) {
      // TODO stop service and updaters
      // TODO clear logfile entries
    }
  }

  public void showDialog(Context context) {
    LinearLayout view = new LinearLayout(context);
    view.setOrientation(LinearLayout.VERTICAL);

    LinearLayout layout = new LinearLayout(context);
    layout.setOrientation(LinearLayout.HORIZONTAL);

    TextView tv = new TextView(context);
    tv.setText("Remove log entries older than ");
    layout.addView(tv);

    final String[] timerangeValues = context.getResources().getStringArray(R.array.clearlog_timerange_values);

    final Spinner spinner = new Spinner(context);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        context, R.array.clearlog_timerange_entries, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    spinner.setPrompt("Remove entires older than");
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
    checkbox.setChecked(false);
    checkbox.setText("Delete entries from logfile");

    view.addView(checkbox);

    dialog = new FixedSpinnerAlertDialog(context);
    dialog.setTitle("Clear Log");
    dialog.setCancelable(true);
    dialog.setView(view);
    dialog.setLayout(view); /* workaround to dismiss Spinner pop-up dialog when changing orientation */
    dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Clear log", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        dialog.dismiss();
        clearLogEntriesOlderThan(Long.parseLong(timerangeValues[spinner.getSelectedItemPosition()]), checkbox.isChecked());
      }
    });
    dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
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
