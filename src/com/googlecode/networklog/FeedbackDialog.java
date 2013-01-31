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
import android.net.Uri;
import android.widget.EditText;
import android.view.View;
import android.view.LayoutInflater;
import android.os.Build;

public class FeedbackDialog
{
  public EditText message;
  public AlertDialog dialog;
  private Context context;

  public FeedbackDialog(final Context context)
  {
    this.context = context;
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    View view = inflater.inflate(R.layout.feedbackdialog, null);

    message = (EditText) view.findViewById(R.id.feedbackMessage);

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle("Send Feedback")
      .setView(view)
      .setCancelable(true)
      .setPositiveButton("Send", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface d, int id) {
          // see show() method for implementation -- avoids dismiss() unless validation passes
        }
      })
    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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

  public void show() {
    if(dialog != null) {
      dialog.show();
      dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          String msg = message.getText().toString().trim();

          if(msg.length() == 0) {
            Iptables.showError(context, "No message", "Please enter a message, or use the Cancel button.");
            return;
          }

          dialog.dismiss();
          dialog = null;

          try {
            msg += "\n\nNetworkLog " + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
          } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            msg += "\n\nNetworkLog unknown version";
          }
          msg += "\nAndroid " + Build.VERSION.RELEASE;
          msg += "\nDevice " + Build.MANUFACTURER + " " + Build.MODEL + " " + Build.PRODUCT + " " + Build.BRAND;
          msg += "\n" + Build.DISPLAY;
          msg += "\nCPU " + Build.CPU_ABI + " " + Build.CPU_ABI2;

          Intent sendIntent = new Intent(Intent.ACTION_SENDTO);
          sendIntent.setData(Uri.parse("mailto:pragma78@gmail.com?subject=[NetworkLog] Bug report/feedback&body=" + msg));
          context.startActivity(Intent.createChooser(sendIntent, "Send Bug Report/Feedback"));
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
}
