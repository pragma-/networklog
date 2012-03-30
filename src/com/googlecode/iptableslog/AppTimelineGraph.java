package com.googlecode.iptableslog;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

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
      LineGraphView graphView = new LineGraphView(this, item.toString())
      {
        private SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        @Override
          protected String formatLabel(double value, boolean isValueX)
          {
            if (isValueX)
            {
              // convert from unix timestamp to human time format
              String result = formatter.format(new Date((long) value));
              MyLog.d("x axis label: " + result);
              return result;
            }
            else
            {
              // y-axis, use default formatter
              String result = String.format("%.2f", value/1000.0f) + "KB";
              MyLog.d("y axis label: " + result);
              return result;
            }
          }
      };

      MyLog.d("Starting graph for " + item);

      ArrayList<String> list = new ArrayList<String>(item.uniqueHostsList.keySet());
      Collections.sort(list);
      Iterator<String> itr = list.iterator();
      int color = 0;
      while(itr.hasNext())
      {
        String host = itr.next();
        MyLog.d("Graphing " + host);
        AppView.HostInfo info = item.uniqueHostsList.get(host);

        if(info.packetGraphBuffer.size() > 0)
        {
          MyLog.d("number of packets: " + info.packetGraphBuffer.size());
          ArrayList<PacketGraphItem> graphData = new ArrayList<PacketGraphItem>();

          int timeFrameSize = 1000; // fixme: make user-definable via graph menu
          long nextTimeFrame = 0; 
          long frameLen = 0; // len for this time frame

          for(PacketGraphItem data : info.packetGraphBuffer)
          {
            MyLog.d("processing: " + data + "; nextTimeFrame: " + nextTimeFrame + "; frameLen: " + frameLen);
            if(nextTimeFrame == 0)
            {
              nextTimeFrame = data.timestamp + timeFrameSize;
              MyLog.d("setting nextTimeFrame: " + nextTimeFrame);
            }
            else if(data.timestamp > nextTimeFrame)
            {
              long time = nextTimeFrame;
              for(; time < data.timestamp; time += timeFrameSize)
              {
                MyLog.d("- plotting: (" + time + ", " + frameLen + ")");
                graphData.add(new PacketGraphItem(time, frameLen));
                frameLen = 0;
              }

              nextTimeFrame = time;
              MyLog.d("setting nextTimeFrame: " + nextTimeFrame);
            }

            frameLen += data.len;
            MyLog.d("Adding " + data.len + "; frameLen: " + frameLen);
          }

          MyLog.d("+ plotting: (" + nextTimeFrame + ", " + frameLen + ")");
          graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));
          
          MyLog.d("Adding series " + info);
          
          GraphViewData[] seriesData = new GraphViewData[graphData.size()];

          int i = 0;
          for(PacketGraphItem graphItem : graphData)
          {
            seriesData[i] = new GraphViewData(graphItem.timestamp, graphItem.len);
            i++;
          }

          graphView.addSeries(new GraphViewSeries(info.toString(), Color.parseColor(getResources().getString(Colors.distinctColor[color])), seriesData));
          color++;
          if(color >= Colors.distinctColor.length)
          {
            color = 0;
          }
        }
      }

      // ??? by 30 seconds viewport
      // graphView.setViewPort(0, 1000 * 30);  // fixme: allow user-defined viewport configuration
      graphView.setScrollable(true);
      graphView.setScalable(true);
      graphView.setDrawBackground(false);
      graphView.setShowLegend(true);
      graphView.setLegendAlign(LegendAlign.BOTTOM);
      // todo calculate length of max text in legend and set width accordingly
      // graphView.setLegendWidth(300);

      setContentView(graphView);
    }
}
