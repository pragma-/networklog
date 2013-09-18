/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.HashMap;

public class Iptables {
  public static HashMap<String, String> targets;

  public static boolean getTargets(Context context) {
    String scriptFile = context.getFilesDir().getAbsolutePath() + File.separator + NetworkLog.SCRIPT;
    String grepBinary = SysUtils.getGrepBinary();

    if(grepBinary == null) {
      return false;
    }

    String grep  = context.getFilesDir().getAbsolutePath() + File.separator + grepBinary;

    synchronized(NetworkLog.SCRIPT) {
      try {
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(scriptFile)));
        script.println(grep + " \\.\\* /proc/net/ip_tables_targets");
        script.flush();
        script.close();
      } catch(java.io.IOException e) {
        Log.e("NetworkLog", "getTargets error", e);
      }

      ShellCommand command = new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "getTargets");
      command.start(false);

      if(command.error != null) {
        SysUtils.showError(context, context.getResources().getString(R.string.iptables_error_check_rules), command.error);
        return false;
      }

      targets = new HashMap<String, String>();

      StringBuilder result = new StringBuilder();
      String line;
      while(true) {
        line = command.readStdoutBlocking();
        if(line == null) {
          break;
        }
        line = line.trim();
        targets.put(line, line);
        result.append(line);
      }

      command.waitForExit();
      if(command.exitval != 0) {
        Log.e("NetworkLog", "Bad exit for getTargets (exit " + command.exitval + ")");
        SysUtils.showError(context, context.getResources().getString(R.string.iptables_error_check_rules), result.toString());
        return false;
      }

      MyLog.d("getTargets result: [" + result + "]");
      return true;
    }
  }

  public static boolean addRules(Context context) {
    String iptablesBinary = SysUtils.getIptablesBinary();
    if(iptablesBinary == null) {
      return false;
    }

    if(targets == null && getTargets(context) == false) {
      return false;
    }

    if(checkRules(context) == true) {
      removeRules(context);
    }

    synchronized(NetworkLog.SCRIPT) {
      String scriptFile = context.getFilesDir().getAbsolutePath() + File.separator + NetworkLog.SCRIPT;
      String iptables  = context.getFilesDir().getAbsolutePath() + File.separator + iptablesBinary;

      try {
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(scriptFile)));

        if(targets.get("NFLOG") != null) {
          if(NetworkLogService.behindFirewall) {
            script.println(iptables + " -A OUTPUT ! -o lo -j NFLOG --nflog-prefix \"{NL}\"");
            script.println(iptables + " -A INPUT ! -i lo -j NFLOG --nflog-prefix \"{NL}\"");
          } else {
            script.println(iptables + " -I OUTPUT 1 ! -o lo -j NFLOG --nflog-prefix \"{NL}\"");
            script.println(iptables + " -I INPUT 1 ! -i lo -j NFLOG --nflog-prefix \"{NL}\"");
          }
        } else if(targets.get("LOG") != null) {
          if(NetworkLogService.behindFirewall) {
            script.println(iptables + " -A OUTPUT ! -o lo -j LOG --log-prefix \"{NL}\" --log-uid");
            script.println(iptables + " -A INPUT ! -i lo -j LOG --log-prefix \"{NL}\" --log-uid");
          } else {
            script.println(iptables + " -I OUTPUT 1 ! -o lo -j LOG --log-prefix \"{NL}\" --log-uid");
            script.println(iptables + " -I INPUT 1 ! -i lo -j LOG --log-prefix \"{NL}\" --log-uid");
          }
        } else {
          SysUtils.showError(context,
              context.getResources().getString(R.string.iptables_error_unsupported_title),
              context.getResources().getString(R.string.iptables_error_missingfeatures_text));
          script.close();
          return false;
        }

        script.flush();
        script.close();
      } catch(java.io.IOException e) {
        Log.e("NetworkLog", "addRules error", e);
      }

      ShellCommand command = new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "addRules");
      command.start(false);

      if(command.error != null) {
        SysUtils.showError(context, context.getResources().getString(R.string.iptables_error_add_rules), command.error);
        return false;
      }

      StringBuilder result = new StringBuilder();
      String line;
      while(true) {
        line = command.readStdoutBlocking();
        if(line == null) {
          break;
        }
        result.append(line);
      }

      command.waitForExit();
      if(command.exitval != 0) {
        Log.e("NetworkLog", "Bad exit for addRules (exit " + command.exitval + ")");
        SysUtils.showError(context, context.getResources().getString(R.string.iptables_error_add_rules), result.toString());
        return false;
      }

      MyLog.d("addRules result: [" + result + "]");

      if(result.indexOf("No chain/target/match by that name", 0) != -1) {
        Resources res = context.getResources();
        SysUtils.showError(context,
            res.getString(R.string.iptables_error_unsupported_title),
            res.getString(R.string.iptables_error_missingfeatures_text));
        return false;
      }
    }

    return true;
  }

  public static boolean removeRules(Context context) {
    String iptablesBinary = SysUtils.getIptablesBinary();
    if(iptablesBinary == null) {
      return false;
    }

    if(targets == null && getTargets(context) == false) {
      return false;
    }

    String iptables  = context.getFilesDir().getAbsolutePath() + File.separator + iptablesBinary;
    int tries = 0;

    while(checkRules(context) == true) {
      synchronized(NetworkLog.SCRIPT) {
        String scriptFile = context.getFilesDir().getAbsolutePath() + File.separator + NetworkLog.SCRIPT;

        try {
          PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(scriptFile)));

          if(targets.get("NFLOG") != null) {
            script.println(iptables + " -D OUTPUT ! -o lo -j NFLOG --nflog-prefix \"{NL}\"");
            script.println(iptables + " -D INPUT ! -i lo -j NFLOG --nflog-prefix \"{NL}\"");
          } else if(targets.get("LOG") != null) {
            script.println(iptables + " -D OUTPUT ! -o lo -j LOG --log-prefix \"{NL}\" --log-uid");
            script.println(iptables + " -D INPUT ! -i lo -j LOG --log-prefix \"{NL}\" --log-uid");
          } else {
            SysUtils.showError(context,
                context.getResources().getString(R.string.iptables_error_unsupported_title),
                context.getResources().getString(R.string.iptables_error_missingfeatures_text));
            script.close();
            return false;
          }

          script.flush();
          script.close();
        } catch(java.io.IOException e) {
          Log.e("NetworkLog", "removeRules error", e);
        }

        ShellCommand cmd = new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "removeRules");
        cmd.start(true);

        if(cmd.error != null) {
          SysUtils.showError(context, context.getResources().getString(R.string.iptables_error_remove_rules), cmd.error);
          return false;
        }

        tries++;

        if(tries > 3) {
          Log.w("NetworkLog", "Too many attempts to remove rules, moving along...");
          return false;
        }
      }
    }

    return true;
  }

  public static String getRules(Context context) {
    return getRules(context, false);
  }

  public static String getRules(Context context, boolean verbose) {
    String iptablesBinary = SysUtils.getIptablesBinary();
    if(iptablesBinary == null) {
      return null;
    }

    String iptables  = context.getFilesDir().getAbsolutePath() + File.separator + iptablesBinary;

    synchronized(NetworkLog.SCRIPT) {
      String scriptFile = context.getFilesDir().getAbsolutePath() + File.separator + NetworkLog.SCRIPT;

      try {
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(scriptFile)));
        if(verbose) {
          script.println(iptables + " -L -v");
        } else {
          script.println(iptables + " -L");
        }

        script.flush();
        script.close();
      } catch(java.io.IOException e) {
        Log.e("NetworkLog", "getRules error", e);
      }

      ShellCommand command = new ShellCommand(new String[] { "su", "-c", "sh " + scriptFile }, "getRules");
      command.start(false);

      if(command.error != null) {
        SysUtils.showError(context, context.getResources().getString(R.string.iptables_error_check_rules), command.error);
        return null;
      }

      StringBuilder result = new StringBuilder();
      String line;
      while(true) {
        line = command.readStdoutBlocking();
        if(line == null) {
          break;
        }
        result.append(line);
      }

      command.waitForExit();
      if(command.exitval != 0) {
        Log.e("NetworkLog", "Bad exit for getRules (exit " + command.exitval + ")");
        SysUtils.showError(context, context.getResources().getString(R.string.iptables_error_check_rules), result.toString());
        return null;
      }

      if(MyLog.enabled) {
        MyLog.d("getRules result: [" + result.toString() + "]");
      }

      return result.toString();
    }
  }

  public static boolean checkRules(Context context) {
    String rules = getRules(context, true);

    if(rules == null) {
      return false;
    }

    if(rules.indexOf("Perhaps iptables or your kernel needs to be upgraded", 0) != -1) {
      Resources res = context.getResources();
      SysUtils.showError(context, res.getString(R.string.iptables_error_unsupported_title), res.getString(R.string.iptables_error_unsupported_text));
      return false;
    }

    return rules.indexOf("{NL}", 0) == -1 ? false : true;
  }
}
