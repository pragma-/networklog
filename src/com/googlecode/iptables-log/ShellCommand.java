package com.googlecode.iptableslog;

import android.util.Log;

import java.lang.Runtime;
import java.lang.Process;
import java.io.InputStream;
import java.lang.Thread;

public class ShellCommand {
  Runtime rt;
  String[] command;
  Process process;
  InputStream stdout;

  public ShellCommand(String[] command) {
    this.command = command;
    rt = Runtime.getRuntime();
  }

  public void start() {
    try {
      Log.d("[Iptables Log]", "exec");
      process = rt.exec(command);
      stdout = process.getInputStream();

      Thread onShutdown = new Thread() {
        public void run() {
          Log.d("[Iptables Log]", "destroy on shutdown");
          process.destroy();
        }
      };
      rt.addShutdownHook(onShutdown);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public void stop() {
    Log.d("[Iptables Log]", "stop");
    process.destroy();
  }

  public boolean checkForExit() {
    try {
      Log.d("[Iptables Log]", "check exit");
      int exit = process.exitValue();
      Log.d("[Iptables Log]", "exit " + exit);
    } catch(Exception IllegalThreadStateException) {
      Log.d("[Iptables Log]", "still alive");
      return false;
    }

    Log.d("[Iptables Log]", "exited");
    process.destroy();
    return true;
  }

  public String readStdout() {
    byte[] buf = new byte[8192];
    int read;

    Log.d("[Iptables Log]", "readStdout");
    
    try {
      read = stdout.read(buf);
      if(read < 0) {
        Log.d("[Iptables Log]", "readStdout return null");
        return null;
      }

      String result = new String(buf, 0, read);
      Log.d("[Iptables Log]", "readStdout return [" + result + "]");
      return result;
    } catch(Exception e) {
      Log.d("[Iptables Log]", "error readStdout");
      e.printStackTrace();
      return "ERROR\n";
    }
  }
}
