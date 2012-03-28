package com.googlecode.iptableslog;

import android.app.Activity;
import android.content.Context;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.text.TextWatcher;
import android.text.Editable;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.view.View;
import android.view.LayoutInflater;
import android.text.Html;

public class FilterDialog implements DialogInterface.OnDismissListener {
  EditText editText;
  CheckBox checkboxUid;
  CheckBox checkboxName;
  CheckBox checkboxAddress;
  CheckBox checkboxPort;

  public FilterDialog(final Context context) {
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    View view = inflater.inflate(R.layout.filterdialog, null);

    editText = (EditText) view.findViewById(R.id.filterText); 
    checkboxUid = (CheckBox) view.findViewById(R.id.filterUid);
    checkboxName = (CheckBox) view.findViewById(R.id.filterName);
    checkboxAddress = (CheckBox) view.findViewById(R.id.filterAddress);
    checkboxPort = (CheckBox) view.findViewById(R.id.filterPort);

    editText.setText(IptablesLog.filterText);
    checkboxUid.setChecked(IptablesLog.filterUid);
    checkboxName.setChecked(IptablesLog.filterName);
    checkboxAddress.setChecked(IptablesLog.filterAddress);
    checkboxPort.setChecked(IptablesLog.filterPort);

    CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton button, boolean isChecked) {
        if(button == checkboxUid) {
          IptablesLog.filterUid = isChecked;
        } else if(button == checkboxName) {
          IptablesLog.filterName = isChecked;
        } else if(button == checkboxAddress) {
          IptablesLog.filterAddress = isChecked;
        } else if(button == checkboxPort) {
          IptablesLog.filterPort = isChecked;
        }

        IptablesLog.appView.setFilter(IptablesLog.filterText);
        IptablesLog.logView.setFilter(IptablesLog.filterText);
      }
    };

    checkboxUid.setOnCheckedChangeListener(listener);
    checkboxName.setOnCheckedChangeListener(listener);
    checkboxAddress.setOnCheckedChangeListener(listener);
    checkboxPort.setOnCheckedChangeListener(listener);
    
    TextWatcher filterTextWatcher = new TextWatcher() {
      public void afterTextChanged(Editable s) {}

      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      public void onTextChanged(CharSequence s, int start, int before, int count) {
        IptablesLog.appView.setFilter(s);
        IptablesLog.logView.setFilter(s);
      }
    };

    editText.addTextChangedListener(filterTextWatcher);

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle("Filter")
      .setView(view)
      .setCancelable(true)
      .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          IptablesLog.filterText = "";
          IptablesLog.filterUid = true;
          IptablesLog.filterName = true;
          IptablesLog.filterAddress = true;
          IptablesLog.filterPort = true;
          IptablesLog.appView.setFilter("");
          IptablesLog.logView.setFilter("");
        }
      })
    .setNeutralButton("Done", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          dialog.dismiss();
        }
      })
   /* .setNegativeButton("Help", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          new FilterHelpDialog(context);
        }
      }) */;
    AlertDialog alert = builder.create();
    alert.setOnDismissListener(this);
    alert.show();
  }
   
  @Override
    public void onDismiss(DialogInterface dialog) {
      IptablesLog.settings.setFilterText(IptablesLog.filterText.toString());
      IptablesLog.settings.setFilterUid(IptablesLog.filterUid);
      IptablesLog.settings.setFilterName(IptablesLog.filterName);
      IptablesLog.settings.setFilterAddress(IptablesLog.filterAddress);
      IptablesLog.settings.setFilterPort(IptablesLog.filterPort);
    }

  /*
  public class FilterHelpDialog {
    public FilterHelpDialog(final Context context) {
      AlertDialog.Builder builder = new AlertDialog.Builder(context);
      builder.setTitle("Filter Help")
        .setMessage(Html.fromHtml(context.getResources().getString(R.string.filterHelp)))
        .setCancelable(true)
        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            IptablesLog.handler.post(new Runnable() {
              public void run() {
                new FilterDialog(context);
              }
            });
          }
        });
      AlertDialog alert = builder.create();
      alert.show();
    }
  }
  */
}
