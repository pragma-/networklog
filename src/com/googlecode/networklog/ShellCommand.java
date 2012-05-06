package com.googlecode.networklog;

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
  public int exit;

  public ShellCommand(String[] command, String tag) {
    this(command);
    this.tag = tag;
  }

  public ShellCommand(String[] command) {
    this.command = command;
    rt = Runtime.getRuntime();
  }

  public String start(boolean waitForExit) {
    MyLog.d("ShellCommand: starting [" + tag + "] " + Arrays.toString(command));

    try {
      process = new ProcessBuilder()
        .command(command)
        .redirectErrorStream(true)
        .start();

      stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
    } catch(Exception e) {
      Log.e("NetworkLog", "Failure starting shell command [" + tag + "]", e);
      return e.getCause().getMessage();
    }

    if(waitForExit) {
      waitForExit();
    }
    return null;
  }

  public void waitForExit() {
    while(checkForExit() == false) {
      if(stdoutAvailable()) {
        MyLog.d("ShellCommand waitForExit [" + tag + "] discarding read: " + readStdout());
      }
      else
        try {
          Thread.sleep(100);
        }
      catch(Exception e) {
        Log.e("NetworkLog", "waitForExit error", e);
      }
    }
  }

  public void finish() {
    MyLog.d("ShellCommand: finishing [" + tag + "] " + Arrays.toString(command));

    try {
      if(stdout != null) {
        stdout.close();
      }
    } catch(Exception e) {
      Log.e("NetworkLog", "Exception finishing [" + tag + "]", e);
    }

    process.destroy();
    process = null;
  }

  public boolean checkForExit() {
    try {
      exit = process.exitValue();
      MyLog.d("ShellCommand exited: [" + tag + "] exit " + exit);
    } catch(Exception IllegalThreadStateException) {
      return false;
    }

    finish();
    return true;
  }

  public boolean stdoutAvailable() {
    try {
      if(MyLog.enabled) {
        MyLog.d("stdoutAvailable [" + tag + "]: " + stdout.ready());
      }
      return stdout.ready();
    } catch(java.io.IOException e) {
      Log.e("NetworkLog", "stdoutAvailable error", e);
      return false;
    }
  }

  public String readStdoutBlocking() {
    if(MyLog.enabled) {
      MyLog.d("readStdoutBlocking [" + tag + "]");
    }
    String line;

    if(stdout == null) {
      return null;
    }

    try {
      line = stdout.readLine();
    } catch(Exception e) {
      Log.e("NetworkLog", "readStdoutBlocking error", e);
      return null;
    }

    if(MyLog.enabled) {
      MyLog.d("readStdoutBlocking [" + tag + "] return [" + line + "]");
    }

    if(line == null) {
      return null;
    }
    else {
      return line + "\n";
    }
  }

  public String readStdout() {
    if(MyLog.enabled) {
      MyLog.d("readStdout [" + tag + "]");
    }

    if(stdout == null) {
      return null;
    }

    try {
      if(stdout.ready()) {
        String line = stdout.readLine();
        if(MyLog.enabled) {
          MyLog.d("read line: [" + line + "]");
        }

        if(line == null) {
          return null;
        }
        else {
          return line + "\n";
        }
      } else {
        MyLog.d("readStdout [" + tag + "] no data");
        return "";
      }
    } catch(Exception e) {
      Log.e("NetworkLog", "readStdout error", e);
      return null;
    }
  }
}
