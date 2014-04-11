/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.util.Log;

import java.lang.Runtime;
import java.lang.Process;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.Thread;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ShellCommand {
  Runtime rt;
  String[] command;
  String tag = "";
  Process process;
  DataOutputStream stdin;
  StreamReader stdout;
  private String error;
  public int exitval;

  public ShellCommand(String[] command, String tag) {
    this(command);
    this.tag = tag;
  }

  public ShellCommand(String[] command) {
    this.command = command;
    rt = Runtime.getRuntime();
  }

  public String[] start(boolean waitForExit) {
    MyLog.d("ShellCommand: starting [" + tag + "] " + Arrays.toString(command));

    exitval = -1;
    error = null;

    try {
      process = new ProcessBuilder()
        .command(command)
        .redirectErrorStream(true)
        .start();

      stdout = new StreamReader(process.getInputStream());
      stdout.start();

    } catch(Exception e) {
      Log.e("NetworkLog", "Failure starting shell command [" + tag + "]", e);
      error = e.getCause().getMessage();
      return null;
    }

    if(waitForExit) {
      waitForExit();
      String[] output = stdout.buffer.toArray(new String[stdout.buffer.size()]);
      return output;
    }

    stdin = new DataOutputStream(process.getOutputStream());
    return null;
  }

  public boolean hasError() {
    return error != null;
  }

  public String getError(boolean clearError) {
    if(clearError) {
      String ret = error;
      error = null;
      return ret;
    } else {
      return error;
    }
  }

  public int waitForExit() {
    try {
      exitval = process.waitFor();
      MyLog.d("ShellCommand exited: [" + tag + "] exit " + exitval);
      finish();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return exitval;
  }

  public boolean checkForExit() {
    return checkForExit(false);
  }

  public boolean checkForExit(boolean ignoreStdout) {
    if(process == null) {
      System.out.println("checkForExit process null");
      return true;
    }

    try {
      exitval = process.exitValue();
      if(ignoreStdout == true) {
        MyLog.d("ShellCommand exited: [" + tag + "] exit " + exitval);
        return true;
      } else if(stdout.buffer.size() == 0) {
        MyLog.d("ShellCommand exited: [" + tag + "] exit " + exitval);
        return true;
      } else {
        return false;
      }
    } catch(IllegalThreadStateException e) {
      return false;
    }
  }

  public void close() {
    finish();
  }

  public void finish() {
    MyLog.d("ShellCommand: finishing [" + tag + "] " + Arrays.toString(command));

    try {
      stdout.join();
      stdout.close();

      if(stdin != null) {
        stdin.close();
      }
    } catch(Exception e) {
      Log.e("NetworkLog", "Exception finishing [" + tag + "]", e);
    }

    process.destroy();
  }

  public boolean sendCommand(String command) {
    if(stdin == null) {
      Log.e("NetworkLog", "ShellCommand [" + tag + "] Error attempting to execute command [" + command.trim() + "] -- process has no stdin");
      error = "Process has no stdin";
      return false;
    }
    
    if(checkForExit(true)) {
      Log.e("NetworkLog", "ShellCommand [" + tag + "] Error attempting to execute command [" + command.trim() + "] -- process has exited");
      error = "Process has exited";
      return false;
    }

    try {
      stdin.writeBytes(command);
      stdin.writeBytes("\n");
      stdin.flush();
    } catch (IOException e) {
      e.printStackTrace();
      error = e.getCause().getMessage();
      return false;
    }
    return true;
  }

  class StreamReader extends Thread {
    InputStream is;
    String tag;
    LinkedBlockingQueue<String> buffer = new LinkedBlockingQueue<String>();

    StreamReader(InputStream is) {
      this(is, null);
    }

    StreamReader(InputStream is, String tag) {
      this.is = is;
      this.tag = tag;
    }

    public void run() {
      String outTag = null;

      if(tag != null && tag.length() > 0) {
        outTag = tag + "> ";
      }

      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while((line = reader.readLine()) != null) {
          if(tag == null || tag.length() == 0) {
            buffer.put(line);
          } else {
            buffer.put(outTag + line);
          }
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      System.out.println("StreamReader done");
    }

    public boolean lineAvailable() {
      return buffer.size() != 0;
    }

    public String readLine() {
      String result;

      try {
        if(checkForExit() && buffer.size() == 0) {
          return null;
        }
        while(checkForExit() == false) {
          result = buffer.poll(200, TimeUnit.MILLISECONDS);
          if(result != null) {
            return result;
          }
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return null;
    }

    public void close() {
      try {
        is.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
