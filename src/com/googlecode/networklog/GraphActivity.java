package com.googlecode.networklog;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView.OnItemClickListener;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.LayoutInflater;
import android.util.Log;

import java.lang.Runnable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.jjoe64.graphview.GraphView.*;

public abstract class GraphActivity extends Activity
{
  public MyGraphView graphView;
  public ArrayList<LegendItem> legendData;
  public double interval;
  public double viewsize;

  public abstract void buildLegend(Context context);
  public abstract void buildSeries(double timeFrameSize, double viewSize);

  public class LegendItem {
    Drawable mIcon;
    int mHashCode;
    String mName;
    boolean mEnabled;
    double size;
  }

  private CustomAdapter adapter;
  private Spinner intervalSpinner;
  private Spinner viewsizeSpinner;
  private String[] intervalValues;
  private double[] intervalValuesDouble;
  private int spinnerInit = 0;
  private int lastViewsizePos = -1;
  private double viewportStart;

  protected class InstanceData {
    int spinnerInit;
    int lastViewsizePos;
    double interval;
    double viewsize;
    ArrayList<LegendItem> legendData;
    String[] intervalValues;
    double[] intervalValuesDouble;
    double viewportStart;
  }

  public InstanceData instanceData = null;

  @Override
    public Object onRetainNonConfigurationInstance() {
      instanceData = new InstanceData();

      instanceData.lastViewsizePos = lastViewsizePos;
      instanceData.spinnerInit = spinnerInit;
      instanceData.interval = interval;
      instanceData.viewsize = viewsize;
      instanceData.legendData = legendData;
      instanceData.intervalValues = intervalValues;
      instanceData.intervalValuesDouble = intervalValuesDouble;
      instanceData.viewportStart = graphView.viewportStart;

      return instanceData;
    }

  @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      requestWindowFeature(Window.FEATURE_NO_TITLE);
      setContentView(R.layout.graph_main);

      instanceData = (InstanceData) getLastNonConfigurationInstance();

      if(instanceData != null) {
        intervalValues = instanceData.intervalValues;
        intervalValuesDouble = instanceData.intervalValuesDouble;
        legendData = instanceData.legendData;
        viewsize = instanceData.viewsize;
        interval = instanceData.interval;
        spinnerInit = instanceData.spinnerInit;
        lastViewsizePos = instanceData.lastViewsizePos;
      } else {
        legendData = new ArrayList<LegendItem>();
        interval = NetworkLog.settings.getGraphInterval();
        viewsize = NetworkLog.settings.getGraphViewsize();

        intervalValues = getResources().getStringArray(R.array.interval_values);
        intervalValuesDouble = new double[intervalValues.length];

        for(int i = 0; i < intervalValues.length; i++) {
          intervalValuesDouble[i] = Double.valueOf(intervalValues[i]);
        }
      }
      
      graphView = (MyGraphView) findViewById(R.id.graph);
      graphView.setEnableMultiLineXLabel(true);
      graphView.setOnScaleChangeListener(new OnScaleChangeListener() {
        @Override
        public void scaleChanged(double newViewportSize) {
          viewsize = newViewportSize;
          for(int i = intervalValuesDouble.length - 1; i >= 0; i--) {
            if(newViewportSize >= intervalValuesDouble[i]) {
              lastViewsizePos = i; // force onItemSelected() listener to ignore programmatical call
              viewsizeSpinner.setSelection(i);
              break;
            }
          }
        }
      });
      /* Unused code commented out and retained for example purposes */
      /*
      graphView.setOnScrollChangeListener(new MyOnScrollChangeListener() {
        @Override
        public void scrollChanged(double newViewportStart) {
          // unused in this app
        }
      });
      */
      graphView.setScrollable(true);
      graphView.setScalable(true);
      graphView.setShowLegend(false);
      graphView.setLegendSorter(new Runnable() {
        public void run() {
          sortLegend();
        }
      });

      ListView listView = (ListView) findViewById(R.id.graph_legend);
      adapter = new CustomAdapter(this, R.layout.graph_legend_item, legendData);
      listView.setAdapter(adapter);
      listView.setOnItemClickListener(new CustomOnItemClickListener());
      listView.setFastScrollEnabled(true);

      MyOnItemSelectedListener listener = new MyOnItemSelectedListener();

      intervalSpinner = (Spinner) findViewById(R.id.intervalSpinner);
      intervalSpinner.setOnItemSelectedListener(listener);
      ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
          this, R.array.interval_entries, android.R.layout.simple_spinner_item);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      intervalSpinner.setAdapter(adapter);

      viewsizeSpinner = (Spinner) findViewById(R.id.viewsizeSpinner);
      viewsizeSpinner.setOnItemSelectedListener(listener);
      adapter = ArrayAdapter.createFromResource(
          this, R.array.interval_entries, android.R.layout.simple_spinner_item);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      viewsizeSpinner.setAdapter(adapter);

      int length = intervalValues.length;
      String intervalString = String.valueOf((int)interval);
      String viewsizeString = String.valueOf((int)viewsize);

      for(int i = 0; i < length; i++) {
        if(intervalString.equals(intervalValues[i])) {
          spinnerInit++;
          intervalSpinner.setSelection(i);
          break;
        }
      }

      for(int i = 0; i < length; i++) {
        if(viewsizeString.equals(intervalValues[i])) {
          spinnerInit++;
          viewsizeSpinner.setSelection(i);
          break;
        }
      }
    }

  public class MyOnItemSelectedListener implements OnItemSelectedListener {
    @Override
      public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        if(spinnerInit > 0) {
          // Don't process selection event if spinner is initializing
          spinnerInit--;
          return;
        }

        if(parent == intervalSpinner) {
          interval = Double.parseDouble(intervalValues[pos]);
          MyLog.d("Setting interval " + pos + ", " + interval);
          NetworkLog.settings.setGraphInterval((long)interval);
        } else {
          if(lastViewsizePos == pos) {
            // Skip programmatical call to setSelected()
            return;
          }
          viewsize = Double.parseDouble(intervalValues[pos]);
          MyLog.d("Setting viewsize " + pos + ", " + viewsize);
          NetworkLog.settings.setGraphViewsize((long)viewsize);
          lastViewsizePos = pos;
        }
        buildSeries(interval, viewsize);
      }

    @Override
      public void onNothingSelected(AdapterView parent) {
        // do nothing
      }
  }

  private Comparator<LegendItem> legendSorter = new Comparator<LegendItem>() {
    public int compare(LegendItem o1, LegendItem o2) {
      return o1.size > o2.size ? -1 : (o1.size == o2.size) ? 0 : 1;
    }
  };

  HashMap<Integer, Double> legendMap = new HashMap<Integer, Double>();

  public void sortLegend() {
    for(GraphViewSeries series : graphView.graphSeries) {
      legendMap.put(series.id, series.size);
    }

    // FIXME: update hashcode when resolving host addr
    Double size;
    synchronized(legendData) {
      for(LegendItem item : legendData) {
        size = legendMap.get(item.mHashCode);
        item.size = size == null ? 0 : size;
      }

      Collections.sort(legendData, legendSorter);

      adapter.notifyDataSetChanged();
    }
  }

  private class CustomOnItemClickListener implements OnItemClickListener {
    @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LegendItem item = legendData.get(position);
        item.mEnabled = !item.mEnabled;

        CheckedTextView ctv = (CheckedTextView) view.findViewById(R.id.legendName);
        ctv.setChecked(item.mEnabled);

        graphView.setSeriesEnabled(item.mHashCode, item.mEnabled);
        graphView.invalidateLabels();
        graphView.invalidate();
      }
  }

  private class CustomAdapter extends ArrayAdapter<LegendItem> {
    LayoutInflater mInflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

    public CustomAdapter(Context context, int resource, List<LegendItem> objects) {
      super(context, resource, objects);
    }

    @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;

        ImageView icon;
        CheckedTextView name;

        LegendItem item = getItem(position);

        if(convertView == null) {
          convertView = mInflater.inflate(R.layout.graph_legend_item, null);
          holder = new ViewHolder(convertView);
          convertView.setTag(holder);
        } else {
          holder = (ViewHolder) convertView.getTag();
        }

        icon = holder.getIcon();
        icon.setImageDrawable(item.mIcon);

        name = holder.getName();
        name.setText(item.mName);
        name.setChecked(item.mEnabled);

        return convertView;
      }
  }

  private class ViewHolder {
    private View mView;
    private ImageView mIcon;
    private CheckedTextView mName;

    public ViewHolder(View view) {
      mView = view;
    }

    public ImageView getIcon() {
      if(mIcon == null) {
        mIcon = (ImageView) mView.findViewById(R.id.legendIcon);
      }

      return mIcon;
    }

    public CheckedTextView getName() {
      if(mName == null) {
        mName = (CheckedTextView) mView.findViewById(R.id.legendName);
      }

      return mName;
    }
  }
}
