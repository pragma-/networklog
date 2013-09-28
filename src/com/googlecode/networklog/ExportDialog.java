/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.widget.Button;
import android.widget.DatePicker;
import android.view.View;
import android.view.LayoutInflater;
import android.os.Bundle;
import android.os.Environment;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.support.v4.app.DialogFragment;

import com.samsung.sprc.fileselector.*;

public class ExportDialog
{
  public Button startDateButton; 
  public Button endDateButton;
  public Button filenameButton;
  public AlertDialog dialog;
  private Context context;

  private SimpleDateFormat dateDisplayFormat = new SimpleDateFormat("MMMM d, y");
  private SimpleDateFormat dateFilenameFormat = new SimpleDateFormat("y-MM-dd");

  public Date startDate;
  public Date endDate;
  public File file;

  public ExportDialog(final Context context)
  {
    this.context = context;
    Resources res = context.getResources();
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    View view = inflater.inflate(R.layout.exportdialog, null);

    startDateButton = (Button) view.findViewById(R.id.exportStartDateButton);
    endDateButton = (Button) view.findViewById(R.id.exportEndDateButton);
    filenameButton = (Button) view.findViewById(R.id.exportFilenameButton);

    GregorianCalendar today = new GregorianCalendar();
    startDate = new GregorianCalendar(today.get(Calendar.YEAR), today.get(Calendar.MONTH), 1).getTime();
    endDate = today.getTime();
    file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator 
        + defaultFilename());

    startDateButton.setText(dateDisplayFormat.format(startDate));
    endDateButton.setText(dateDisplayFormat.format(endDate));
    filenameButton.setText(file.getAbsolutePath());

    startDateButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
          public void onDateSet(DatePicker view, int year, int month, int day) {
          }
        };

        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        DialogFragment newFragment = new DatePickerFragment(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), listener);
        newFragment.show(NetworkLog.instance.getSupportFragmentManager(), "datePicker");
      }
    });

    endDateButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
          public void onDateSet(DatePicker view, int year, int month, int day) {
          }
        };

        Calendar cal = Calendar.getInstance();
        cal.setTime(endDate);
        DialogFragment newFragment = new DatePickerFragment(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), listener);
        newFragment.show(NetworkLog.instance.getSupportFragmentManager(), "datePicker");
      }
    });

    filenameButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        OnHandleFileListener saveListener = new OnHandleFileListener() {
          public void handleFile(final String filePath) {
            file = new File(filePath);
            filenameButton.setText(filePath);
          }
        };
        new FileSelector(context, FileOperation.SAVE, saveListener, defaultFilename(), new String[] { "*.*", "*.csv" }).show();
      }
    });

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(res.getString(R.string.export_title))
      .setView(view)
      .setCancelable(true)
      .setPositiveButton(res.getString(R.string.export_button), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface d, int id) {
          // see show() method for implementation -- avoids dismiss() unless validation passes
        }
      })
    .setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface d, int id) {
        dialog.cancel();
        dialog = null;
      }
    });
    dialog = builder.create();
  }

  private String defaultFilename() {
    return "networklog-" + dateFilenameFormat.format(startDate) + "-" + dateFilenameFormat.format(endDate) + ".csv";
  }

  public void setStartDate(Date date) {
  }

  public void setEndDate(Date date) {
  }

  public void setFilename(File file) {
  }

  public void show() {
    if(dialog != null) {
      dialog.show();
      dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          dialog.dismiss();
          dialog = null;
          // do export
        }
      });
    }
  }

  public void dismiss() {
    if(dialog != null) {
      dialog.dismiss();
      dialog = null;
    }
  }

  private class DatePickerFragment extends DialogFragment {
    int year;
    int month;
    int day;
    DatePickerDialog.OnDateSetListener listener;

    public DatePickerFragment(int year, int month, int day, DatePickerDialog.OnDateSetListener listener) {
      this(year, month, day);
      this.listener = listener;
    }

    public DatePickerFragment(int year, int month, int day) {
      this.year = year;
      this.month = month;
      this.day = day;
    }

    public void setListener(DatePickerDialog.OnDateSetListener listener) {
      this.listener = listener;
    }

    @Override
      public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new DatePickerDialog(getActivity(), this.listener, this.year, this.month, this.day);
      }
  }

}
