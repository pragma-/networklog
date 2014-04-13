/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Iptables {
  public static HashMap<String, String> targets = null;

  public static boolean getTargets(Context context) {
    if(targets != null) {
      return true;
    }

    targets = new HashMap<String, String>();

    if(!NetworkLog.shell.sendCommand("cat /proc/net/ip_tables_targets")) {
      SysUtils.showError(context, context.getResources().getString(R.string.iptables_error_check_rules), NetworkLog.shell.getError(true));
      return false;
    }

    List<String> output = new ArrayList<String>();
    if(NetworkLog.shell.waitForCommandExit(output) != 0) {
      String error = "";
      for(String line : output) {
        error += line;
      }
      Log.e("NetworkLog", "Bad exit for getTargets (exit " + NetworkLog.shell.exitval + ")");
      SysUtils.showError(context, context.getResources().getString(R.string.iptables_error_check_rules), error);
      return false;
    }

    StringBuilder result = new StringBuilder();
    for(String line : output) {
      line = line.trim();
      targets.put(line, line);
      result.append(line).append(" ");
    }

    MyLog.d("getTargets result: [" + result + "]");
    return true;
  }

  public static boolean addRules(Context context) {
    String iptablesBinary = SysUtils.getIptablesBinary(context);
    if(iptablesBinary == null) {
      return false;
    }

    if(targets == null && getTargets(context) == false) {
      return false;
    }

    if(checkRules(context) == true) {
      removeRules(context);
    }

    ArrayList<String> commands = new ArrayList<String>();

    if(targets.get("LOG") != null) {
      if(NetworkLogService.behindFirewall) {
        commands.add(iptablesBinary + " -A OUTPUT ! -o lo -j LOG --log-prefix \"{NL}\" --log-uid");
        commands.add(iptablesBinary + " -A INPUT ! -i lo -j LOG --log-prefix \"{NL}\" --log-uid");
      } else {
        commands.add(iptablesBinary + " -I OUTPUT 1 ! -o lo -j LOG --log-prefix \"{NL}\" --log-uid");
        commands.add(iptablesBinary + " -I INPUT 1 ! -i lo -j LOG --log-prefix \"{NL}\" --log-uid");
      }
    } else if(targets.get("NFLOG") != null) {
      if(NetworkLogService.behindFirewall) {
        commands.add(iptablesBinary + " -A OUTPUT ! -o lo -j NFLOG --nflog-prefix \"{NL}\"");
        commands.add(iptablesBinary + " -A INPUT ! -i lo -j NFLOG --nflog-prefix \"{NL}\"");
      } else {
        commands.add(iptablesBinary + " -I OUTPUT 1 ! -o lo -j NFLOG --nflog-prefix \"{NL}\"");
        commands.add(iptablesBinary + " -I INPUT 1 ! -i lo -j NFLOG --nflog-prefix \"{NL}\"");
      }
    } else {
      SysUtils.showError(context,
          context.getResources().getString(R.string.iptables_error_unsupported_title),
          context.getResources().getString(R.string.iptables_error_missingfeatures_text));
      return false;
    }

    for(String command : commands) {
      if(!NetworkLog.shell.sendCommand(command)) {
        SysUtils.showError(context, context.getResources().getString(R.string.iptables_error_add_rules), NetworkLog.shell.getError(true));
        return false;
      }

      List<String> output = new ArrayList<String>();
      NetworkLog.shell.waitForCommandExit(output);

      StringBuilder result = new StringBuilder();
      for(String line : output) {
        result.append(line);
      }

      if(MyLog.enabled) {
        MyLog.d("addRules result: [" + result + "]");
      }

      if(NetworkLog.shell.exitval != 0) {
        Log.e("NetworkLog", "Bad exit for addRules (exit " + NetworkLog.shell.exitval + ")");
        SysUtils.showError(context, context.getResources().getString(R.string.iptables_error_add_rules), result.toString());
        return false;
      }

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
    String iptablesBinary = SysUtils.getIptablesBinary(context);
    if(iptablesBinary == null) {
      return false;
    }

    if(targets == null && getTargets(context) == false) {
      return false;
    }

    int tries = 0;

    while(checkRules(context) == true) {
      ArrayList<String> commands = new ArrayList<String>();
      if(targets.get("NFLOG") != null) {
        commands.add(iptablesBinary + " -D OUTPUT ! -o lo -j NFLOG --nflog-prefix \"{NL}\"");
        commands.add(iptablesBinary + " -D INPUT ! -i lo -j NFLOG --nflog-prefix \"{NL}\"");
      } else if(targets.get("LOG") != null) {
        commands.add(iptablesBinary + " -D OUTPUT ! -o lo -j LOG --log-prefix \"{NL}\" --log-uid");
        commands.add(iptablesBinary + " -D INPUT ! -i lo -j LOG --log-prefix \"{NL}\" --log-uid");
      } else {
        SysUtils.showError(context,
            context.getResources().getString(R.string.iptables_error_unsupported_title),
            context.getResources().getString(R.string.iptables_error_missingfeatures_text));
        return false;
      }

      for(String command : commands) {
        if(!NetworkLog.shell.sendCommand(command)) {
          SysUtils.showError(context, context.getResources().getString(R.string.iptables_error_remove_rules), NetworkLog.shell.getError(true));
          return false;
        }

        List<String> output = new ArrayList<String>();
        NetworkLog.shell.waitForCommandExit(output);

        StringBuilder result = new StringBuilder();
        for(String line : output) {
          result.append(line);
        }

        if(MyLog.enabled) {
          MyLog.d("removeRules result: [" + result + "]");
        }

        if(NetworkLog.shell.exitval != 0) {
          Log.e("NetworkLog", "Bad exit for removeRules (exit " + NetworkLog.shell.exitval + ")");
          SysUtils.showError(context, context.getResources().getString(R.string.iptables_error_remove_rules), result.toString());
          return false;
        }
      }

      tries++;

      if(tries > 3) {
        Log.w("NetworkLog", "Too many attempts to remove rules, moving along...");
        return false;
      }
    }

    return true;
  }

  public static String getRules(Context context) {
    return getRules(context, false);
  }

  public static String getRules(Context context, boolean verbose) {
    String iptablesBinary = SysUtils.getIptablesBinary(context);
    if(iptablesBinary == null) {
      return null;
    }

    String command;

    if(verbose) {
      command = iptablesBinary + " -L -v";
    } else {
      command = iptablesBinary + " -L";
    }

    if(!NetworkLog.shell.sendCommand(command)) {
      SysUtils.showError(context, context.getResources().getString(R.string.iptables_error_check_rules), NetworkLog.shell.getError(true));
      return null;
    }

    List<String> output = new ArrayList<String>();
    NetworkLog.shell.waitForCommandExit(output);

    StringBuilder result = new StringBuilder();
    for(String line : output) {
      result.append(line);
    }

    if(MyLog.enabled) {
      MyLog.d("getRules result: [" + result + "]");
    }

    if(NetworkLog.shell.exitval != 0) {
      Log.e("NetworkLog", "Bad exit for getRules (exit " + NetworkLog.shell.exitval + ")");
      SysUtils.showError(context, context.getResources().getString(R.string.iptables_error_check_rules), result.toString());
      return null;
    }

    return result.toString();
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
