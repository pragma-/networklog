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
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


public abstract class AppsSelector
{
  Context context;
  ArrayList<AppItem> appData;
  CustomAdapter adapter;
  HashMap<String, String> apps;
  AlertDialog dialog;
  String name;

  class AppItem {
    Drawable icon;
    String name;
    String packageName;
    boolean enabled;
  }

  protected abstract File getSaveFile(Context context);
  protected abstract void negativeButton();
  protected abstract void positiveButton();

  public HashMap<String, String> loadBlockedApps(Context context) {
    File file = getSaveFile(context);
    HashMap<String, String> map = new HashMap<String, String>();

    if(!file.exists()) {
      return map;
    }

    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line;
      while ((line = br.readLine()) != null) {
        map.put(line, line);
      }
      br.close();
    } catch(Exception e) {
      Log.w("NetworkLog", "Exception loading " + name + ": " + e);
      SysUtils.showError(context, "Error loading " + name, e.getMessage());
      return map;
    }
    return map;
  }

  public void saveBlockedApps(Context context, HashMap<String, String> map) {
    File file = getSaveFile(context);

    try {
      PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
      for(String key : map.keySet()) {
        writer.println(key);
      }
      writer.close();
    } catch(Exception e) {
      Log.w("NetworkLog", "Exception saving " + name + ": " + e);
      SysUtils.showError(context, "Error saving " + name, e.getMessage());
    }
  }

  protected static class SortAppsByName implements Comparator<AppItem> {
    public int compare(AppItem o1, AppItem o2) {
      return o1.name.compareToIgnoreCase(o2.name);
    }
  }

  public void showDialog(final Context context) {
    showDialog(context, null);
  }

  public void showDialog(final Context context, ArrayList<AppItem> data)
  {
    this.context = context;
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    View view = inflater.inflate(R.layout.select_apps, null);

    if(data == null) {
      appData = new ArrayList<AppItem>();

      for(ApplicationsTracker.AppEntry app : ApplicationsTracker.installedApps) {
        AppItem item = new AppItem();
        item.name = app.name;
        item.packageName = app.packageName;
        appData.add(item);
      }

      Collections.sort(appData, new SortAppsByName());

      apps = loadBlockedApps(context);
      if(apps != null) {
        for(AppItem item : appData) {
          if(apps.get(item.packageName) != null) {
            item.enabled = true;
          }
        }
      }
    } else {
      appData = data;
    }

    ListView listView = (ListView) view.findViewById(R.id.select_apps);
    adapter = new CustomAdapter(context, R.layout.select_apps_item, appData);
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(new CustomOnItemClickListener());
    listView.setFastScrollEnabled(true);

    ((Button) view.findViewById(R.id.select_all)).setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        for(AppItem item : appData) {
          item.enabled = true;
        }
        adapter.notifyDataSetChanged();
      }
    });

    ((Button) view.findViewById(R.id.select_none)).setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        for(AppItem item : appData) {
          item.enabled = false;
        }
        adapter.notifyDataSetChanged();
      }
    });

    Resources res = context.getResources();
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(res.getString(R.string.pref_toast_block_apps))
      .setView(view)
      .setCancelable(true)
      .setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          dialog.dismiss();
          negativeButton();
        }
      })
    .setPositiveButton(res.getString(R.string.done), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        apps = new HashMap<String, String>();

        for(AppItem item : appData) {
          if(item.enabled == true) {
            apps.put(item.packageName, item.packageName);
          }
        }
        saveBlockedApps(context, apps);
        dialog.dismiss();
        positiveButton();
      }
    });

    dialog = builder.create();
    dialog.show();
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
        icon.setTag(item.packageName);
        icon.setImageDrawable(ApplicationsTracker.loadIcon(context, icon, item.packageName));

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
