/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.content.Context;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.shapes.Shape;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.ShapeDrawable;
import android.util.Log;

import java.lang.Runnable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.GraphViewSeries;

public class AppTimelineGraph extends GraphActivity
{
  private String app_uid = null;
  private String src_addr;
  private int src_port;
  private String dst_addr;
  private int dst_port;

  @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      Bundle extras = getIntent().getExtras();

      if(extras != null) {
        app_uid = extras.getString("app_uid");
        src_addr = extras.getString("src_addr");
        src_port = extras.getInt("src_port");
        dst_addr = extras.getString("dst_addr");
        dst_port = extras.getInt("dst_port");
      }

      if(app_uid == null) {
        // alert dialog
        finish();
      }

      int index = NetworkLog.appFragment.getItemByAppUid(Integer.parseInt(app_uid));

      if(index < 0) {
        // alert dialog
        finish();
      }

      AppFragment.GroupItem item = NetworkLog.appFragment.groupDataBuffer.get(index);
      graphView.setTitle(item.toString() + " Timeline");

      if(instanceData == null) {
        buildLegend(this);
      }
      buildSeries(interval, viewsize);
    }

  public void buildLegend(Context context) {
    int index = NetworkLog.appFragment.getItemByAppUid(Integer.parseInt(app_uid));

    if(index < 0)
    {
      // alert dialog
      finish();
    }

    AppFragment.GroupItem item = NetworkLog.appFragment.groupDataBuffer.get(index);

    MyLog.d("Starting graph for " + item);

    synchronized(item.childrenData) {
      ArrayList<String> list = new ArrayList<String>(item.childrenData.keySet());
      Collections.sort(list);
      Iterator<String> itr = list.iterator();
      int color = 0;

      float density = context.getResources().getDisplayMetrics().density;
      Shape rect = new RectShape();

      while(itr.hasNext())
      {
        String host = itr.next();
        AppFragment.ChildItem info = item.childrenData.get(host);

        if(info.packetGraphBuffer.size() > 0)
        {
          String label = host;

          if(info.sentPackets > 0 && info.out != null && info.out.length() != 0) {
            String sentAddressString;
            String sentPortString;

            if(NetworkLog.resolveHosts) {
              sentAddressString = NetworkLog.resolver.resolveAddress(info.sentAddress);

              if(sentAddressString == null) {
                sentAddressString = info.sentAddress;
              }
            } else {
              sentAddressString = info.sentAddress;
            }

            if(NetworkLog.resolvePorts) {
              sentPortString = NetworkLog.resolver.resolveService(String.valueOf(info.sentPort));
            } else {
              sentPortString = String.valueOf(info.sentPort);
            }

            label = sentAddressString + ":" + sentPortString;
          }
          else if(info.receivedPackets > 0 && info.in != null && info.in.length() != 0) {
            String receivedAddressString;
            String receivedPortString;

            if(NetworkLog.resolveHosts) {
              receivedAddressString = NetworkLog.resolver.resolveAddress(info.receivedAddress);

              if(receivedAddressString == null) {
                receivedAddressString = info.receivedAddress;
              }
            } else {
              receivedAddressString = info.receivedAddress;
            }

            if(NetworkLog.resolvePorts) {
              receivedPortString = NetworkLog.resolver.resolveService(String.valueOf(info.receivedPort));
            } else {
              receivedPortString = String.valueOf(info.receivedPort);
            }

            label = receivedAddressString + ":" + receivedPortString;
          }

          MyLog.d("Building legend for " + label);

          ShapeDrawable shape = new ShapeDrawable(rect);
          shape.getPaint().setColor(Color.parseColor(getResources().getString(Colors.distinctColor[color])));
          shape.setIntrinsicWidth((int)(18 * (density + 0.5)));
          shape.setIntrinsicHeight((int)(18 * (density + 0.5)));

          int hashCode = label.hashCode();
          LegendItem legend = new LegendItem();

          legend.mIcon = shape;
          legend.mHashCode = hashCode;
          legend.mName = label;
          legend.mEnabled = true;

          legendData.add(legend);

          color++;

          if(color >= Colors.distinctColor.length)
          {
            color = 0;
          }
        }
      }
    }
  }

  public void buildSeries(double timeFrameSize, double viewSize) {
    graphView.graphSeries.clear();

    int index = NetworkLog.appFragment.getItemByAppUid(Integer.parseInt(app_uid));

    if(index < 0)
    {
      // alert dialog
      finish();
    }

    AppFragment.GroupItem item = NetworkLog.appFragment.groupDataBuffer.get(index);

    MyLog.d("Starting graph for " + item);

    synchronized(item.childrenData) {
      ArrayList<String> list = new ArrayList<String>(item.childrenData.keySet());
      Collections.sort(list);
      Iterator<String> itr = list.iterator();
      int color = 0;

      while(itr.hasNext())
      {
        String host = itr.next();
        MyLog.d("Graphing " + host);
        AppFragment.ChildItem info = item.childrenData.get(host);

        if(info.packetGraphBuffer.size() > 0)
        {
          MyLog.d("number of packets: " + info.packetGraphBuffer.size());
          ArrayList<PacketGraphItem> graphData = new ArrayList<PacketGraphItem>();

          double nextTimeFrame = 0;
          double frameLen = 1; // len for this time frame

          for(PacketGraphItem data : info.packetGraphBuffer)
          {
            //MyLog.d("processing: " + data + "; nextTimeFrame: " + nextTimeFrame + "; frameLen: " + frameLen);

            if(nextTimeFrame == 0) {
              // first  plot
              graphData.add(new PacketGraphItem(data.timestamp, data.len));

              // set up first time frame
              nextTimeFrame = data.timestamp + timeFrameSize;
              frameLen = data.len;

              // get next data
              continue;
            }

            if(data.timestamp <= nextTimeFrame) {
              // data within current time frame, add to frame len
              frameLen += data.len;
              // MyLog.d("Adding " + data.len + "; frameLen: " + frameLen);

              // get next data
              continue;
            } else {
              // data outside current time frame
              // signifies end of frame
              // plot frame len
              // MyLog.d("first plot: (" + nextTimeFrame + ", " + frameLen + ")");
              graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));

              // set up next time frame
              nextTimeFrame += timeFrameSize;
              frameLen = 1;

              // test for gap
              if(data.timestamp > nextTimeFrame) {
                // data is past this time frame, plot zero here
                // MyLog.d("post zero plot: (" + nextTimeFrame + ", " + frameLen + ")");
                graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));

                if((data.timestamp - timeFrameSize) > nextTimeFrame) {
                  // MyLog.d("post pre zero plot: (" + nextTimeFrame + ", " + frameLen + ")");
                  graphData.add(new PacketGraphItem(data.timestamp - timeFrameSize, 1));
                }

                nextTimeFrame = data.timestamp;
                frameLen = data.len;

                // MyLog.d("- plotting: (" + nextTimeFrame + ", " + frameLen + ")");
                graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));

                nextTimeFrame += timeFrameSize;
                frameLen = 1;
                continue;
              } else {
                // data is within this frame, add len
                frameLen = data.len;
              }
            }
          }

          // MyLog.d("post plotting: (" + nextTimeFrame + ", " + frameLen + ")");
          graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));

          // MyLog.d("post zero plotting: (" + (nextTimeFrame + timeFrameSize) +  ", " + 1 + ")");
          graphData.add(new PacketGraphItem(nextTimeFrame + timeFrameSize, 1));

          // MyLog.d("Adding series " + info);

          GraphViewData[] seriesData = new GraphViewData[graphData.size()];

          int i = 0;

          for(PacketGraphItem graphItem : graphData)
          {
            seriesData[i] = new GraphViewData(graphItem.timestamp, graphItem.len);
            i++;
          }

          String label = host;

          if(info.sentPackets > 0 && info.out != null && info.out.length() != 0) {
            String sentAddressString;
            String sentPortString;

            if(NetworkLog.resolveHosts) {
              sentAddressString = NetworkLog.resolver.resolveAddress(info.sentAddress);

              if(sentAddressString == null) {
                sentAddressString = info.sentAddress;
              }

            } else {
              sentAddressString = info.sentAddress;
            }

            if(NetworkLog.resolvePorts) {
              sentPortString = NetworkLog.resolver.resolveService(String.valueOf(info.sentPort));
            } else {
              sentPortString = String.valueOf(info.sentPort);
            }

            label = sentAddressString + ":" + sentPortString;
          }
          else if(info.receivedPackets > 0 && info.in != null && info.in.length() != 0) {
            String receivedAddressString;
            String receivedPortString;

            if(NetworkLog.resolveHosts) {
              receivedAddressString = NetworkLog.resolver.resolveAddress(info.receivedAddress);

              if(receivedAddressString == null) {
                receivedAddressString = info.receivedAddress;
              }

            } else {
              receivedAddressString = info.receivedAddress;
            }

            if(NetworkLog.resolvePorts) {
              receivedPortString = NetworkLog.resolver.resolveService(String.valueOf(info.receivedPort));
            } else {
              receivedPortString = String.valueOf(info.receivedPort);
            }

            label = receivedAddressString + ":" + receivedPortString;
          }

          int hashCode = label.hashCode();

          graphView.addSeries(new GraphViewSeries(hashCode, label, Color.parseColor(getResources().getString(Colors.distinctColor[color])), seriesData));

          boolean enabled = true;
          for(LegendItem legend : legendData) {
            if(legend.mHashCode == hashCode) {
              enabled = legend.mEnabled;
              break;
            }
          }
          graphView.setSeriesEnabled(hashCode, enabled);

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

    double viewStart = maxX - viewSize;

    if(instanceData != null) {
      viewStart = instanceData.viewportStart;
      viewSize = instanceData.viewsize;
    }

    if(viewStart < minX)
    {
      viewStart = minX;
    }

    if(viewStart + viewSize > maxX)
    {
      viewSize = maxX - viewStart;
    }

    graphView.setViewPort(viewStart, viewSize);
    graphView.invalidateLabels();
    graphView.invalidate();
  }
}
