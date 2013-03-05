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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class SelectToastApps implements DialogInterface.OnDismissListener
{
  Context context;
  ArrayList<AppItem> appData;
  CustomAdapter adapter;
  boolean dialogShowing;

  class AppItem {
    Drawable icon;
    String name;
    boolean enabled;
  }

  public void showDialog(final Context context)
  {
    if(dialogShowing) {
      // do nothing
      return;
    }

    this.context = context;
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    View view = inflater.inflate(R.layout.select_apps, null);

    appData = new ArrayList<AppItem>();


    ListView listView = (ListView) view.findViewById(R.id.select_apps);
    adapter = new CustomAdapter(context, R.layout.select_apps_item, appData);
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(new CustomOnItemClickListener());
    listView.setFastScrollEnabled(true);

    ((Button) view.findViewById(R.id.select_all)).setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
      }
    });

    ((Button) view.findViewById(R.id.select_none)).setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
      }
    });

    Resources res = context.getResources();
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(res.getString(R.string.pref_choose_apps))
      .setView(view)
      .setCancelable(false)
      .setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          dialog.dismiss();
        }
      })
    .setPositiveButton(res.getString(R.string.done), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        /* apply and save selections */
        dialog.dismiss();
      }
    });

    AlertDialog alert = builder.create();
    alert.setOnDismissListener(this);
    alert.show();
  }

  @Override
    public void onDismiss(DialogInterface dialog) {
      dialogShowing = false;
      NetworkLog.selectToastApps = null;
    }

  private class CustomOnItemClickListener implements OnItemClickListener {
    @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AppItem item = appData.get(position);
        item.enabled = !item.enabled;

        CheckedTextView ctv = (CheckedTextView) view.findViewById(R.id.select_apps_name);
        ctv.setChecked(item.enabled);
      }
  }

  private class CustomAdapter extends ArrayAdapter<AppItem> {
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

    public CustomAdapter(Context context, int resource, List<AppItem> objects) {
      super(context, resource, objects);
    }

    @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        ImageView icon;
        CheckedTextView name;

        AppItem item = getItem(position);

        if(convertView == null) {
          convertView = inflater.inflate(R.layout.select_apps_item, null);
          holder = new ViewHolder(convertView);
          convertView.setTag(holder);
        } else {
          holder = (ViewHolder) convertView.getTag();
        }

        icon = holder.getIcon();
        icon.setImageDrawable(item.icon); // ApplicationsTracker.getIcon()

        name = holder.getName();
        name.setText(item.name);
        name.setChecked(item.enabled);

        return convertView;
      }
  }

  private class ViewHolder {
    private View view;
    private ImageView icon;
    private CheckedTextView name;

    public ViewHolder(View view) {
      this.view = view;
    }

    public ImageView getIcon() {
      if(icon == null) {
        icon = (ImageView) view.findViewById(R.id.select_apps_icon);
      }

      return icon;
    }

    public CheckedTextView getName() {
      if(name == null) {
        name = (CheckedTextView) view.findViewById(R.id.select_apps_name);
      }

      return name;
    }
  }
}
