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

  public void start(boolean waitForExit) {
    try {
      process = rt.exec(command);
      stdout = process.getInputStream();

      Thread onShutdown = new Thread() {
        public void run() {
          Log.d("IptablesLog", "finish on shutdown");
          finish();
        }
      };
      rt.addShutdownHook(onShutdown);
    } catch(Exception e) {
      e.printStackTrace();
      return;
    }

    if(waitForExit) {
      waitForExit();
    }
  }

  public void waitForExit() {
    while(checkForExit() == false) {
      try { Thread.sleep(100); } catch (Exception e) { e.printStackTrace(); }
    }
  }

  public void finish() {
    Log.d("IptablesLog", "ShellCommand: finishing " + command.toString());
    process.destroy();
  }

  public boolean checkForExit() {
    try {
      int exit = process.exitValue();
      Log.d("IptablesLog", "exit " + exit);
    } catch(Exception IllegalThreadStateException) {
      return false;
    }

    Log.d("IptablesLog", "exited");
    finish();
    return true;
  }

  public String readStdout() {
    byte[] buf = new byte[8192];
    int read;

    Log.d("IptablesLog", "readStdout");
    
    try {
      read = stdout.read(buf);
      if(read < 0) {
        Log.d("IptablesLog", "readStdout return null");
        return null;
      }

      String result = new String(buf, 0, read);
      Log.d("IptablesLog", "readStdout return [" + result + "]");
      return result;
    } catch(Exception e) {
      Log.d("IptablesLog", "error readStdout");
      e.printStackTrace();
      return "ERROR\n";
    }
  }
}
