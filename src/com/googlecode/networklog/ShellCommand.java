/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

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
  public String error;
  public int exitval;

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

    exitval = -1;
    error = null;

    try {
      process = new ProcessBuilder()
        .command(command)
        .redirectErrorStream(true)
        .start();

      stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
    } catch(Exception e) {
      Log.e("NetworkLog", "Failure starting shell command [" + tag + "]", e);
      error = e.getCause().getMessage();
      return;
    }

    if(waitForExit) {
      waitForExit();
    }
  }

  public void waitForExit() {
    while(checkForExit() == false) {
      if(stdoutAvailable()) {
        if(MyLog.enabled && MyLog.level >= 3) {
          MyLog.d(3, "ShellCommand waitForExit [" + tag + "] discarding read: " + readStdout());
        }
      } else {
        try {
          Thread.sleep(100);
        } catch(Exception e) {
          Log.d("NetworkLog", "waitForExit", e);
        }
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
      exitval = process.exitValue();
      MyLog.d("ShellCommand exited: [" + tag + "] exit " + exitval);
    } catch(IllegalThreadStateException e) {
      return false;
    }

    finish();
    return true;
  }

  public boolean stdoutAvailable() {
    try {
      /*
      if(MyLog.enabled) {
        MyLog.d("stdoutAvailable [" + tag + "]: " + stdout.ready());
      }
      */
      return stdout.ready();
    } catch(java.io.IOException e) {
      Log.e("NetworkLog", "stdoutAvailable error", e);
      return false;
    }
  }

  public String readStdoutBlocking() {
    if(MyLog.enabled && MyLog.level >= 3) {
      MyLog.d(3, "readStdoutBlocking [" + tag + "]");
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

    if(MyLog.enabled && MyLog.level >= 3) {
      MyLog.d(3, "readStdoutBlocking [" + tag + "] return [" + line + "]");
    }

    if(line == null) {
      return null;
    }
    else {
      return line + "\n";
    }
  }

  public String readStdout() {
    if(MyLog.enabled && MyLog.level >= 3) {
      MyLog.d(3, "readStdout [" + tag + "]");
    }

    if(stdout == null) {
      return null;
    }

    try {
      if(stdout.ready()) {
        String line = stdout.readLine();
        if(MyLog.enabled && MyLog.level >= 3) {
          MyLog.d(3, "read line: [" + line + "]");
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
