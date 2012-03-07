package com.googlecode.iptableslog;

public interface IptablesLogListener {
  public void onNewLogEntry(IptablesLogTracker.LogEntry entry);
}
