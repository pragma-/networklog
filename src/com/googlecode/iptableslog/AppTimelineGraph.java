package com.googlecode.iptableslog;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.GraphView.*;

public class AppTimelineGraph extends Activity
{
  @Override
    protected void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);

      boolean good_init = false;
      String app_uid = null;
      String src_addr;
      String src_port;
      String dst_addr;
      String dst_port;

      Bundle extras = getIntent().getExtras();

      if(extras != null)
      {
        app_uid = extras.getString("app_uid");
        src_addr = extras.getString("src_addr");
        src_port = extras.getString("src_port");
        dst_addr = extras.getString("dst_addr");
        dst_port = extras.getString("dst_port");
      }

      if(app_uid == null)
      {
        // alert dialog
        finish();
      }

      int index = IptablesLog.appView.getItemByAppUid(Integer.parseInt(app_uid));

      if(index < 0)
      {
        // alert dialog
        finish();
      }

      AppView.ListItem item = IptablesLog.appView.listDataBuffer.get(index);

      // always give data sorted by x values
      GraphView graphView = new LineGraphView(this, item.toString())
      {
        private SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        @Override
          protected String formatLabel(double value, boolean isValueX)
          {
            if (isValueX)
            {
              // convert from unix timestamp to human time format
              String result = formatter.format(new Date((long) value));
              //MyLog.d("x axis label: " + result);
              return result;
            }
            else
            {
              // y-axis
              String result = String.format("%.2f", value/1000.0f) + "KB";
              //MyLog.d("y axis label: " + result);
              return result;
            }
          }
      };

      MyLog.d("Adding data to graph for " + item);

      synchronized(item.uniqueHostsList)
      {
        ArrayList<String> list = new ArrayList<String>(item.uniqueHostsList.keySet());
        Collections.sort(list);
        Iterator<String> itr = list.iterator();
        int color = 0;
        while(itr.hasNext())
        {
          String host = itr.next();
          AppView.HostInfo info = item.uniqueHostsList.get(host);
          MyLog.d("Graphing series " + host + "; hostinfo: " + info);

          if(info.packetGraphBuffer.size() > 0)
          {
            MyLog.d("number of packets: " + info.packetGraphBuffer.size());
            GraphViewData[] seriesData = new GraphViewData[info.packetGraphBuffer.size()];

            int i = 0;
            for(PacketGraphItem data : info.packetGraphBuffer)
            {
                  MyLog.d("Adding data " + data.timestamp + "; " + data.len);
                  seriesData[i] = new GraphViewData(data.timestamp, data.len);
                  i++;
            }

            MyLog.d("Adding series data " + info);
            graphView.addSeries(new GraphViewSeries(info.toString(), Color.parseColor(getResources().getString(Colors.distinctColor[color])), seriesData));
            color++;
            if(color >= Colors.distinctColor.length)
            {
              color = 0;
            }
          }
        }
      }

      double minX = graphView.getMinX(true);
      double maxX = graphView.getMaxX(true);

      double viewSize = (maxX - minX) * 0.50f;
      double viewStart = maxX - viewSize;

      if(viewStart < minX)
      {
        viewStart = minX;
      }

      if(viewStart + viewSize > maxX)
      {
        viewSize = maxX - viewStart;
      }

      graphView.intervalStep = 1000;
      graphView.setViewPort(viewStart, viewSize);
      graphView.setScrollable(true);
      graphView.setScalable(true);
      graphView.setShowLegend(true);
      graphView.setLegendAlign(LegendAlign.MIDDLE);
      graphView.invalidate();
      setContentView(graphView);
    }
}
