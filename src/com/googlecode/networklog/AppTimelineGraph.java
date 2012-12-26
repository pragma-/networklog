/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
   */

package com.googlecode.networklog;

import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.shapes.Shape;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.ShapeDrawable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.GraphViewSeries;

public class AppTimelineGraph extends GraphActivity
{
  private int app_uid;
  private String src_addr;
  private int src_port;
  private String dst_addr;
  private int dst_port;

  @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      Bundle extras = getIntent().getExtras();

      if(extras != null) {
        app_uid = Integer.parseInt(extras.getString("app_uid"));
        src_addr = extras.getString("src_addr");
        src_port = extras.getInt("src_port");
        dst_addr = extras.getString("dst_addr");
        dst_port = extras.getInt("dst_port");
      }

      int index = NetworkLog.appFragment.getItemByAppUid(app_uid);

      if(index < 0) {
        // alert dialog
        finish();
      }

      AppFragment.GroupItem item = NetworkLog.appFragment.groupDataBuffer.get(index);
      graphView.setTitle(item.toString() + " Timeline");

      buildSeries(interval, viewsize);
    }

  private class HostPort {
    String host;
    int port;
  }

  public void buildSeries(double timeFrameSize, double viewSize) {
    if(instanceData != null) {
      graphView.graphSeries = instanceData.graphSeries;
    } else {
      HashMap<String, ArrayList<PacketGraphItem>> hostMap = new HashMap<String, ArrayList<PacketGraphItem>>();
      HashMap<String, HostPort> addressMap = new HashMap<String, HostPort>();
      ArrayList<PacketGraphItem> packetList;
      CharArray charBuffer = new CharArray(256);
      String hostKey;
      String address;
      int port;
      HostPort hostPort;

      graphView.graphSeries.clear();

      synchronized(NetworkLog.logFragment.listData) {
        for(LogFragment.ListItem item : NetworkLog.logFragment.listData) {
          if(item.mUid == app_uid) {
            try {
              charBuffer.reset();
              if(item.in != null) {
                charBuffer.append(item.srcAddr).append(':').append(item.srcPort);
                address = item.srcAddr;
                port = item.srcPort;
              } else {
                charBuffer.append(item.dstAddr).append(':').append(item.dstPort);
                address = item.dstAddr;
                port = item.dstPort;
              }
            } catch (ArrayIndexOutOfBoundsException e) {
              Log.e("NetworkLog", "[AppTimelimeGraph] charBuffer too long, skipping entry " + item, e);
              continue;
            }

            hostKey = StringPool.get(charBuffer);

            hostPort = addressMap.get(hostKey);
            if(hostPort == null) {
              hostPort = new HostPort();
              hostPort.host = address;
              hostPort.port = port;
              addressMap.put(hostKey, hostPort);
            }

            packetList = hostMap.get(hostKey);
            if(packetList == null) {
              packetList = new ArrayList<PacketGraphItem>();
              hostMap.put(hostKey, packetList);
            }

            packetList.add(new PacketGraphItem(item.timestamp, item.len));
          }
        }
      }

      int color = 0;
      float density = getResources().getDisplayMetrics().density;
      Shape rect = new RectShape();
      int intrinsicLength = (int)(18 * (density + 0.5));
      for(Map.Entry<String, ArrayList<PacketGraphItem>> entry : hostMap.entrySet()) {
        hostKey = entry.getKey();
        packetList = entry.getValue();

        if(MyLog.enabled) {
          MyLog.d("number of packets for " + hostKey + ": " + packetList.size());
        }
        ArrayList<PacketGraphItem> graphData = new ArrayList<PacketGraphItem>();

        double nextTimeFrame = 0;
        double frameLen = 1; // len for this time frame

        for(PacketGraphItem data : packetList) {
          if(nextTimeFrame == 0) {
            // first  plot
            graphData.add(new PacketGraphItem(data.timestamp - 1, 1));
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
            // get next data
            continue;
          } else {
            // data outside current time frame
            // signifies end of frame
            // plot frame len
            graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));

            // set up next time frame
            nextTimeFrame += timeFrameSize;
            frameLen = 1;

            // test for gap
            if(data.timestamp > nextTimeFrame) {
              // data is past this time frame, plot zero here
              graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));

              if((data.timestamp - timeFrameSize) > nextTimeFrame) {
                graphData.add(new PacketGraphItem(data.timestamp - timeFrameSize, 1));
              }

              nextTimeFrame = data.timestamp;
              frameLen = data.len;

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

        // post plot
        graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));
        // post zero plot
        graphData.add(new PacketGraphItem(nextTimeFrame + timeFrameSize, 1));
        GraphViewData[] seriesData = new GraphViewData[graphData.size()];

        int i = 0;
        for(PacketGraphItem graphItem : graphData) {
          seriesData[i] = new GraphViewData(graphItem.timestamp, graphItem.len);
          i++;
        }

        hostPort = addressMap.get(hostKey);

        String addressString;
        if(NetworkLog.resolveHosts) {
          addressString = NetworkLog.resolver.resolveAddress(hostPort.host);
          if(addressString == null) {
            addressString = hostPort.host;
          }
        } else {
          addressString = hostPort.host;
        }

        String portString;
        if(NetworkLog.resolvePorts) {
          portString = NetworkLog.resolver.resolveService(String.valueOf(hostPort.port));
        } else {
          portString = String.valueOf(hostPort.port);
        }

        String label = addressString + ":" + portString;
        int hashCode = label.hashCode();

        graphView.addSeries(new GraphViewSeries(hashCode, label, Color.parseColor(getResources().getString(Colors.distinctColor[color])), seriesData));

        boolean enabled = true;
        boolean exists = false;
        for(LegendItem legend : legendData) {
          if(legend.mHashCode == hashCode) {
            enabled = legend.mEnabled;
            exists = true;
            break;
          }
        }

        if(exists == false) {
          ShapeDrawable shape = new ShapeDrawable(rect);
          shape.getPaint().setColor(Color.parseColor(getResources().getString(Colors.distinctColor[color])));
          shape.setIntrinsicWidth(intrinsicLength);
          shape.setIntrinsicHeight(intrinsicLength);

          LegendItem legend = new LegendItem();

          legend.mIcon = shape;
          legend.mHashCode = hashCode;
          legend.mName = label;
          legend.mEnabled = true;

          legendData.add(legend);
        }

        graphView.setSeriesEnabled(hashCode, enabled);

        color++;

        if(color >= Colors.distinctColor.length) {
          color = 0;
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

    if(viewStart < minX) {
      viewStart = minX;
    }

    if(viewStart + viewSize > maxX) {
      viewSize = maxX - viewStart;
    }

    graphView.setViewPort(viewStart, viewSize);
    graphView.invalidateLabels();
    graphView.invalidate();
  }
}
