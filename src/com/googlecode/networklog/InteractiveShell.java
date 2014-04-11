/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

public class InteractiveShell {
  ShellCommand command;
  String shell;
  String tag;
  int exitval;

  public InteractiveShell() {
    this("sh", "InteractiveShell");
  }

  public InteractiveShell(String shell) {
    this(shell, "InteractiveShell");
  }

  public InteractiveShell(String shell, String tag) {
    this.shell = shell;
    this.tag = tag;
  }

  public ShellCommand getShell() {
    return command;
  }

  public void start() {
    command = new ShellCommand(new String[] { shell }, tag);
    command.start(false);
  }

  public int close() {
    if(command == null) {
      return -1;
    }

    command.sendCommand("exit\n");
    return command.waitForExit();
  }

  public boolean hasError() {
    return command.hasError();
  }

  public String getError(boolean clearError) {
    return command.getError(clearError);
  }

  public boolean checkForExit() {
    if(command.checkForExit()) {
      exitval = command.exitval;
      return true;
    } else {
      return false;
    }
  }

  public boolean checkForExit(boolean ignoreStdout) {
    if(command.checkForExit(ignoreStdout)) {
      exitval = command.exitval;
      return true;
    } else {
      return false;
    }
  }

  public int waitForExit() {
    exitval = command.waitForExit();
    return exitval;
  }

  public boolean sendCommand(String cmd) {
    return sendCommand(cmd, false);
  }

  public boolean sendCommand(String cmd, boolean ignoreOutput) {
    try {
      if(command == null || !command.sendCommand(cmd)) {
        Log.w("NetworkLog", "[" + tag + "] shell not running, starting new shell");
        getError(true); // clear the error
        start();
        if(!command.sendCommand(cmd)) {
          Log.e("NetworkLog", "[" + tag + "] Unable to execute command [" + cmd.trim() + "]: " + getError(false));
          return false;
        }
      }

      command.sendCommand("echo ..EOF..$?\n");

      if(ignoreOutput) {
        eatOutput();
      }
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public Integer peekAtCommandExitValue() {
    return peekAtCommandExitValue(Integer.MAX_VALUE);
  }

  public Integer peekAtCommandExitValue(int maxLines) {
    Iterator<String> iterator = command.stdout.buffer.iterator();
    int lines = 0;

    while(iterator.hasNext() && lines < maxLines) {
      String line = iterator.next();
      lines++;

      if(line.startsWith("..EOF..")) {
        try {
          return Integer.valueOf(line.substring(7, line.length()));
        } catch (Exception e) {
          Log.w("NetworkLog", "PeekAtCommandExitValue [" + tag + "] encountered EOF without valid exit value [" + line.trim() + "]");
          continue;
        }
      }
    }
    return null;
  }

  public int waitForCommandExit(List<String> output) {
    String line;

    while((line = readLine()) != null) {
      output.add(line);
    }

    return exitval;
  }

  public void eatOutput() {
    Log.d("NetworkLog", "Eating output");
    String line;
    while((line = readLine()) != null) {
      Log.d("NetworkLog", "Discarding output: [" + line.trim() + "]");
    }
  }

  public boolean stdoutAvailable() {
    return command.stdout.lineAvailable();
  }

  public String readLine() {
    String line = command.stdout.readLine();
    if(line == null) {
      return null;
    } else if(line.startsWith("..EOF..")) {
      exitval = Integer.parseInt(line.substring(7, line.length()));
      return null;
    } else {
      return line;
    }
  }
}
