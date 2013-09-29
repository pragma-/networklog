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
import android.app.ProgressDialog;
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
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.FutureTask;
import java.util.Date;
import java.util.GregorianCalendar;

import android.support.v4.app.DialogFragment;

import com.samsung.sprc.fileselector.*;

import au.com.bytecode.opencsv.CSVWriter;

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

  ProgressDialog progressDialog = null;
  int progress_max = 0;
  int progress = 0;
  boolean canceled = false;

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
            startDate = new GregorianCalendar(year, month, day).getTime();
            startDateButton.setText(dateDisplayFormat.format(startDate));
            file = new File((file.getParent() == null ? "" : file.getParent()) + File.separator + defaultFilename());
            filenameButton.setText(file.getAbsolutePath());
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
            endDate = new GregorianCalendar(year, month, day, 23, 59, 59).getTime();
            endDateButton.setText(dateDisplayFormat.format(endDate));
            file = new File((file.getParent() == null ? "" : file.getParent()) + File.separator + defaultFilename());
            filenameButton.setText(file.getAbsolutePath());
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
          exportLog(startDate, endDate, file);
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

  public FutureTask createProgressDialog(final Context context) {
    FutureTask futureTask = new FutureTask(new Runnable() {
      public void run() {
        progressDialog = new ProgressDialog(context);
        progressDialog.setIndeterminate(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(progress_max);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("");
        progressDialog.setMessage(context.getResources().getString(R.string.exporting_log));

        progressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            canceled = true;
          }
        });

        progressDialog.show();
        progressDialog.setProgress(progress);
      }
    }, null);

    NetworkLog.handler.post(futureTask);
    return futureTask;
  }

  public void exportLog(final Date startDate, final Date endDate, final File file) {
    MyLog.d("Exporting from " + dateFilenameFormat.format(startDate) + " to " + dateFilenameFormat.format(endDate) + " to path " + file.getAbsolutePath());

    final long end_timestamp = endDate.getTime();
    final LogfileLoader loader = new LogfileLoader();

    try {
      loader.openLogfile(NetworkLog.settings.getLogFile());
    } catch (FileNotFoundException fnfe) {
      SysUtils.showError(context, context.getResources().getString(R.string.export_error_title), "No logfile found at " + NetworkLog.settings.getLogFile());
      return;
    } catch (Exception e) {
      SysUtils.showError(context, context.getResources().getString(R.string.export_error_title), "Error opening logfile: " + e.getMessage());
      return;
    }

    try {
      final long length = loader.getLength();

      if(length == 0) {
        SysUtils.showError(context, context.getResources().getString(R.string.export_error_title), "Logfile empty -- nothing to export");
        return;
      }
      
      long possible_end_pos = loader.seekToTimestampPosition(endDate.getTime(), true);
      final long start_pos = loader.seekToTimestampPosition(startDate.getTime());

      if(possible_end_pos == -1) {
        possible_end_pos = length;
      }

      final long end_pos = possible_end_pos;

      if(start_pos == -1) {
        SysUtils.showError(context, context.getResources().getString(R.string.export_error_title), "No entries found at " + dateDisplayFormat.format(startDate));
        return;
      }

      progress_max = (int)(end_pos - start_pos);
      progress = 0;

      CSVWriter open_writer;
      try {
        open_writer = new CSVWriter(new FileWriter(file));
      } catch (Exception e) {
        SysUtils.showError(context, context.getResources().getString(R.string.export_error_title), "Error opening export file: " + e.getMessage());
        return;
      }
      final CSVWriter writer = open_writer;

      new Thread(new Runnable() {
        public void run() {
          try {
            FutureTask createDialog = createProgressDialog(context);
            createDialog.get(); // wait until createDialog task completes
          } catch (Exception e) {
            // ignored
          }

          LogEntry entry;
          long processed_so_far = 0;
          long progress_increment_size = (long)((end_pos - start_pos) * 0.01);
          long next_progress_increment = progress_increment_size;

          try {
            String[] entries = new String[11];

            entries[0] = "Timestamp";
            entries[1] = "AppName";
            entries[2] = "AppPackage";
            entries[3] = "AppUid";
            entries[4] = "In interface";
            entries[5] = "Out interface";
            entries[6] = "Source";
            entries[7] = "Source Port";
            entries[8] = "Destination";
            entries[9] = "Destination Port";
            entries[10] = "Length";

            writer.writeNext(entries);

            while(!canceled) {
              entry = loader.readEntry();

              processed_so_far = loader.getProcessedSoFar();

              if(processed_so_far >= next_progress_increment) {
                next_progress_increment += progress_increment_size;
                progress = (int)processed_so_far;
                if(progressDialog != null) {
                  progressDialog.setProgress(progress);
                }
              }

              if(entry == null) {
                // end of file
                break;
              }

              if(entry.timestamp > end_timestamp) {
                break;
              }

              entries[0] = Timestamp.getTimestamp(entry.timestamp);
              entries[1] = ApplicationsTracker.uidMap.get(entry.uidString).name;
              entries[2] = ApplicationsTracker.uidMap.get(entry.uidString).packageName;
              entries[3] = entry.uidString;
              entries[4] = entry.in;
              entries[5] = entry.out;
              entries[6] = entry.src;
              entries[7] = String.valueOf(entry.spt);
              entries[8] = entry.dst;
              entries[9] = String.valueOf(entry.dpt);
              entries[10] = String.valueOf(entry.len);

              writer.writeNext(entries);
            }
          } catch (Exception e) {
            SysUtils.showError(context, context.getResources().getString(R.string.export_error_title), "Error exporting logfile: " + e.getMessage());
          } finally {
            try {
              loader.closeLogfile();
            } catch (Exception e) {
              // ignored
            }

            try {
              writer.close();
            } catch (Exception e) {
              // ignored
            }

            NetworkLog.handler.post(new Runnable() {
              public void run() {
                if(progressDialog != null) {
                  progressDialog.dismiss();
                  progressDialog = null;
                }
              }
            });
          }
        }
      }, "ExportLogfile").start();
    } catch (Exception e) {
      SysUtils.showError(context, context.getResources().getString(R.string.export_error_title), "Error exporting logfile: " + e.getMessage());
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
