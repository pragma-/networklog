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

  public static final int IGNORE_OUTPUT = (1 << 0);
  public static final int BACKGROUND    = (1 << 1);

  public InteractiveShell() {
    this("sh", "InteractiveShell");
  }

  public InteractiveShell(String shell) {
    this(shell, "InteractiveShell");
  }

  public InteractiveShell(String shell, String tag) {
    MyLog.d("Creating new InteractiveShell [" + tag + "]");
    this.shell = shell;
    this.tag = tag;
  }

  public ShellCommand getShell() {
    return command;
  }

  public void start() {
    MyLog.d("Starting InteractiveShell [" + tag + "]");
    command = new ShellCommand(new String[] { shell }, tag);
    command.start(false);
  }

  public int close() {
    MyLog.d("Closing InteractiveShell [" + tag + "]");

    if(command == null) {
      MyLog.d("No active ShellCommand for InteractiveShell [" + tag + "]");
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
    return sendCommand(cmd, 0);
  }

  public boolean sendCommand(String cmd, int flags) {
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

      if((flags & BACKGROUND) != BACKGROUND) {
        command.sendCommand("echo;echo ..EOF..$?\n");
      }

      if((flags & IGNORE_OUTPUT) == IGNORE_OUTPUT) {
        waitForCommandExit(null);
      }

      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public int waitForCommandExit(List<String> output) {
    String line;
    while((line = readLine()) != null) {
      if(output != null) {
        output.add(line);
      } else {
        if(MyLog.enabled) {
          MyLog.d("Discarding output: [" + line.trim() + "]");
        }
      }
    }
    return exitval;
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

  public boolean stdoutAvailable() {
    return command.stdout.lineAvailable();
  }

  public String readLine() {
    String line = command.stdout.readLine();
    if(line == null) {
      return null;
    } else if(line.startsWith("..EOF..")) {
      exitval = Integer.parseInt(line.substring(7, line.length()));
      if(MyLog.enabled) {
        MyLog.d("InteractiveShell [" + tag + "] command exited " + exitval);
      }
      return null;
    } else {
      return line;
    }
  }
}
