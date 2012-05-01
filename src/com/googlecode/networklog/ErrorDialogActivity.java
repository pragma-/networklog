package com.googlecode.networklog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

public class ErrorDialogActivity extends Activity {
  @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      Bundle extras = getIntent().getExtras();

      String title = "Error";
      String message = "Some error occurred";

      if(extras != null) {
        title = extras.getString("title");
        message = extras.getString("message");
      }

      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(title)
        .setMessage(message)
        .setCancelable(false)
        .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            finish();
          }
        });
      AlertDialog alert = builder.create();
      alert.show();
    }
}
