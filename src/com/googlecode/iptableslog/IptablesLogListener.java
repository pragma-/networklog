package com.googlecode.iptableslog;

public interface IptablesLogListener {
  public void onNewLogEntry(IptablesLogService.LogEntry entry);
}
