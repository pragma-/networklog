/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

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

public class FilterDialog implements DialogInterface.OnDismissListener
{
  EditText editTextInclude;
  CheckBox checkboxUidInclude;
  CheckBox checkboxNameInclude;
  CheckBox checkboxAddressInclude;
  CheckBox checkboxPortInclude;
  CheckBox checkboxInterfaceInclude;
  CheckBox checkboxProtocolInclude;

  EditText editTextExclude;
  CheckBox checkboxUidExclude;
  CheckBox checkboxNameExclude;
  CheckBox checkboxAddressExclude;
  CheckBox checkboxPortExclude;
  CheckBox checkboxInterfaceExclude;
  CheckBox checkboxProtocolExclude;
  Context context;

  public FilterDialog(final Context context)
  {
    this.context = context;
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    View view = inflater.inflate(R.layout.filterdialog, null);

    editTextInclude = (EditText) view.findViewById(R.id.filterTextInclude);
    checkboxUidInclude = (CheckBox) view.findViewById(R.id.filterUidInclude);
    checkboxNameInclude = (CheckBox) view.findViewById(R.id.filterNameInclude);
    checkboxAddressInclude = (CheckBox) view.findViewById(R.id.filterAddressInclude);
    checkboxPortInclude = (CheckBox) view.findViewById(R.id.filterPortInclude);
    checkboxInterfaceInclude = (CheckBox) view.findViewById(R.id.filterInterfaceInclude);
    checkboxProtocolInclude = (CheckBox) view.findViewById(R.id.filterProtocolInclude);

    editTextExclude = (EditText) view.findViewById(R.id.filterTextExclude);
    checkboxUidExclude = (CheckBox) view.findViewById(R.id.filterUidExclude);
    checkboxNameExclude = (CheckBox) view.findViewById(R.id.filterNameExclude);
    checkboxAddressExclude = (CheckBox) view.findViewById(R.id.filterAddressExclude);
    checkboxPortExclude = (CheckBox) view.findViewById(R.id.filterPortExclude);
    checkboxInterfaceExclude = (CheckBox) view.findViewById(R.id.filterInterfaceExclude);
    checkboxProtocolExclude = (CheckBox) view.findViewById(R.id.filterProtocolExclude);

    editTextInclude.setText(NetworkLog.filterTextInclude);
    checkboxUidInclude.setChecked(NetworkLog.filterUidInclude);
    checkboxNameInclude.setChecked(NetworkLog.filterNameInclude);
    checkboxAddressInclude.setChecked(NetworkLog.filterAddressInclude);
    checkboxPortInclude.setChecked(NetworkLog.filterPortInclude);
    checkboxInterfaceInclude.setChecked(NetworkLog.filterInterfaceInclude);
    checkboxProtocolInclude.setChecked(NetworkLog.filterProtocolInclude);

    editTextExclude.setText(NetworkLog.filterTextExclude);
    checkboxUidExclude.setChecked(NetworkLog.filterUidExclude);
    checkboxNameExclude.setChecked(NetworkLog.filterNameExclude);
    checkboxAddressExclude.setChecked(NetworkLog.filterAddressExclude);
    checkboxPortExclude.setChecked(NetworkLog.filterPortExclude);
    checkboxInterfaceExclude.setChecked(NetworkLog.filterInterfaceExclude);
    checkboxProtocolExclude.setChecked(NetworkLog.filterProtocolExclude);

    CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener()
    {
      public void onCheckedChanged(CompoundButton button, boolean isChecked)
      {
        if(button == checkboxUidInclude) {
          NetworkLog.filterUidInclude = isChecked;
        } else if(button == checkboxNameInclude) {
          NetworkLog.filterNameInclude = isChecked;
        } else if(button == checkboxAddressInclude) {
          NetworkLog.filterAddressInclude = isChecked;
        } else if(button == checkboxPortInclude) {
          NetworkLog.filterPortInclude = isChecked;
        } else if(button == checkboxInterfaceInclude) {
          NetworkLog.filterInterfaceInclude = isChecked;
        } else if(button == checkboxProtocolInclude) {
          NetworkLog.filterProtocolInclude = isChecked;
        }

        if(button == checkboxUidExclude) {
          NetworkLog.filterUidExclude = isChecked;
        } else if(button == checkboxNameExclude) {
          NetworkLog.filterNameExclude = isChecked;
        } else if(button == checkboxAddressExclude) {
          NetworkLog.filterAddressExclude = isChecked;
        } else if(button == checkboxPortExclude) {
          NetworkLog.filterPortExclude = isChecked;
        } else if(button == checkboxInterfaceExclude) {
          NetworkLog.filterInterfaceExclude = isChecked;
        } else if(button == checkboxProtocolExclude) {
          NetworkLog.filterProtocolExclude = isChecked;
        }
      }
    };

    checkboxUidInclude.setOnCheckedChangeListener(listener);
    checkboxNameInclude.setOnCheckedChangeListener(listener);
    checkboxAddressInclude.setOnCheckedChangeListener(listener);
    checkboxPortInclude.setOnCheckedChangeListener(listener);
    checkboxInterfaceInclude.setOnCheckedChangeListener(listener);
    checkboxProtocolInclude.setOnCheckedChangeListener(listener);

    checkboxUidExclude.setOnCheckedChangeListener(listener);
    checkboxNameExclude.setOnCheckedChangeListener(listener);
    checkboxAddressExclude.setOnCheckedChangeListener(listener);
    checkboxPortExclude.setOnCheckedChangeListener(listener);
    checkboxInterfaceExclude.setOnCheckedChangeListener(listener);
    checkboxProtocolExclude.setOnCheckedChangeListener(listener);

    TextWatcher filterTextIncludeWatcher = new TextWatcher()
    {
      public void afterTextChanged(Editable s) {}

      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      public void onTextChanged(CharSequence s, int start, int before, int count)
      {
        NetworkLog.filterTextInclude = s.toString().trim();
      }
    };

    editTextInclude.addTextChangedListener(filterTextIncludeWatcher);

    TextWatcher filterTextExcludeWatcher = new TextWatcher()
    {
      public void afterTextChanged(Editable s) {}

      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      public void onTextChanged(CharSequence s, int start, int before, int count)
      {
        NetworkLog.filterTextExclude = s.toString().trim();
      }
    };

    editTextExclude.addTextChangedListener(filterTextExcludeWatcher);

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle("Filter")
      .setView(view)
      .setCancelable(true)
      .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          NetworkLog.filterTextInclude = "";
          NetworkLog.filterTextIncludeList.clear();
          NetworkLog.filterUidInclude = false;
          NetworkLog.filterNameInclude = false;
          NetworkLog.filterAddressInclude = false;
          NetworkLog.filterPortInclude = false;
          NetworkLog.filterInterfaceInclude = false;
          NetworkLog.filterProtocolInclude = false;

          NetworkLog.filterTextExclude = "";
          NetworkLog.filterTextExcludeList.clear();
          NetworkLog.filterUidExclude = false;
          NetworkLog.filterNameExclude = false;
          NetworkLog.filterAddressExclude = false;
          NetworkLog.filterPortExclude = false;
          NetworkLog.filterInterfaceExclude = false;
          NetworkLog.filterProtocolExclude = false;
        }
      })
    .setNeutralButton("Done", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.dismiss();
      }
    })
    /* .setNegativeButton("Help", new DialogInterface.OnClickListener()
       {
       public void onClick(DialogInterface dialog, int id)
       {
       new FilterHelpDialog(context);
       }
       }) */;
    AlertDialog alert = builder.create();
    alert.setOnDismissListener(this);
    alert.show();
  }

  @Override
    public void onDismiss(DialogInterface dialog)
    {
      NetworkLog.settings.setFilterTextInclude(NetworkLog.filterTextInclude);
      NetworkLog.settings.setFilterUidInclude(NetworkLog.filterUidInclude);
      NetworkLog.settings.setFilterNameInclude(NetworkLog.filterNameInclude);
      NetworkLog.settings.setFilterAddressInclude(NetworkLog.filterAddressInclude);
      NetworkLog.settings.setFilterPortInclude(NetworkLog.filterPortInclude);
      NetworkLog.settings.setFilterInterfaceInclude(NetworkLog.filterInterfaceInclude);
      NetworkLog.settings.setFilterProtocolInclude(NetworkLog.filterProtocolInclude);

      NetworkLog.settings.setFilterTextExclude(NetworkLog.filterTextExclude);
      NetworkLog.settings.setFilterUidExclude(NetworkLog.filterUidExclude);
      NetworkLog.settings.setFilterNameExclude(NetworkLog.filterNameExclude);
      NetworkLog.settings.setFilterAddressExclude(NetworkLog.filterAddressExclude);
      NetworkLog.settings.setFilterPortExclude(NetworkLog.filterPortExclude);
      NetworkLog.settings.setFilterInterfaceExclude(NetworkLog.filterInterfaceExclude);
      NetworkLog.settings.setFilterProtocolExclude(NetworkLog.filterProtocolExclude);

      FilterUtils.buildList(NetworkLog.filterTextInclude, NetworkLog.filterTextIncludeList);
      FilterUtils.buildList(NetworkLog.filterTextExclude, NetworkLog.filterTextExcludeList);

      NetworkLog.appFragment.setFilter("");
      NetworkLog.logFragment.setFilter("");

      NetworkLog.updateStatusText();
    }
}
