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

public class FilterDialog implements DialogInterface.OnDismissListener 
{
  EditText editTextInclude;
  CheckBox checkboxUidInclude;
  CheckBox checkboxNameInclude;
  CheckBox checkboxAddressInclude;
  CheckBox checkboxPortInclude;

  EditText editTextExclude;
  CheckBox checkboxUidExclude;
  CheckBox checkboxNameExclude;
  CheckBox checkboxAddressExclude;
  CheckBox checkboxPortExclude;

  public FilterDialog(final Context context) 
  {
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    View view = inflater.inflate(R.layout.filterdialog, null);

    editTextInclude = (EditText) view.findViewById(R.id.filterTextInclude); 
    checkboxUidInclude = (CheckBox) view.findViewById(R.id.filterUidInclude);
    checkboxNameInclude = (CheckBox) view.findViewById(R.id.filterNameInclude);
    checkboxAddressInclude = (CheckBox) view.findViewById(R.id.filterAddressInclude);
    checkboxPortInclude = (CheckBox) view.findViewById(R.id.filterPortInclude);

    editTextExclude = (EditText) view.findViewById(R.id.filterTextExclude); 
    checkboxUidExclude = (CheckBox) view.findViewById(R.id.filterUidExclude);
    checkboxNameExclude = (CheckBox) view.findViewById(R.id.filterNameExclude);
    checkboxAddressExclude = (CheckBox) view.findViewById(R.id.filterAddressExclude);
    checkboxPortExclude = (CheckBox) view.findViewById(R.id.filterPortExclude);

    editTextInclude.setText(IptablesLog.filterTextInclude);
    checkboxUidInclude.setChecked(IptablesLog.filterUidInclude);
    checkboxNameInclude.setChecked(IptablesLog.filterNameInclude);
    checkboxAddressInclude.setChecked(IptablesLog.filterAddressInclude);
    checkboxPortInclude.setChecked(IptablesLog.filterPortInclude);

    editTextExclude.setText(IptablesLog.filterTextExclude);
    checkboxUidExclude.setChecked(IptablesLog.filterUidExclude);
    checkboxNameExclude.setChecked(IptablesLog.filterNameExclude);
    checkboxAddressExclude.setChecked(IptablesLog.filterAddressExclude);
    checkboxPortExclude.setChecked(IptablesLog.filterPortExclude);

    CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() 
    {
      public void onCheckedChanged(CompoundButton button, boolean isChecked) 
      {
        if(button == checkboxUidInclude) 
        {
          IptablesLog.filterUidInclude = isChecked;
        } 
        else if(button == checkboxNameInclude)
        {
          IptablesLog.filterNameInclude = isChecked;
        } 
        else if(button == checkboxAddressInclude)
        {
          IptablesLog.filterAddressInclude = isChecked;
        } 
        else if(button == checkboxPortInclude)
        {
          IptablesLog.filterPortInclude = isChecked;
        }

        if(button == checkboxUidExclude)
        {
          IptablesLog.filterUidExclude = isChecked;
        } 
        else if(button == checkboxNameExclude)
        {
          IptablesLog.filterNameExclude = isChecked;
        } 
        else if(button == checkboxAddressExclude)
        {
          IptablesLog.filterAddressExclude = isChecked;
        } 
        else if(button == checkboxPortExclude)
        {
          IptablesLog.filterPortExclude = isChecked;
        }
      }
    };

    checkboxUidInclude.setOnCheckedChangeListener(listener);
    checkboxNameInclude.setOnCheckedChangeListener(listener);
    checkboxAddressInclude.setOnCheckedChangeListener(listener);
    checkboxPortInclude.setOnCheckedChangeListener(listener);

    checkboxUidExclude.setOnCheckedChangeListener(listener);
    checkboxNameExclude.setOnCheckedChangeListener(listener);
    checkboxAddressExclude.setOnCheckedChangeListener(listener);
    checkboxPortExclude.setOnCheckedChangeListener(listener);

    TextWatcher filterTextIncludeWatcher = new TextWatcher()
    {
      public void afterTextChanged(Editable s) {}

      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      public void onTextChanged(CharSequence s, int start, int before, int count)
      {
        IptablesLog.filterTextInclude = s.toString().trim();
      }
    };

    editTextInclude.addTextChangedListener(filterTextIncludeWatcher);

    TextWatcher filterTextExcludeWatcher = new TextWatcher()
    {
      public void afterTextChanged(Editable s) {}

      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      public void onTextChanged(CharSequence s, int start, int before, int count)
      {
        IptablesLog.filterTextExclude = s.toString().trim();
      }
    };

    editTextExclude.addTextChangedListener(filterTextExcludeWatcher);

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle("Filter")
      .setView(view)
      .setCancelable(true)
      .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          IptablesLog.filterTextInclude = "";
          IptablesLog.filterTextIncludeList.clear();
          IptablesLog.filterUidInclude = false;
          IptablesLog.filterNameInclude = false;
          IptablesLog.filterAddressInclude = false;
          IptablesLog.filterPortInclude = false;

          IptablesLog.filterTextExclude = "";
          IptablesLog.filterTextExcludeList.clear();
          IptablesLog.filterUidExclude = false;
          IptablesLog.filterNameExclude = false;
          IptablesLog.filterAddressExclude = false;
          IptablesLog.filterPortExclude = false;
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
      IptablesLog.settings.setFilterTextInclude(IptablesLog.filterTextInclude);
      IptablesLog.settings.setFilterUidInclude(IptablesLog.filterUidInclude);
      IptablesLog.settings.setFilterNameInclude(IptablesLog.filterNameInclude);
      IptablesLog.settings.setFilterAddressInclude(IptablesLog.filterAddressInclude);
      IptablesLog.settings.setFilterPortInclude(IptablesLog.filterPortInclude);

      IptablesLog.settings.setFilterTextExclude(IptablesLog.filterTextExclude);
      IptablesLog.settings.setFilterUidExclude(IptablesLog.filterUidExclude);
      IptablesLog.settings.setFilterNameExclude(IptablesLog.filterNameExclude);
      IptablesLog.settings.setFilterAddressExclude(IptablesLog.filterAddressExclude);
      IptablesLog.settings.setFilterPortExclude(IptablesLog.filterPortExclude);

      FilterUtils.buildList(IptablesLog.filterTextInclude, IptablesLog.filterTextIncludeList);
      FilterUtils.buildList(IptablesLog.filterTextExclude, IptablesLog.filterTextExcludeList);

      IptablesLog.appView.setFilter("");
      IptablesLog.logView.setFilter("");
    }
}
