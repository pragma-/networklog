/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.widget.CheckBox;
import android.widget.EditText;
import android.view.View;
import android.view.LayoutInflater;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class FeedbackDialog
{
  public EditText message;
  public CheckBox attachLogcat;
  public AlertDialog dialog;
  private Context context;

  public FeedbackDialog(final Context context)
  {
    this.context = context;
    Resources res = context.getResources();
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    View view = inflater.inflate(R.layout.feedbackdialog, null);

    message = (EditText) view.findViewById(R.id.feedbackMessage);
    attachLogcat = (CheckBox) view.findViewById(R.id.attachLogcat);

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(res.getString(R.string.feedback_title))
      .setView(view)
      .setCancelable(true)
      .setPositiveButton(res.getString(R.string.feedback_send), new DialogInterface.OnClickListener() {
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

  public void setMessage(CharSequence text) {
    if(message != null) {
      message.setText(text);
    }
  }

  public void setCursorPosition(int position) {
    if(message != null) {
      message.setSelection(position);
    }
  }

  public void setAttachLogcat(boolean value) {
    if(attachLogcat != null) {
      attachLogcat.setChecked(value);
    }
  }

  public void show() {
    if(dialog != null) {
      dialog.show();
      dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          Resources res = context.getResources();
          String msg = message.getText().toString().trim();
          File logcat = null;

          if(msg.length() == 0) {
            SysUtils.showError(context, res.getString(R.string.feedback_error_no_message_title), res.getString(R.string.feedback_error_no_message_text));
            return;
          }

          if(attachLogcat.isChecked()) {
            try {
              logcat = generateLogcat();
            } catch(Exception e) {
              SysUtils.showError(context, res.getString(R.string.feedback_error_getting_debug_log), e.toString());
              return;
            }
          }

          dialog.dismiss();
          dialog = null;

          sendFeedback(msg, logcat);
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

  public File generateLogcat() throws Exception {
    File logcat = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "netlog_logcat.txt");
    String path = logcat.getAbsolutePath();

    String iptablesBinary = SysUtils.getIptablesBinary(context);
    if(iptablesBinary == null) {
      throw new Exception(String.format(context.getResources().getString(R.string.error_unsupported_system_text), Build.CPU_ABI));
    }

    boolean hasRoot = SysUtils.checkRoot(context);

    InteractiveShell shell;

    if(hasRoot) {
      shell = NetworkLog.shell;
    } else {
      Log.d("NetworkLog", "No root shell, creating standard shell");
      shell = new InteractiveShell("sh", "GenerateLogcat");
      shell.start();
      // FIXME: devise better method to check for exit that doesn't involve arbitrary sleeping
      try { Thread.sleep(500); } catch (Exception e) {}
      if(shell.hasError()) {
        String error = shell.getError(true);
        Log.e("NetworkLog", "Error creating standard shell: " + error);
        throw new Exception("Error creating shell: " + error);
      }
      if(shell.checkForExit()) {
        Log.e("NetworkLog", "Error creating standard shell: shell exited with code " + shell.exitval);
        throw new Exception("Error creating shell: exited with code " + shell.exitval);
      }
    }

    shell.sendCommand("logcat -d -v time > " + path, true);
    shell.sendCommand("echo === uname: >> " + path + " 2>&1", true);
    shell.sendCommand("uname -a >> " + path + " 2>&1", true);

    if(hasRoot) {
      shell.sendCommand("echo === ip_tables_matches: >> " + path + " 2>&1", true);
      shell.sendCommand("cat /proc/net/ip_tables_matches >> " + path + " 2>&1", true);
      shell.sendCommand("echo === ip_tables_names: >> " + path + " 2>&1", true);
      shell.sendCommand("cat /proc/net/ip_tables_names >> " + path + " 2>&1", true);
      shell.sendCommand("echo === ip_tables_targets: >> " + path + " 2>&1", true);
      shell.sendCommand("cat /proc/net/ip_tables_targets >> " + path + " 2>&1", true);
      shell.sendCommand("echo === iptables: >> " + path + " 2>&1", true);
      shell.sendCommand(iptablesBinary + " -L -v >> " + path + " 2>&1", true);
    } else {
      shell.sendCommand("echo === not rooted >> " + path + " 2>&1", true);
      shell.close();
    }

    return logcat;
  }

  public void sendFeedback(String message, File logcat) {
    StringBuilder msg = new StringBuilder(message.length() + 512);

    msg.append(message);

    try {
      msg.append("\n\nNetworkLog " + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
    } catch (android.content.pm.PackageManager.NameNotFoundException e) {
      msg.append("\n\nNetworkLog unknown version");
    }
    msg.append("\nAndroid " + Build.VERSION.RELEASE + (SysUtils.checkRoot(context) ? " rooted" : ""));
    msg.append("\nDevice " + Build.MANUFACTURER + " " + Build.MODEL + " " + Build.PRODUCT + " " + Build.BRAND);
    msg.append("\nKernel " + System.getProperty("os.version"));
    msg.append("\n" + Build.DISPLAY);
    msg.append("\nCPU " + Build.CPU_ABI + " " + Build.CPU_ABI2);

    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{ "pragma78@gmail.com" });
    intent.putExtra(Intent.EXTRA_SUBJECT, "[NetworkLog] Bug report/feedback");
    intent.putExtra(Intent.EXTRA_TEXT, msg.toString());
    intent.setType("message/rfc822");

    if(logcat != null) {
      intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logcat));
    }

    context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.feedback_chooser_title)));
  }
}
