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
  static final int MSG_TOGGLE_FOREGROUND = 5;
  final Messenger messenger = new Messenger(new IncomingHandler(this));
  boolean has_root = false;
  public static NetworkLogService instance = null;

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

            if(start_foreground) {
              nManager.notify(NOTIFICATION_ID, notification);
            }
            break;

          case MSG_TOGGLE_FOREGROUND:
            MyLog.d("[service] toggling service foreground state: " + ((Boolean)msg.obj));
            start_foreground = (Boolean)msg.obj;

            if(start_foreground) {
              startForeground(notification);
            } else {
              stopForeground();
            }
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
      if(!has_root) {
        return null;
      } else {
        return messenger.getBinder();
      }
    }

  HashMap<String, Integer> logEntriesMap;
  ShellCommand command;
  NetworkLogger logger;
  String logfile = null;
  PrintWriter logWriter = null;
  NotificationManager nManager;
  Notification notification;
  LogEntry entry;
  Boolean start_foreground = true;

  //StringBuilder buffer;

  public void renameLogFile(String newLogFile) {
  }

  public void clearLogFile() {
  }

  public void startForeground(Notification n) {
    startForeground(NOTIFICATION_ID, n);
  }

  public void stopForeground() {
    stopForeground(true);
  }

  public Notification createNotification() {
    Notification n = new Notification(R.drawable.icon, "Network logging started", System.currentTimeMillis());
    Intent i = new Intent(this, NetworkLog.class);
    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
    n.setLatestEventInfo(this, "Network Log", "Logging active", pi);
    return n;
  }

  public boolean hasRoot() {
    return Iptables.checkRoot(this);
  }

  @Override
    public void onCreate() {
      MyLog.d("[service] onCreate");

      if(!hasRoot()) {
        Iptables.showError(this, "Network Log Error", "Network Log requires root/superuser access");
        has_root = false;
        stopSelf();
        return;
      } else {
        has_root = true;
      }

      Iptables.installBinaries(this);

      if(instance != null) {
        MyLog.d("[service] Last instance destroyed unexpectedly");
      }

      instance = this;

      nManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
      notification = createNotification();

      // reuse entry object
      entry = new LogEntry();

      if(NetworkLog.settings == null) {
        NetworkLog.settings = new Settings(this);
      }

      start_foreground = NetworkLog.settings.getStartForeground();

      if(start_foreground) {
        startForeground(notification);
      }
    }

  @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
      MyLog.d("[service] onStartCommand");

      if(!has_root) {
        return Service.START_NOT_STICKY;
      }

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
          String logfile_intent = null;

          if(extras != null) {
            logfile_intent = extras.getString("logfile");
            MyLog.d("[service] set logfile: " + logfile_intent);
          }

          if(logfile_intent == null) {
            logfile_intent = NetworkLog.settings.getLogFile();
          }

          MyLog.d("[service] NetworkLog service starting [" + logfile_intent + "]");;

          final String l = logfile_intent;

          if(logfile != null) {
            // service already started and has logfile open
          } else {
            logfile = logfile_intent;

            try {
              logWriter = new PrintWriter(new BufferedWriter(new FileWriter(logfile, true)), true);
            } catch(final Exception e) {
              Log.e("NetworkLog", "Exception opening logfile [" + logfile +"]", e);
              handler.post(new Runnable() {
                public void run() {
                  Iptables.showError(context, "Network Log Error", "Failed to start Network Log service: " + e.getMessage());
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

      stopForeground();

      instance = null;

      if(has_root) {
        Iptables.removeRules(this);
        stopLogging();
        Toast.makeText(this, "Network Log service done", Toast.LENGTH_SHORT).show();
      }
    }

  public static NetworkLogService getInstance() { 
    return instance;
  }

  public void initEntriesMap() {
    NetStat netstat = new NetStat();
    ArrayList<NetStat.Connection> connections = netstat.getConnections();

    for(NetStat.Connection connection : connections) {
      String mapKey = connection.src + ":" + connection.spt + " -> " + connection.dst + ":" + connection.dpt;
      if(MyLog.enabled) {
        MyLog.d("[netstat src-dst] New entry " + connection.uid + " for [" + mapKey + "]");
      }
      logEntriesMap.put(mapKey, Integer.valueOf(connection.uid));

      mapKey = connection.dst + ":" + connection.dpt + " -> " + connection.src + ":" + connection.spt;
      if(MyLog.enabled) {
        MyLog.d("[netstat dst-src] New entry " + connection.uid + " for [" + mapKey + "]");
      }
      logEntriesMap.put(mapKey, Integer.valueOf(connection.uid));
    }
  }

  public void parseResult(String result) {
    if(MyLog.enabled) {
      MyLog.d("--------------- parsing network entry --------------");
    }
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
      if(MyLog.enabled) {
        MyLog.d("---- got [NetworkLogEntry] at " + pos + " ----");
      }
      // buffer_pos = pos;

      pos += "[NetworkLogEntry]".length(); // skip past "[NetworkLogEntry]"

      int newline = result.indexOf("\n", pos);
      int nextEntry = result.indexOf("[NetworkLogEntry]", pos);

      if(nextEntry != -1 && nextEntry < newline) {
        if(MyLog.enabled) {
          MyLog.d("Skipping corrupted entry");
        }
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

      in = StringPool.get(result.substring(pos + 3, space));

      pos = result.indexOf("OUT=", pos);

      if(pos == -1 || pos > newline) {
        /* MyLog.d("buffering [" + line + "] for OUT");  */ break;
      };

      space = result.indexOf(" ", pos);

      if(space == -1 || space > newline) {
        /* MyLog.d("buffering [" + line + "] for OUT space");  */ break;
      };

      out = StringPool.get(result.substring(pos + 4, space));

      pos = result.indexOf("SRC=", pos);

      if(pos == -1 || pos > newline) {
        /* MyLog.d("buffering [" + line + "] for SRC");  */ break;
      };

      space = result.indexOf(" ", pos);

      if(space == -1 || space > newline) {
        /* MyLog.d("buffering [" + line + "] for SRC space");  */ break;
      };

      src = StringPool.get(result.substring(pos + 4, space));

      pos = result.indexOf("DST=", pos);

      if(pos == -1 || pos > newline) {
        /* MyLog.d("buffering [" + line + "] for DST");  */ break;
      };

      space = result.indexOf(" ", pos);

      if(space == -1 || space > newline) {
        /* MyLog.d("buffering [" + line + "] for DST space");  */ break;
      };

      dst = StringPool.get(result.substring(pos + 4, space));

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
        if(MyLog.enabled) {
          MyLog.d("Looking for UID newline");
        }

        if(newline == -1) {
          /* MyLog.d("buffering [" + line + "] for UID newline");  */ break;
        };

        uidString = result.substring(pos + 4, newline);
      }

      int uid = -13;
      int spt = -1;
      int dpt = -1;
      int len = -1;

      int end, length;

      try {
        length = uidString.length();
        // uidString could be "-1", so check for '-'; but the other strings will be > 0
        for(end = 0; end < length && (uidString.charAt(end) == '-' || Character.isDigit(uidString.charAt(end))); end++);
        uid = Integer.parseInt(uidString.substring(0, end));

        length = sptString.length();
        for(end = 0; end < length && Character.isDigit(sptString.charAt(end)); end++);
        spt = Integer.parseInt(sptString.substring(0, end));
        
        length = dptString.length();
        for(end = 0; end < length && Character.isDigit(dptString.charAt(end)); end++);
        dpt = Integer.parseInt(dptString.substring(0, end));
        
        length = lenString.length();
        for(end = 0; end < length && Character.isDigit(lenString.charAt(end)); end++);
        len = Integer.parseInt(lenString.substring(0, end));
      } catch(Exception e) {
        Log.e("NetworkLog", "Bad data for: [" + result + "]", e);
      }

      String srcDstMapKey = src + ":" + spt + " -> " + dst + ":" + dpt;
      String dstSrcMapKey = dst + ":" + dpt + " -> " + src + ":" + spt;

      if(MyLog.enabled) {
        MyLog.d("Checking entry for " + uid + " " + srcDstMapKey + " and " + dstSrcMapKey);
      }
      Integer srcDstMapUid = logEntriesMap.get(srcDstMapKey);
      Integer dstSrcMapUid = logEntriesMap.get(dstSrcMapKey);

      if(uid < 0) {
        // Unknown uid, retrieve from entries map
        if(MyLog.enabled) {
          MyLog.d("Unknown uid");
        }

        if(srcDstMapUid == null || dstSrcMapUid == null) {
          // refresh netstat and try again
          if(MyLog.enabled) {
            MyLog.d("Refreshing netstat ...");
          }
          initEntriesMap();
          srcDstMapUid = logEntriesMap.get(srcDstMapKey);
          dstSrcMapUid = logEntriesMap.get(dstSrcMapKey);
        }

        if(srcDstMapUid == null) {
          if(MyLog.enabled) {
            MyLog.d("[src-dst] No entry uid for " + uid + " [" + srcDstMapKey + "]");
          }

          if(uid == -1) {
            if(dstSrcMapUid != null) {
              if(MyLog.enabled) {
                MyLog.d("[dst-src] Reassigning kernel packet -1 to " + dstSrcMapUid);
              }
              uid = dstSrcMapUid;
            } else {
              if(MyLog.enabled) {
                MyLog.d("[src-dst] New kernel entry -1 for [" + srcDstMapKey + "]");
              }
              srcDstMapUid = uid;
              logEntriesMap.put(srcDstMapKey, srcDstMapUid);
            }
          } else {
            if(MyLog.enabled) {
              MyLog.d("[src-dst] New entry " + uid + " for [" + srcDstMapKey + "]");
            }
            srcDstMapUid = uid;
            logEntriesMap.put(srcDstMapKey, srcDstMapUid);
          }
        } else {
          if(MyLog.enabled) {
            MyLog.d("[src-dst] Found entry uid " + srcDstMapUid + " for " + uid + " [" + srcDstMapKey + "]");
          }
          uid = srcDstMapUid;
        }

        if(dstSrcMapUid == null) {
          if(MyLog.enabled) {
            MyLog.d("[dst-src] No entry uid for " + uid + " [" + dstSrcMapKey + "]");
          }

          if(uid == -1) {
            if(srcDstMapUid != null) {
              if(MyLog.enabled) {
                MyLog.d("[src-dst] Reassigning kernel packet -1 to " + srcDstMapUid);
              }
              uid = srcDstMapUid;
            } else {
              if(MyLog.enabled) {
                MyLog.d("[dst-src] New kernel entry -1 for [" + dstSrcMapKey + "]");
              }
              dstSrcMapUid = uid;
              logEntriesMap.put(dstSrcMapKey, dstSrcMapUid);
            }
          } else {
            if(MyLog.enabled) {
              MyLog.d("[dst-src] New entry " + uid + " for [" + dstSrcMapKey + "]");
            }
            dstSrcMapUid = uid;
            logEntriesMap.put(dstSrcMapKey, dstSrcMapUid);
          }
        } else {
          if(MyLog.enabled) {
            MyLog.d("[dst-src] Found entry uid " + dstSrcMapUid + " for " + uid + " [" + dstSrcMapKey + "]");
          }
          uid = dstSrcMapUid;
        }
      } else {
        if(MyLog.enabled) {
          MyLog.d("Known uid");
        }

        if(srcDstMapUid == null || dstSrcMapUid == null) {
          if(MyLog.enabled) {
            MyLog.d("Adding missing uid " + uid + " to netstat map for " + srcDstMapKey + " and " + dstSrcMapKey);
          }
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

    if(MyLog.enabled) {
      MyLog.d("[service] notifyNewEntry: clients: " + clients.size());
    }

    for(int i = clients.size() - 1; i >= 0; i--) {
      try {
        if(MyLog.enabled) {
          MyLog.d("[service] Sending entry to " + clients.get(i));
        }
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
      String busybox = getFilesDir().getAbsolutePath() + File.separator + "busybox_g1";

      try {
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(scriptFile)));
        script.println(busybox + " grep NetworkLogEntry /proc/kmsg");
        script.close();
      } catch(java.io.IOException e) {
        e.printStackTrace();
      }

      MyLog.d("Starting iptables log tracker");

      command = new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "NetworkLogger");
      final String error = command.start(false);

      if(error != null) {
        Iptables.showError(this, "Network Log Error", error);
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

      int networklog_pid = -1;
      String string, pid_string, ppid_string, cmd = "";
      int pid = 0, ppid = 0, token, pos, space;
      boolean error = false;

      String busybox = getFilesDir().getAbsolutePath() + File.separator + "busybox_g1";

      while(true) {
        String line = command.readStdoutBlocking();

        if(line == null) {
          break;
        }

        // MyLog.d("ps - parsing line [" + line + "]");

        token = 0;
        pos = 0;
        error = false;

        // get tokens
        while(true) {
          space = line.indexOf(' ', pos);

          if(space == -1) {
            // last token
            cmd = line.substring(pos, line.length() - 1);
            break;
          }

          string = line.substring(pos, space);

          try {
            switch(token) {
              case 1:
                pid = Integer.parseInt(string);
                break;
              case 2:
                ppid = Integer.parseInt(string);
                break;
              default:
            }
          } catch(NumberFormatException e) {
            error = true;
            break;
          } catch(ArrayIndexOutOfBoundsException e) {
            error = true;
            break;
          } catch(Exception e) {
            error = true;
            Log.d("NetworkLog", "Unexpected exception", e);
            break;
          }

          token++;
          
          pos = space + 1;

          while(line.charAt(pos) == ' ') {
            pos++;
          }
        }

        if(error == true) {
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

          if(cmd.equals(busybox)) {
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
        MyLog.d("NetworkLogger checking stdout");

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

        parseResult(result);
      }

      MyLog.d("NetworkLogger " + this + " exiting [end of loop]");
    }
  }
}
