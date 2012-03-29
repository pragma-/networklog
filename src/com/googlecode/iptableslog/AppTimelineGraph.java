package com.googlecode.iptableslog;

import android.app.Activity;
import android.os.Bundle;

import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.GraphView.*;

public class AppTimelineGraph extends Activity {
  @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      boolean good_init = false;
      String app_uid = null;
      String src_addr;
      String src_port;
      String dst_addr;
      String dst_port;

      Bundle extras = getIntent().getExtras();

      if(extras != null) {
        app_uid = extras.getString("app_uid");
        src_addr = extras.getString("src_addr");
        src_port = extras.getString("src_port");
        dst_addr = extras.getString("dst_addr");
        dst_port = extras.getString("dst_port");
      }

      if(app_uid == null) {
        // alert dialog
        finish();
      }

      int index = IptablesLog.appView.getItemByAppUid(Integer.parseInt(app_uid));

      if(index < 0) {
        // alert dialog
        finish();
      }

      AppView.ListItem item = IptablesLog.appView.listDataBuffer.get(index);

      LineGraphView graphView = new LineGraphView(this, item.toString());

      graphView.addSeries(
          new GraphViewSeries(
            new GraphViewData[] 
            {
              new GraphViewData(1, 2.0d),
              new GraphViewData(2, 1.5d),
              new GraphViewData(2.5, 3.0d),
              new GraphViewData(3, 2.5d),
              new GraphViewData(4, 1.0d),
              new GraphViewData(5, 3.0d)
            }
            )
          );

      setContentView(graphView);
    }
}
