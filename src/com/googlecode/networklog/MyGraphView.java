package com.googlecode.networklog;

import android.content.Context;
import android.util.AttributeSet;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.jjoe64.graphview.LineGraphView;

public class MyGraphView extends LineGraphView {
  private SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");

  public MyGraphView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public MyGraphView(Context context, String title) {
    super(context, title);
  }

  @Override
    protected String formatLabel(double value, boolean isValueX)
    {
      if(isValueX)
      {
        String result = formatter.format(new Date((long) value));
        return result;
      }
      else
      {
        // y-axis
        String result;
        if(value >= 1000000) {
          result = String.format("%.1f", value / 1000000.0f) + "M";
        } else if(value >= 1000) {
          result = String.format("%.1f", value / 1000.0f) + "K";
        } else {
          int v = (int)value;
          if(v == 1) {
            v = 0;
          }
          result = String.valueOf(v);
        }

        return result;
      }
    }
}
