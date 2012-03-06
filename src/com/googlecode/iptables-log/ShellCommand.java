package com.googlecode.iptableslog;

import android.util.Log;

import java.lang.Runtime;
import java.lang.Process;
import java.io.InputStream;
import java.lang.Thread;
import java.util.Arrays;

public class ShellCommand {
  Runtime rt;
  String[] command;
  String tag = "";
  Process process;
  InputStream stdout;

  public ShellCommand(String[] command, String tag) {
    this(command);
    this.tag = tag;
  }

  public ShellCommand(String[] command) {
    this.command = command;
    rt = Runtime.getRuntime();
  }

  public void start(boolean waitForExit) {
    Log.d("IptablesLog", "ShellCommand: starting [" + tag + "] " + Arrays.toString(command));
    try {
      process = new ProcessBuilder()
        .command(command)
        .redirectErrorStream(true)
        .start();

      stdout = process.getInputStream();
      /*
      process = rt.exec(command);
      stdout = process.getInputStream();

      Thread onShutdown = new  Thread() {
        public void run() {
          Log.d("IptablesLog", "shutdown hook finishing [" + tag + "]");
          finish();
        }
      };
      rt.addShutdownHook(onShutdown);
    */
    } catch(Exception e) {
      Log.d("IptablesLog", e.toString(), e);
      return;
    }

    if(waitForExit) {
      waitForExit();
    }
  }

  public void waitForExit() {
    while(checkForExit() == false) {
      if(stdoutAvailable())
        readStdout();
      else 
        try { Thread.sleep(100); } catch (Exception e) { Log.d("IptablesLog", e.toString(), e); }
    }
  }

  public void finish() {
    Log.d("IptablesLog", "ShellCommand: finishing [" + tag + "] " + Arrays.toString(command));
    process.destroy();
    process = null;
  }

  public boolean checkForExit() {
    try {
      int exit = process.exitValue();
      Log.d("IptablesLog", "ShellCommand exited: [" + tag + "] exit " + exit);
    } catch(Exception IllegalThreadStateException) {
      return false;
    }

    finish();
    return true;
  }

  public boolean stdoutAvailable() {
    try {
      Log.d("IptablesLog", "stdoutAvailable [" + tag + "]: " + stdout.available());
      return stdout.available() > 0;
    } catch (java.io.IOException e) { Log.d("IptablesLog", e.toString(), e); return false; }
  }

  public String readStdoutBlocking() {
    byte[] buf = new byte[8192];
    int read;

    Log.d("IptablesLog", "readStdoutBlocking [" + tag + "]");

    try {
      read = stdout.read(buf);

      if(read < 0) {
        Log.d("IptablesLog", "readStdoutBlocking [" + tag + "] return null");
        return null;
      }

      String result = new String(buf, 0, read);
      Log.d("IptablesLog", "readStdoutBlocking [" + tag + "] return [" + result + "]");
      return result;
    } catch (Exception e) {
      Log.d("IptablesLog", e.toString(), e);
      return "ERROR\n";
    }
  }

  public String readStdout() {
    byte[] buf = new byte[8192];
    int read;

    Log.d("IptablesLog", "readStdout [" + tag + "]");

    try {
      int available;
      String result = new String();

      while((available = stdout.available()) > 0) {
        Log.d("IptablesLog", "stdout available: " + available);

        read = stdout.read(buf);

        Log.d("IptablesLog", "read returned " + read);

        if(read < 0) {
          Log.d("IptablesLog", "readStdout return null");
          return null;
        }

        result += new String(buf, 0, read);
      }
      Log.d("IptablesLog", "readStdout return [" + result + "]");
      return result;
    } catch(Exception e) {
      Log.d("IptablesLog", "error readStdout");
      Log.d("IptablesLog", e.toString(), e);
      return "ERROR\n";
    }
  }
}
