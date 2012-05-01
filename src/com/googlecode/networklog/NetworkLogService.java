package com.googlecode.networklog;

import android.content.ContextWrapper;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.util.Log;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Handler;
import android.os.Messenger;
import android.os.Message;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.RemoteException;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.lang.Thread;
import java.lang.Runnable;
import java.lang.reflect.Method;

public class NetworkLogService extends Service {
  ArrayList<Messenger> clients = new ArrayList<Messenger>();
  static final int NOTIFICATION_ID = 42;
  static final int MSG_REGISTER_CLIENT = 1;
  static final int MSG_UNREGISTER_CLIENT = 2;
  static final int MSG_UPDATE_NOTIFICATION = 3;
  static final int MSG_BROADCAST_LOG_ENTRY = 4;
  final Messenger messenger = new Messenger(new IncomingHandler(this));

  private class IncomingHandler extends Handler {
    Context context;

    public IncomingHandler(Context context) {
      this.context = context;
    }

    @Override
      public void handleMessage(Message msg) {
        MyLog.d("[service] got message: " + msg);

        switch(msg.what) {
          case MSG_REGISTER_CLIENT:
            MyLog.d("[service] registering client " + msg.replyTo);
            clients.add(msg.replyTo);
            break;

          case MSG_UNREGISTER_CLIENT:
            MyLog.d("[service] unregistering client " + msg.replyTo);
            clients.remove(msg.replyTo);
            break;

          case MSG_UPDATE_NOTIFICATION:
            MyLog.d("[service] updating notification: " + ((String)msg.obj));
            Intent i = new Intent(context, NetworkLog.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);
            notification.setLatestEventInfo(context, "Network Log", "Logging active [" + ((String)msg.obj) + "]", pi);
            nManager.notify(42, notification);
            break;

          case MSG_BROADCAST_LOG_ENTRY:
            MyLog.d("[service] got MSG_BROADCOAST_LOG_ENTRY unexpectedly");
            break;

          default:
            MyLog.d("[service] unhandled message");
            super.handleMessage(msg);
        }
      }
  }

  @Override
    public IBinder onBind(Intent intent) {
      MyLog.d("[service] onBind");
      return messenger.getBinder();
    }

  HashMap<String, Integer> logEntriesMap;
  ShellCommand command;
  NetworkLogger logger;
  String logfile = null;
  long logfile_maxsize;
  PrintWriter logWriter = null;
  NotificationManager nManager;
  Notification notification;
  LogEntry entry;

  //StringBuilder buffer;

  public void renameLogFile(String newLogFile) {
  }

  public void clearLogFile() {
  }

  public void startForeground(Notification n) {
    try {
      Method m = Service.class.getMethod("startForeground", new Class[] {int.class, Notification.class});
      m.invoke(this, NOTIFICATION_ID, n);
      MyLog.d("[service] Started service in foreground");
    } catch(Exception e) {
      MyLog.d("[service] Fallback to setForeground");
      setForeground(true);
      nManager.notify(NOTIFICATION_ID, n);
    }
  }

  public void stopForeground() {
    try {
      Method m = Service.class.getMethod("stopForeground", new Class[] {boolean.class});
      m.invoke(this, true);
      MyLog.d("[service] Stopped foreground service state");
    } catch(Exception e) {
      setForeground(false);
      nManager.cancel(NOTIFICATION_ID);
      MyLog.d("[service] Fallback to setForeground(false)");
    }
  }

  public Notification createNotification() {
    Notification n = new Notification(R.drawable.icon, "Network logging started", System.currentTimeMillis());
    Intent i = new Intent(this, NetworkLog.class);
    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
    n.setLatestEventInfo(this, "Network Log", "Logging active", pi);
    return n;
  }

  @Override
    public void onCreate() {
      MyLog.d("[service] onCreate");

      nManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
      notification = createNotification();

      // reuse entry object
      entry = new LogEntry();

      this.startForeground(notification);
    }

  @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
      MyLog.d("[service] onStartCommand");

      Bundle ext = null;

      if(intent == null) {
        MyLog.d("[service] Service null intent");
      } else {
        ext = intent.getExtras();
      }

      final Bundle extras = ext;
      final Context context = this;
      final Handler handler = new Handler(Looper.getMainLooper());

      // run in background thread
      new Thread(new Runnable() {
        public void run() {
          String logfile_intent = "/sdcard/networklog.txt";
          String logfile_maxsize_intent = "12000000";

          if(extras != null) {
            logfile_intent = extras.getString("logfile");
            logfile_maxsize_intent = extras.getString("logfile_maxsize");
          }

          MyLog.d("[service] NetworkLog service starting [" + logfile_intent + "; " + logfile_maxsize_intent + "]");;

          final String l = logfile_intent;
          final String m = logfile_maxsize_intent;

          if(logfile != null) {
            // service already started and has logfile open
            // close logfile and rename and open new one
          } else {
            logfile = logfile_intent;

            try {
              logfile_maxsize = Long.parseLong(logfile_maxsize_intent);
            } catch(Exception e) {
              Log.w("[service] Bad log maxsize: [" + logfile_maxsize_intent + "]: " + e.toString(), e);
              logfile_maxsize = 12000000;
            }

            try {
              logWriter = new PrintWriter(new BufferedWriter(new FileWriter(logfile, true)), true);
            } catch(final Exception e) {
              Log.e("NetworkLog", "Exception opening logfile [" + logfile +"]", e);
              handler.post(new Runnable() {
                public void run() {
                  Toast.makeText(context, "Failed to start Iptableslog service: " + e.getMessage(), Toast.LENGTH_LONG).show();
                  stopSelf();
                }
              });
              return;
            }

            // service starting up fresh
            logEntriesMap = new HashMap<String, Integer>();
            //buffer = new StringBuilder(8192 * 2);

            initEntriesMap();
          }

          if(!startLogging()) {
            MyLog.d("[service] start logging error, aborting");
            handler.post(new Runnable() {
              public void run() {
                stopSelf();
              }
            });
          }
        }
      }).start();

      return Service.START_STICKY;
    }

  @Override
    public void onDestroy() {
      MyLog.d("[service] onDestroy");
      Iptables.removeRules(this);
      stopLogging();
      stopForeground();
      Toast.makeText(this, "Network Log service done", Toast.LENGTH_SHORT).show();
    }

  public void initEntriesMap() {
    NetStat netstat = new NetStat();
    ArrayList<NetStat.Connection> connections = netstat.getConnections();

    for(NetStat.Connection connection : connections) {
      String mapKey = connection.src + ":" + connection.spt + " -> " + connection.dst + ":" + connection.dpt;
      MyLog.d("[netstat src-dst] New entry " + connection.uid + " for [" + mapKey + "]");
      logEntriesMap.put(mapKey, Integer.valueOf(connection.uid));

      mapKey = connection.dst + ":" + connection.dpt + " -> " + connection.src + ":" + connection.spt;
      MyLog.d("[netstat dst-src] New entry " + connection.uid + " for [" + mapKey + "]");
      logEntriesMap.put(mapKey, Integer.valueOf(connection.uid));
    }
  }

  public void parseResult(String result) {
    MyLog.d("--------------- parsing result --------------");
    int pos = 0 /* , buffer_pos = 0 */;
    String in, out, src, dst, lenString, sptString, dptString, uidString;

    /*
       if(MyLog.enabled) {
       MyLog.d("buffer length: " + buffer.length() + "; result length: " + result.length());
       MyLog.d("buffer: [" + buffer + "] <--> [" + result + "]");
       }

       buffer.append(result);
       */

    while((pos = result.indexOf("[NetworkLogEntry]", pos)) > -1) {
      MyLog.d("---- got [NetworkLogEntry] at " + pos + " ----");
      // buffer_pos = pos;

      pos += "[NetworkLogEntry]".length(); // skip past "[NetworkLogEntry]"

      int newline = result.indexOf("\n", pos);
      int nextEntry = result.indexOf("[NetworkLogEntry]", pos);

      if(nextEntry != -1 && nextEntry < newline) {
        MyLog.d("Skipping corrupted entry");
        continue;
      }

      /*
         MyLog.d("pos: " + pos + "; buffer length: " + buffer.length());
         String line = "no newline: " + buffer.substring(pos, buffer.length());

         if(newline != -1) {
         line = buffer.substring(pos, newline - 1);
         }

         MyLog.d("parsing line [" + line + "]");
         */

      pos = result.indexOf("IN=", pos);

      if(pos == -1 || pos > newline) {
        /* MyLog.d("buffering [" + line + "] for IN");  */ break;
      };

      int space = result.indexOf(" ", pos);

      if(space == -1 || space > newline) {
        /* MyLog.d("buffering [" + line + "] for IN space");  */ break;
      };

      in = result.substring(pos + 3, space);

      pos = result.indexOf("OUT=", pos);

      if(pos == -1 || pos > newline) {
        /* MyLog.d("buffering [" + line + "] for OUT");  */ break;
      };

      space = result.indexOf(" ", pos);

      if(space == -1 || space > newline) {
        /* MyLog.d("buffering [" + line + "] for OUT space");  */ break;
      };

      out = result.substring(pos + 4, space);

      pos = result.indexOf("SRC=", pos);

      if(pos == -1 || pos > newline) {
        /* MyLog.d("buffering [" + line + "] for SRC");  */ break;
      };

      space = result.indexOf(" ", pos);

      if(space == -1 || space > newline) {
        /* MyLog.d("buffering [" + line + "] for SRC space");  */ break;
      };

      src = result.substring(pos + 4, space);

      pos = result.indexOf("DST=", pos);

      if(pos == -1 || pos > newline) {
        /* MyLog.d("buffering [" + line + "] for DST");  */ break;
      };

      space = result.indexOf(" ", pos);

      if(space == -1 || space > newline) {
        /* MyLog.d("buffering [" + line + "] for DST space");  */ break;
      };

      dst = result.substring(pos + 4, space);

      pos = result.indexOf("LEN=", pos);

      if(pos == -1 || pos > newline) {
        /* MyLog.d("buffering [" + line + "] for LEN");  */ break;
      };

      space = result.indexOf(" ", pos);

      if(space == -1 || space > newline) {
        /* MyLog.d("buffering [" + line + "] for LEN space");  */ break;
      };

      lenString = result.substring(pos + 4, space);

      pos = result.indexOf("SPT=", pos);

      if(pos == -1 || pos > newline) {
        /* MyLog.d("buffering [" + line + "] for SPT");  */ break;
      };

      space = result.indexOf(" ", pos);

      if(space == -1 || space > newline) {
        /* MyLog.d("buffering [" + line + "] for SPT space");  */ break;
      };

      sptString = result.substring(pos + 4, space);

      pos = result.indexOf("DPT=", pos);

      if(pos == -1 || pos > newline) {
        /* MyLog.d("buffering [" + line + "] for DPT");  */ break;
      };

      space = result.indexOf(" ", pos);

      if(space == -1 || space > newline) {
        /* MyLog.d("buffering [" + line + "] for DPT space");  */ break;
      };

      dptString = result.substring(pos + 4, space);

      int lastpos = pos;

      pos = result.indexOf("UID=", pos);

      // MyLog.d("newline pos: " + newline + "; UID pos: " + pos);
      if(pos == -1 || (pos > newline && newline != -1)) {
        uidString = "-1";
        pos = lastpos;
      } else {
        MyLog.d("Looking for UID newline");

        if(newline == -1) {
          /* MyLog.d("buffering [" + line + "] for UID newline");  */ break;
        };

        uidString = result.substring(pos + 4, newline);
      }

      int uid;
      int spt;
      int dpt;
      int len;

      try {
        uid = Integer.parseInt(uidString.split("[^0-9-]+")[0]);
      } catch(Exception e) {
        Log.e("NetworkLog", "Bad data for uid: [" + uidString + "]", e);
        uid = -13;
      }

      try {
        spt = Integer.parseInt(sptString.split("[^0-9-]+")[0]);
      } catch(Exception e) {
        Log.e("NetworkLog", "Bad data for spt: [" + sptString + "]", e);
        spt = -1;
      }

      try {
        dpt = Integer.parseInt(dptString.split("[^0-9-]+")[0]);
      } catch(Exception e) {
        Log.e("NetworkLog", "Bad data for dpt: [" + dptString + "]", e);
        dpt = -1;
      }

      try {
        len = Integer.parseInt(lenString.split("[^0-9-]+")[0]);
      } catch(Exception e) {
        Log.e("NetworkLog", "Bad data for len: [" + lenString + "]", e);
        len = -1;
      }

      String srcDstMapKey = src + ":" + spt + " -> " + dst + ":" + dpt;
      String dstSrcMapKey = dst + ":" + dpt + " -> " + src + ":" + spt;

      MyLog.d("Checking entry for " + uid + " " + srcDstMapKey + " and " + dstSrcMapKey);
      Integer srcDstMapUid = logEntriesMap.get(srcDstMapKey);
      Integer dstSrcMapUid = logEntriesMap.get(dstSrcMapKey);

      if(uid < 0) {
        // Unknown uid, retrieve from entries map
        MyLog.d("Unknown uid");

        if(srcDstMapUid == null || dstSrcMapUid == null) {
          // refresh netstat and try again
          MyLog.d("Refreshing netstat ...");
          initEntriesMap();
          srcDstMapUid = logEntriesMap.get(srcDstMapKey);
          dstSrcMapUid = logEntriesMap.get(dstSrcMapKey);
        }

        if(srcDstMapUid == null) {
          MyLog.d("[src-dst] No entry uid for " + uid + " [" + srcDstMapKey + "]");

          if(uid == -1) {
            if(dstSrcMapUid != null) {
              MyLog.d("[dst-src] Reassigning kernel packet -1 to " + dstSrcMapUid);
              uid = dstSrcMapUid;
            } else {
              MyLog.d("[src-dst] New kernel entry -1 for [" + srcDstMapKey + "]");
              srcDstMapUid = uid;
              logEntriesMap.put(srcDstMapKey, srcDstMapUid);
            }
          } else {
            MyLog.d("[src-dst] New entry " + uid + " for [" + srcDstMapKey + "]");
            srcDstMapUid = uid;
            logEntriesMap.put(srcDstMapKey, srcDstMapUid);
          }
        } else {
          MyLog.d("[src-dst] Found entry uid " + srcDstMapUid + " for " + uid + " [" + srcDstMapKey + "]");
          uid = srcDstMapUid;
        }

        if(dstSrcMapUid == null) {
          MyLog.d("[dst-src] No entry uid for " + uid + " [" + dstSrcMapKey + "]");

          if(uid == -1) {
            if(srcDstMapUid != null) {
              MyLog.d("[src-dst] Reassigning kernel packet -1 to " + srcDstMapUid);
              uid = srcDstMapUid;
            } else {
              MyLog.d("[dst-src] New kernel entry -1 for [" + dstSrcMapKey + "]");
              dstSrcMapUid = uid;
              logEntriesMap.put(dstSrcMapKey, dstSrcMapUid);
            }
          } else {
            MyLog.d("[dst-src] New entry " + uid + " for [" + dstSrcMapKey + "]");
            dstSrcMapUid = uid;
            logEntriesMap.put(dstSrcMapKey, dstSrcMapUid);
          }
        } else {
          MyLog.d("[dst-src] Found entry uid " + dstSrcMapUid + " for " + uid + " [" + dstSrcMapKey + "]");
          uid = dstSrcMapUid;
        }
      } else {
        MyLog.d("Known uid");

        if(srcDstMapUid == null || dstSrcMapUid == null) {
          MyLog.d("Adding missing uid " + uid + " to netstat map for " + srcDstMapKey + " and " + dstSrcMapKey);
          logEntriesMap.put(srcDstMapKey, uid);
          logEntriesMap.put(dstSrcMapKey, uid);
        }
      }

      entry.uid = uid;
      entry.in = in;
      entry.out = out;
      entry.src = src;
      entry.spt = spt;
      entry.dst = dst;
      entry.dpt = dpt;
      entry.len = len;
      entry.timestamp = System.currentTimeMillis();

      if(MyLog.enabled) {
        MyLog.d("+++ entry: (" + entry.uid + ") in=" + entry.in + " out=" + entry.out + " " + entry.src + ":" + entry.spt + " -> " + entry.dst + ":" + entry.dpt + " [" + entry.len + "]");
      }

      notifyNewEntry(entry);

      // buffer_pos = pos;
    }

    /*
       MyLog.d("truncating buffer_pos: " + buffer_pos + "; length: " + buffer.length());
       if(buffer_pos > buffer.length()) {
       MyLog.d("WTF buffer: [" + buffer + "]");
       } else {
       buffer.delete(0, buffer_pos - 1);
       }
       */
  }

  public void notifyNewEntry(LogEntry entry) {
    // log entry to logfile
    logWriter.println(entry.timestamp + "," + entry.in + "," + entry.out + "," + entry.uid + "," + entry.src + "," + entry.spt + "," + entry.dst + "," + entry.dpt + "," + entry.len);

    MyLog.d("[service] notifyNewEntry: clients: " + clients.size());

    for(int i = clients.size() - 1; i >= 0; i--) {
      try {
        MyLog.d("[service] Sending entry to " + clients.get(i));
        clients.get(i).send(Message.obtain(null, MSG_BROADCAST_LOG_ENTRY, entry));
      } catch(RemoteException e) {
        // client dead
        MyLog.d("[service] Dead client " + clients.get(i));
        clients.remove(i);
      }
    }
  }

  public boolean startLogging() {
    MyLog.d("adding logging rules");
    if(!Iptables.addRules(this)) {
      return false;
    }

    synchronized(NetworkLog.scriptLock) {
      String scriptFile = new ContextWrapper(this).getFilesDir().getAbsolutePath() + File.separator + Iptables.SCRIPT;

      try {
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(scriptFile)));
        script.println("grep NetworkLogEntry /proc/kmsg");
        script.close();
      } catch(java.io.IOException e) {
        e.printStackTrace();
      }

      MyLog.d("Starting iptables log tracker");

      command = new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "NetworkLogger");
      final String error = command.start(false);

      if(error != null) {
        Iptables.showError(this, "Start log error", error);
        return false;
      }
    }

    logger = new NetworkLogger();
    new Thread(logger, "NetworkLogger").start();

    return true;
  }

  public void stopLogging() {
    if(logger != null) {
      logger.stop();
    }

    if(logWriter != null) {
      logWriter.close();
    }

    killLogger();
  }

  public void killLogger() {
    synchronized(NetworkLog.scriptLock) {
      String scriptFile = new ContextWrapper(this).getFilesDir().getAbsolutePath() + File.separator + Iptables.SCRIPT;

      try {
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(scriptFile)));
        script.println("ps");
        script.close();
      } catch(java.io.IOException e) {
        e.printStackTrace();
      }

      ShellCommand command = new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "FindLogger");
      command.start(false);
      String result = "";

      while(true) {
        String line = command.readStdoutBlocking();

        if(line == null) {
          break;
        }

        result += line;
      }

      if(result == null) {
        return;
      }

      int networklog_pid = -1;

      for(String line : result.split("\n")) {
        // MyLog.d("ps - parsing line [" + line + "]");
        String tokens[] = line.split("\\s+");
        String cmd = tokens[tokens.length - 1];
        int pid, ppid;

        try {
          pid = Integer.parseInt(tokens[1]);
          ppid = Integer.parseInt(tokens[2]);
        } catch(NumberFormatException e) {
          // ignored
          continue;
        } catch(ArrayIndexOutOfBoundsException e) {
          // ignored
          continue;
        } catch(Exception e) {
          Log.d("NetworkLog", "Unexpected exception", e);
          continue;
        }

        // MyLog.d("cmd: " + cmd + "; pid: " + pid + "; ppid: " + ppid);

        if(cmd.equals("com.googlecode.networklog")) {
          networklog_pid = pid;
          MyLog.d("NetworkLog pid: " + networklog_pid);
          continue;
        }

        if(ppid == networklog_pid) {
          MyLog.d(cmd + " is our child");
          networklog_pid = pid;

          if(cmd.equals("grep")) {
            MyLog.d("Killing tracker " + pid);

            try {
              PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(scriptFile)));
              script.println("kill " + pid);
              script.close();
            } catch(java.io.IOException e) {
              e.printStackTrace();
            }

            new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "KillLogger").start(true);
            break;
          }
        }
      }
    }
  }

  public class NetworkLogger implements Runnable {
    boolean running = false;

    public void stop() {
      running = false;
    }

    public void run() {
      MyLog.d("NetworkLogger " + this + " starting");
      String result;
      running = true;

      while(running && command.checkForExit() == false) {
        MyLog.d("NetworkLogger " + this + " checking stdout");

        if(command.stdoutAvailable()) {
          result = command.readStdout();
        } else {
          try {
            Thread.sleep(500);
          }
          catch(Exception e) {
            Log.d("NetworkLog", "NetworkLogger exception while sleeping", e);
          }

          continue;
        }

        if(running == false) {
          break;
        }

        if(result == null) {
          MyLog.d("result == null");
          MyLog.d("NetworkLogger " + this + " exiting [returned null]");
          return;
        }

        MyLog.d("result == [" + result + "]");
        parseResult(result);
      }

      MyLog.d("NetworkLogger " + this + " exiting [end of loop]");
    }
  }
}
