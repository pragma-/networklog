package com.jjoe64.graphview;

import java.lang.Runnable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.util.AttributeSet;
import android.util.Log;
import android.graphics.Rect;

import com.jjoe64.graphview.compatible.ScaleGestureDetector;

import com.googlecode.networklog.R;

/**
 * GraphView is a Android View for creating zoomable and scrollable graphs.
 * This is the abstract base class for all graphs. Extend this class and implement {@link #drawSeries(Canvas, float, float, float, double, double, double, double, float)} to display a custom graph.
 * Use {@link LineGraphView} for creating a line chart.
 *
 * @author jjoe64 - jonas gehring - http://www.jjoe64.com
 * @author pragma78 - pragmatic software - pragma78@gmail.com
 *
 * Copyright (C) 2011 Jonas Gehring
 * Licensed under the GNU Lesser General Public License (LGPL)
 * http://www.gnu.org/licenses/lgpl.html
 */
abstract public class GraphView extends LinearLayout {
  static final private class GraphViewConfig {
    static final float BORDER = 10;  // FIXME: replace this with text height
    static final float VERTICAL_LABEL_WIDTH = 115;
    static final float HORIZONTAL_LABEL_HEIGHT = 16;
    static final float LABEL_PADDING = 4;
  }

  private class GraphViewContentView extends View {
    private float lastTouchEventX;
    private int lastTouchEventId;
    private float graphwidth;

    /**
     * @param context
     */
    public GraphViewContentView(Context context) {
      super(context);
    }

    /**
     * @param canvas
     */
    @Override
      protected void onDraw(Canvas canvas) {
        // normal
        paint.setStrokeWidth(0);

        float top_border = GraphViewConfig.BORDER + GraphViewConfig.LABEL_PADDING;
        float bottom_border = GraphViewConfig.BORDER;

        if(enableMultiLineXLabel) {
          bottom_border *= 2.0f;  // add space for one more line
        }
        
        float horstart = 0;
        float height = getHeight();
        float width = getWidth() - 1;
        double maxY = getMaxY();
        double minY = getMinY();
        double diffY = maxY - minY;
        double maxX = getMaxX(false);
        double minX = getMinX(false);
        double diffX = maxX - minX;
        float graphheight = height - top_border - bottom_border;
        graphwidth = width;

        if (horlabels == null) {
          horlabels = generateHorlabels(graphwidth);
        }

        if (verlabels == null) {
          verlabels = generateVerlabels(graphheight - top_border);
        }

        // vertical lines
        paint.setTextAlign(Align.LEFT);
        paint.setStrokeWidth(0);
        paint.setColor(Color.DKGRAY);
        int vers = verlabels.length - 1;
        for (int i = 0; i < verlabels.length; i++) {
          float y = (((graphheight - top_border) / vers) * i) + top_border;
          canvas.drawLine(horstart, y, width, y, paint);
        }

        // horizontal labels + lines
        int hors = horlabels.length - 1;
        for (int i = 0; i < horlabels.length; i++) {
          float x = ((graphwidth / hors) * i) + horstart;
          paint.setColor(Color.DKGRAY);
          canvas.drawLine(x, top_border, x, graphheight, paint);

          if (i==horlabels.length-1)
            paint.setTextAlign(Align.RIGHT);
          else if (i==0)
            paint.setTextAlign(Align.LEFT);
          else
            paint.setTextAlign(Align.CENTER);
          
          paint.setColor(Color.WHITE);

          if(enableMultiLineXLabel) {
            float offsetY = height - bottom_border + GraphViewConfig.LABEL_PADDING;

            int delim = horlabels[i].indexOf('\n', 0);
            String str = horlabels[i].substring(0, delim);
            canvas.drawText(str, x, offsetY, paint);

            paint.getTextBounds(str, 0, str.length(), rect);
            offsetY += (rect.height() + GraphViewConfig.LABEL_PADDING);

            str = horlabels[i].substring(delim + 1, horlabels[i].length());
            canvas.drawText(str, x, offsetY, paint);
          } else {
            canvas.drawText(horlabels[i], x, height - 4, paint);
          }
        }

        paint.setTextAlign(Align.CENTER);
        canvas.drawText(title, (graphwidth / 2) + horstart, top_border, paint);

        if (maxY != minY) {
          paint.setStrokeCap(Paint.Cap.ROUND);
          paint.setStrokeWidth(2);

          for (int i=0; i<graphSeries.size(); i++) {
            if(graphSeries.get(i).enabled) {
              paint.setColor(graphSeries.get(i).color);
              graphSeries.get(i).size = drawSeries(canvas, _values(i), graphwidth, graphheight, top_border, bottom_border, minX, minY, diffX, diffY, horstart);
            } else {
              graphSeries.get(i).size = 0;
            }
          }

          minX = getMinX(true);
          maxX = getMaxX(true);
          double percentage = ((viewportStart - minX) / ((maxX - viewportSize) - minX)) * 100;
          seekbar.setProgress((int)percentage);

          if (legendSorter != null) legendSorter.run();
          if (showLegend) drawLegend(canvas, height, width);
        }
      }

    public void onMoveGesture(float f) {
      // view port update
      if (viewportSize != 0) {
        viewportStart -= f*viewportSize/graphwidth;

        // minimal and maximal view limit
        double minX = getMinX(true);
        double maxX = getMaxX(true);
        if (viewportStart < minX) {
          viewportStart = minX;
        } else if (viewportStart+viewportSize > maxX) {
          viewportStart = maxX - viewportSize;
        }

        // labels have to be regenerated
        horlabels = null;
        verlabels = null;
        viewVerLabels.invalidate();
        invalidate();
      }
    }

    /**
     * @param event
     */
    @Override
      public boolean onTouchEvent(MotionEvent event) {
        if (!isScrollable()) {
          return super.onTouchEvent(event);
        }

        boolean handled = false;
        // first scale
        if (scalable && scaleDetector != null) {
          scaleDetector.onTouchEvent(event);
          if(scaleDetector.isInProgress() == true) {
            lastTouchEventX = 0;
            handled = true;
          }
        }

        if(event.getPointerCount() > 1) {
          lastTouchEventX = 0;
          lastTouchEventId = 0;
        }

        if (!handled && event.getPointerCount() == 1) {
          // if not scaled, scroll
          if ((event.getAction() & MotionEvent.ACTION_DOWN) == MotionEvent.ACTION_DOWN) {
            handled = true;
          }
          if ((event.getAction() & MotionEvent.ACTION_UP) == MotionEvent.ACTION_UP) {
            lastTouchEventX = 0;
            handled = true;
          }
          if ((event.getAction() & MotionEvent.ACTION_MOVE) == MotionEvent.ACTION_MOVE) {
            if (lastTouchEventX != 0 && event.getPointerId(0) == lastTouchEventId) {
              onMoveGesture(event.getX() - lastTouchEventX);
            }
            lastTouchEventX = event.getX();
            lastTouchEventId = event.getPointerId(0);
            handled = true;
          }
          if(handled) {
            invalidate();
          }
        }
        return handled;
      }
  }

  /**
   * one data set for a graph series
   */
  static public class GraphViewData {
    public final double valueX;
    public final double valueY;
    public GraphViewData(double valueX, double valueY) {
      super();
      this.valueX = valueX;
      this.valueY = valueY;
    }
  }

  /**
   * a graph series
   */
  static public class GraphViewSeries {
    final String description;
    final int color;
    final GraphViewData[] values;
    public int id = -1;
    public boolean enabled = true;
    public double size;

    public GraphViewSeries(GraphViewData[] values) {
      description = null;
      color = 0xff0077cc; // blue version
      this.values = values;
    }

    public GraphViewSeries(int id, String description, Integer color, GraphViewData[] values) {
      this(description, color, values);
      this.id = id;
      this.enabled = true;
    }
    
    public GraphViewSeries(String description, Integer color, GraphViewData[] values) {
      super();
      this.description = description;
      if (color == null) {
        color = 0xff0077cc; // blue version
      }
      this.color = color;
      this.values = values;
    }
  }

  public enum LegendAlign {
    TOP, MIDDLE, BOTTOM
  }

  private class VerLabelsView extends View {
    /**
     * @param context
     */
    public VerLabelsView(Context context) {
      super(context);
    }

    /**
     * @param canvas
     */
    @Override
      protected void onDraw(Canvas canvas) {
        // normal
        paint.setStrokeWidth(0);

        float top_border = GraphViewConfig.BORDER + GraphViewConfig.LABEL_PADDING;
        float bottom_border = GraphViewConfig.BORDER;
        if(enableMultiLineXLabel) {
          bottom_border *= 2.0f;
        }
        float height = getHeight();
        float graphheight = height - top_border - bottom_border;

        if (verlabels == null) {
          verlabels = generateVerlabels(graphheight - top_border);
        }

        // vertical labels
        paint.setTextAlign(Align.LEFT);
        paint.setColor(Color.WHITE);
        int vers = verlabels.length - 1;
        for (int i = 0; i < verlabels.length; i++) {
          float y = (((graphheight - top_border) / vers) * i) + top_border;
          canvas.drawText(verlabels[i], 0, y, paint);
        }
      }
  }

  public void invalidateLabels() {
    horlabels = null;
    verlabels = null;
    viewVerLabels.invalidate();
  }

  protected final Paint paint;
  private String[] horlabels;
  private String[] verlabels;
  private String title = "";
  private boolean scrollable;
  private double viewportStart;
  private double viewportSize;
  private final View viewVerLabels;
  private ScaleGestureDetector scaleDetector;
  private boolean scalable;
  private NumberFormat numberformatter;
  public final List<GraphViewSeries> graphSeries;
  private boolean showLegend = false;
  private float legendWidth = 120;
  private LegendAlign legendAlign = LegendAlign.MIDDLE;
  private boolean manualYAxis;
  private double manualMaxYValue;
  private double manualMinYValue;
  private Context context;
  private SeekBar seekbar;
  private Runnable legendSorter;
  private Rect rect;
  private boolean enableMultiLineXLabel;

  public GraphView(Context context, AttributeSet attrs) {
    super(context, attrs);

    rect = new Rect();
    paint = new Paint();

    paint.setAntiAlias(true);

    graphSeries = new ArrayList<GraphViewSeries>();

    setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    setOrientation(LinearLayout.VERTICAL);

    LinearLayout layout = new LinearLayout(context);
    addView(layout, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));

    viewVerLabels = new VerLabelsView(context);
    layout.addView(viewVerLabels, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 7));
    
    layout.addView(new GraphViewContentView(context), new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));

    layout = new LinearLayout(context);
    addView(layout, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0));

    View view = new View(context);
    layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT, 0, 7));

    seekbar = new SeekBar(context);
    seekbar.setProgress(100);
    seekbar.setMax(100);
    seekbar.setThumb(context.getResources().getDrawable(R.drawable.thumb_drawable));
    seekbar.setProgressDrawable(context.getResources().getDrawable(R.drawable.progress_drawable));
    float density = getResources().getDisplayMetrics().density;
    seekbar.setPadding((int)(6 * density + 0.5f), 0, (int)(6 * density + 0.5f), 0);

    seekbar.setOnSeekBarChangeListener(new MyOnSeekBarChangeListener());

    layout.addView(seekbar, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
  }

  /**
   *
   * @param context
   * @param title [optional]
   */
  public GraphView(Context context, String title) {
    this(context, (AttributeSet)null);

    if (title != null)
      this.title = title;
  }

  private GraphViewData[] _values(int idxSeries) {
    GraphViewData[] values = graphSeries.get(idxSeries).values;
    if (viewportStart == 0 && viewportSize == 0) {
      // all data
      return values;
    } else {
      // viewport
      List<GraphViewData> listData = new ArrayList<GraphViewData>();
      for (int i=0; i<values.length; i++) {
        if (values[i].valueX >= viewportStart) {
          if (values[i].valueX > viewportStart+viewportSize) {
            listData.add(values[i]); // one more for nice scrolling
            break;
          } else {
            listData.add(values[i]);
          }
        } else {
          if (listData.isEmpty()) {
            listData.add(values[i]);
          }
          listData.set(0, values[i]); // one before, for nice scrolling
        }
      }
      return listData.toArray(new GraphViewData[listData.size()]);
    }
  }

  public boolean setSeriesEnabled(int id, boolean value) {
    for(GraphViewSeries series : graphSeries) {
      if(series.id == id) {
        series.enabled = value;
        return true;
      }
    }
    return false;
  }

  public void addSeries(GraphViewSeries series) {
    graphSeries.add(series);
  }

  public void removeSeries(int index)
  {
    if (index < 0 || index >= graphSeries.size())
    {
      throw new IndexOutOfBoundsException("No series at index " + index);
    }

    graphSeries.remove(index);
  }

  public void removeSeries(GraphViewSeries series)
  {
    graphSeries.remove(series);
  }

  protected void drawLegend(Canvas canvas, float height, float width) {
    int shapeSize = 15;

    // rect
    paint.setARGB(90, 100, 100, 100);
    float legendHeight = (shapeSize+5)*graphSeries.size() +5;
    float lLeft = width-legendWidth - 10;
    float lTop;
    switch (legendAlign) {
      case TOP:
        lTop = 10;
        break;
      case MIDDLE:
        lTop = height/2 - legendHeight/2;
        break;
      default:
        lTop = height - GraphViewConfig.BORDER - legendHeight -10;
    }
    float lRight = lLeft+legendWidth;
    float lBottom = lTop+legendHeight;
    canvas.drawRoundRect(new RectF(lLeft, lTop, lRight, lBottom), 8, 8, paint);

    for (int i=0; i<graphSeries.size(); i++) {
      paint.setColor(graphSeries.get(i).color);
      canvas.drawRect(new RectF(lLeft+5, lTop+5+(i*(shapeSize+5)), lLeft+5+shapeSize, lTop+((i+1)*(shapeSize+5))), paint);
      if (graphSeries.get(i).description != null) {
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Align.LEFT);
        canvas.drawText(graphSeries.get(i).description, lLeft+5+shapeSize+5, lTop+shapeSize+(i*(shapeSize+5)), paint);
      }
    }
  }

  abstract public double drawSeries(Canvas canvas, GraphViewData[] values, float graphwidth, float graphheight, float top_border, float bottom_border, double minX, double minY, double diffX, double diffY, float horstart);

  /**
   * formats the label
   * can be overwritten
   * @param value x and y values
   * @param isValueX if false, value y wants to be formatted
   * @return value to display
   */
  protected String formatLabel(double value, boolean isValueX) {
    if (numberformatter == null) {
      numberformatter = NumberFormat.getNumberInstance();
      double highestvalue = getMaxY();
      double lowestvalue = getMinY();
      if (highestvalue - lowestvalue < 0.1) {
        numberformatter.setMaximumFractionDigits(6);
      } else if (highestvalue - lowestvalue < 1) {
        numberformatter.setMaximumFractionDigits(4);
      } else if (highestvalue - lowestvalue < 20) {
        numberformatter.setMaximumFractionDigits(3);
      } else if (highestvalue - lowestvalue < 100) {
        numberformatter.setMaximumFractionDigits(1);
      } else {
        numberformatter.setMaximumFractionDigits(0);
      }
    }
    return numberformatter.format(value);
  }

  private String[] generateHorlabels(float graphwidth) {
    int numLabels = (int) (graphwidth/GraphViewConfig.VERTICAL_LABEL_WIDTH);
    if(numLabels <= 0)
      return new String[0];
    String[] labels = new String[numLabels+1];
    double min = getMinX(false);
    double max = getMaxX(false);
    for (int i=0; i<=numLabels; i++) {
      labels[i] = formatLabel(min + ((max-min)*i/numLabels), true);
    }
    return labels;
  }

  synchronized private String[] generateVerlabels(float graphheight) {
    int numLabels = (int) (graphheight/GraphViewConfig.HORIZONTAL_LABEL_HEIGHT);
    if(numLabels <= 0)
      return new String[0];
    String[] labels = new String[numLabels+1];
    double min = getMinY();
    double max = getMaxY();

    /*
    float maxWidth = 0;
    float width = 0;
    */

    for (int i=0; i<=numLabels; i++) {
      labels[numLabels-i] = formatLabel(min + ((max-min)*i/numLabels), false);
    
      /*
      width = paint.measureText(labels[numLabels-i]);
      Log.d("[IptablesLog]", "width for [" + labels[numLabels-i] + "] = " + width); 
      if(width > maxWidth)
        maxWidth = width;
        */
    }
    
    //Log.d("[IptablesLog]", "max width: " + (int)maxWidth);
    //viewVerLabels.setLayoutParams(new LayoutParams((int)maxWidth, LayoutParams.FILL_PARENT, 10));
    return labels;
  }

  public LegendAlign getLegendAlign() {
    return legendAlign;
  }

  public float getLegendWidth() {
    return legendWidth;
  }

  public double getMaxX(boolean ignoreViewport) {
    // if viewport is set, use this
    if (!ignoreViewport && viewportSize != 0) {
      return viewportStart+viewportSize;
    } else {
      // otherwise use the max x value
      // values must be sorted by x, so the last value has the largest X value
      double highest = 0;
      if (graphSeries.size() > 0)
      {
        GraphViewData[] values = graphSeries.get(0).values;
        if(graphSeries.get(0).enabled)
          highest = values[values.length-1].valueX;
        for (int i=1; i<graphSeries.size(); i++) {
          if(!graphSeries.get(i).enabled)
            continue;
          values = graphSeries.get(i).values;
          highest = Math.max(highest, values[values.length-1].valueX);
        }
      }
      return highest;
    }
  }

  private double getMaxY() {
    double largest;
    if (manualYAxis) {
      largest = manualMaxYValue;
    } else {
      largest = Integer.MIN_VALUE;
      for (int i=0; i<graphSeries.size(); i++) {
        if(!graphSeries.get(i).enabled)
          continue;
        GraphViewData[] values = _values(i);
        for (int ii=0; ii<values.length; ii++)
          if (values[ii].valueY > largest)
            largest = values[ii].valueY;
      }
    }
    return largest;
  }

  public double getMinX(boolean ignoreViewport) {
    // if viewport is set, use this
    if (!ignoreViewport && viewportSize != 0) {
      return viewportStart;
    } else {
      // otherwise use the min x value
      // values must be sorted by x, so the first value has the smallest X value
      double lowest = 0;
      if (graphSeries.size() > 0)
      {
        GraphViewData[] values = graphSeries.get(0).values;
        lowest = values[0].valueX;
        for (int i=1; i<graphSeries.size(); i++) {
          if(!graphSeries.get(i).enabled)
            continue;
          values = graphSeries.get(i).values;
          lowest = Math.min(lowest, values[0].valueX);
        }
      }
      return lowest;
    }
  }

  private double getMinY() {
    double smallest;
    if (manualYAxis) {
      smallest = manualMinYValue;
    } else {
      smallest = Integer.MAX_VALUE;
      for (int i=0; i<graphSeries.size(); i++) {
        if(!graphSeries.get(i).enabled)
          continue;
        GraphViewData[] values = _values(i);
        for (int ii=0; ii<values.length; ii++)
          if (values[ii].valueY < smallest)
            smallest = values[ii].valueY;
      }
    }
    return smallest;
  }

  public boolean isScrollable() {
    return scrollable;
  }

  public boolean isShowLegend() {
    return showLegend;
  }

  /**
   * set's static horizontal labels (from left to right)
   * @param horlabels if null, labels were generated automatically
   */
  public void setHorizontalLabels(String[] horlabels) {
    this.horlabels = horlabels;
  }

  public void setLegendAlign(LegendAlign legendAlign) {
    this.legendAlign = legendAlign;
  }

  public void setLegendWidth(float legendWidth) {
    this.legendWidth = legendWidth;
  }

  /**
   * you have to set the bounds {@link #setManualYAxisBounds(double, double)}. That automatically enables manualYAxis-flag.
   * if you want to disable the menual y axis, call this method with false.
   * @param manualYAxis
   */
  public void setManualYAxis(boolean manualYAxis) {
    this.manualYAxis = manualYAxis;
  }

  /**
   * set manual Y axis limit
   * @param max
   * @param min
   */
  public void setManualYAxisBounds(double max, double min) {
    manualMaxYValue = max;
    manualMinYValue = min;
    manualYAxis = true;
  }

  /**
   * this forces scrollable = true
   * @param scalable
   */
  synchronized public void setScalable(boolean scalable) {
    this.scalable = scalable;
    if (scalable == true && scaleDetector == null) {
      scrollable = true; // automatically forces this
      scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
          double center = viewportStart + viewportSize / 2;
          viewportSize /= detector.getScaleFactor();
          viewportStart = center - viewportSize / 2;

          // viewportStart must not be < minX
          double minX = getMinX(true);
          if (viewportStart < minX) {
            viewportStart = minX;
          }

          // viewportStart + viewportSize must not be > maxX
          double maxX = getMaxX(true);
          double overlap = viewportStart + viewportSize - maxX;
          if (overlap > 0) {
            // scroll left
            if (viewportStart-overlap > minX) {
              viewportStart -= overlap;
            } else {
              // maximal scale
              viewportStart = minX;
              viewportSize = maxX - viewportStart;
            }
          }

          verlabels = null;
          horlabels = null;
          numberformatter = null;
          invalidate();
          viewVerLabels.invalidate();
          return true;
        }
      });
    }
  }

  /**
   * the user can scroll (horizontal) the graph. This is only useful if you use a viewport {@link #setViewPort(double, double)} which doesn't displays all data.
   * @param scrollable
   */
  public void setScrollable(boolean scrollable) {
    this.scrollable = scrollable;
  }

  public void setShowLegend(boolean showLegend) {
    this.showLegend = showLegend;
  }

  /**
   * set's static vertical labels (from top to bottom)
   * @param verlabels if null, labels were generated automatically
   */
  public void setVerticalLabels(String[] verlabels) {
    this.verlabels = verlabels;
  }

  /**
   * set's the viewport for the graph.
   * @param start x-value
   * @param size
   */
  public void setViewPort(double start, double size) {
    viewportStart = start;
    viewportSize = size;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setLegendSorter(Runnable sorter) {
    legendSorter = sorter;
  }

  public class MyOnSeekBarChangeListener implements OnSeekBarChangeListener {
    @Override
      public void onProgressChanged(SeekBar seekBar, int percentage, boolean fromUser) {
        if(!fromUser) {
          return;
        }

        if (viewportSize != 0) {
          // minimal and maximal view limit
          double minX = getMinX(true);
          double maxX = getMaxX(true);
          // labels have to be regenerated
          horlabels = null;
          verlabels = null;
          viewVerLabels.invalidate();

          viewportStart = ((percentage / 100.0) * ((maxX - viewportSize) - minX)) + minX;
        }
        invalidate();
      }

    @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
        // do nothing
      }

    @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        // do nothing
      }
  }

  public void setEnableMultiLineXLabel(boolean value) {
    enableMultiLineXLabel = value;
  }
}
