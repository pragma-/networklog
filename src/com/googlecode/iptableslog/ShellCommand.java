package com.googlecode.iptableslog;

import android.util.Log;

import java.lang.Runtime;
import java.lang.Process;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.lang.Thread;
import java.util.Arrays;

public class ShellCommand {
  Runtime rt;
  String[] command;
  String tag = "";
  Process process;
  BufferedReader stdout;

  public ShellCommand(String[] command, String tag) {
    this(command);
    this.tag = tag;
  }

  public ShellCommand(String[] command) {
    this.command = command;
    rt = Runtime.getRuntime();
  }

  public void start(boolean waitForExit) {
    MyLog.d("ShellCommand: starting [" + tag + "] " + Arrays.toString(command));
    try {
      process = new ProcessBuilder()
        .command(command)
        .redirectErrorStream(true)
        .start();

      stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
    } catch(Exception e) {
      Log.d("IptablesLog", "Failure starting shell command [" + tag + "]", e);
      return;
    }

    if(waitForExit) {
      waitForExit();
    }
  }

  public void waitForExit() {
    while(checkForExit() == false) {
      if(stdoutAvailable())
        MyLog.d("ShellCommand waitForExit [" + tag + "] discarding read: " + readStdout());
      else 
        try { Thread.sleep(100); } catch (Exception e) { Log.d("IptablesLog", "waitForExit error", e); }
    }
  }

  public void finish() {
    MyLog.d("ShellCommand: finishing [" + tag + "] " + Arrays.toString(command));
    try {
      stdout.close();
    } catch (Exception e) { Log.d("IptablesLog", "Exception finishing [" + tag + "]", e); }

    process.destroy();
    process = null;
  }

  public boolean checkForExit() {
    try {
      int exit = process.exitValue();
      MyLog.d("ShellCommand exited: [" + tag + "] exit " + exit);
    } catch(Exception IllegalThreadStateException) {
      return false;
    }

    finish();
    return true;
  }

  public boolean stdoutAvailable() {
    try {
      MyLog.d("stdoutAvailable [" + tag + "]: " + stdout.ready());
      return stdout.ready();
    } catch (java.io.IOException e) { Log.d("IptablesLog", "stdoutAvailable error", e); return false; }
  }

  public String readStdoutBlocking() {
    MyLog.d("readStdoutBlocking [" + tag + "]");
    String line;

    try {
      line = stdout.readLine();
    } catch (Exception e) {
      Log.d("IptablesLog", "readStdoutBlocking error", e);
      return null;
    }

    if(MyLog.enabled) {
      MyLog.d("readStdoutBlocking [" + tag + "] return [" + line + "]");
    }

    if(line == null)
      return null;
    else
      return line + "\n";
  }

  public String readStdout() {
    MyLog.d("readStdout [" + tag + "]");

    try {
      if(stdout.ready()) {
        String line = stdout.readLine();
        MyLog.d("read line: [" + line + "]");
        if(line == null)
          return null;
        else
          return line + "\n";
      } else {
        MyLog.d("readStdout [" + tag + "] no data");
        return "";
      }
    } catch(Exception e) {
      Log.d("IptablesLog", "readStdout error", e);
      return null;
    }
  }
}
