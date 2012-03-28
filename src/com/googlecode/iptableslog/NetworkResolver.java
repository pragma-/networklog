package com.googlecode.iptableslog;

import java.util.HashMap;
import java.net.InetAddress;

import android.util.Log;

public class NetworkResolver {
  HashMap<String, String> serviceMap = new HashMap<String, String>();
  HashMap<String, String> hostMap = new HashMap<String, String>();

  public NetworkResolver() {
    serviceMap.put("1","TCPMUX");
    serviceMap.put("5","JOB");
    serviceMap.put("7","ECHO");
    serviceMap.put("11","SYSTAT");
    serviceMap.put("13","DAYTIME");
    serviceMap.put("15","NETSTAT");
    serviceMap.put("17","QUOTE");
    serviceMap.put("18","MSGSEND");
    serviceMap.put("19","CHARGEN");
    serviceMap.put("20","FTPT");
    serviceMap.put("21","FTP");
    serviceMap.put("42","HOSTNAME");
    serviceMap.put("42","WINS");
    serviceMap.put("43","WHOIS");
    serviceMap.put("47","NIFTP");
    serviceMap.put("53","DNS");
    serviceMap.put("56","RAP");
    serviceMap.put("57","MAIL");
    serviceMap.put("67","BOOTP");
    serviceMap.put("68","BOOTP");
    serviceMap.put("69","TFTP");
    serviceMap.put("70","GOPHER");
    serviceMap.put("71","NETRJS");
    serviceMap.put("72","NETRJS");
    serviceMap.put("73","NETRJS");
    serviceMap.put("74","NETRJS");
    serviceMap.put("79","FINGER");
    serviceMap.put("80","HTTP");
    serviceMap.put("107","RTELNET");
    serviceMap.put("109","POP2");
    serviceMap.put("110","POP3");
    serviceMap.put("111","SUNRPC");
    serviceMap.put("113","AUTH");
    serviceMap.put("115","SFTP");
    serviceMap.put("117","UUCP");
    serviceMap.put("118","SQL");
    serviceMap.put("119","NNTP");
    serviceMap.put("123","NTP");
    serviceMap.put("135","DCE");
    serviceMap.put("135","RPCL");
    serviceMap.put("137","NETBIOSN");
    serviceMap.put("138","NETBIOSD");
    serviceMap.put("139","NETBIOSS");
    serviceMap.put("143","IMAP");
    serviceMap.put("152","BFTP");
    serviceMap.put("153","SGMP");
    serviceMap.put("156","SQL");
    serviceMap.put("158","DMSP");
    serviceMap.put("161","SNMP");
    serviceMap.put("162","SNMPTRAP");
    serviceMap.put("177","XDMCP");
    serviceMap.put("179","BORDER GATEWAY PROTOCOL");
    serviceMap.put("194","IRC");
    serviceMap.put("199","SMUX");
    serviceMap.put("201","APPLETALK");
    serviceMap.put("213","IPX");
    serviceMap.put("218","MPP");
    serviceMap.put("220","IMAP3");
    serviceMap.put("259","ESRO");
    serviceMap.put("264","BGMP");
    serviceMap.put("280","HTTP-MGMT");
    serviceMap.put("369","RPC2PORTMAP");
    serviceMap.put("370","SECURECAST1");
    serviceMap.put("389","LDAP");
    serviceMap.put("401","UPS");
    serviceMap.put("427","SLP");
    serviceMap.put("443","HTTPS");
    serviceMap.put("444","SNPP");
    serviceMap.put("445","SMB");
    serviceMap.put("464","KERBEROS");
    serviceMap.put("500","ISAKMP");
    serviceMap.put("517","TALK");
    serviceMap.put("518","NTALK");
    serviceMap.put("520","EFS");
    serviceMap.put("520","RIP");
    serviceMap.put("524","NCP");
    serviceMap.put("525","TIMED");
    serviceMap.put("530","RPC");
    serviceMap.put("531","AIM");
    serviceMap.put("532","NETNEWS");
    serviceMap.put("533","NETWALL");
    serviceMap.put("540","UUCP");
    serviceMap.put("542","COMMERCE");
    serviceMap.put("543","KLOGIN");
    serviceMap.put("544","KSHELL");
    serviceMap.put("546","DHCPV6C");
    serviceMap.put("547","DHCPV6S");
    serviceMap.put("548","AFP");
    serviceMap.put("550","RWHO");
    serviceMap.put("554","RTSP");
    serviceMap.put("556","REMOTEFS");
    serviceMap.put("560","RMONITOR");
    serviceMap.put("561","MONITOR");
    serviceMap.put("563","NNTPS");
    serviceMap.put("587","SMTP");
    serviceMap.put("591","HTTP2");
    serviceMap.put("593","HTTP-RPC");
    serviceMap.put("604","TUNNEL");
    serviceMap.put("623","ASF-RMCP");
    serviceMap.put("631","IPP");
    serviceMap.put("631","CUPS");
    serviceMap.put("635","RLZ-DBASE");
    serviceMap.put("636","LDAPS");
    serviceMap.put("639","MSDP");
    serviceMap.put("646","LDP");
    serviceMap.put("647","DHCPF");
    serviceMap.put("648","RRP");
    serviceMap.put("651","IEEE-MMS");
    serviceMap.put("654","MMS");
    serviceMap.put("657","RMC");
    serviceMap.put("666","DOOM");
    serviceMap.put("674","ACAP");
    serviceMap.put("691","MSEXCHANGE");
    serviceMap.put("694","HEARTBEAT");
    serviceMap.put("695","MMS-SSL");
    serviceMap.put("698","OLSR");
    serviceMap.put("700","EPP");
    serviceMap.put("701","LMP");
    serviceMap.put("702","IRIS");
    serviceMap.put("706","SILC");
    serviceMap.put("711","MPLS");
    serviceMap.put("712","TBRPF");
    serviceMap.put("749","KERBEROS");
    serviceMap.put("750","KERBEROS4");
    serviceMap.put("751","KERBEROS");
    serviceMap.put("752","KPASSWD");
    serviceMap.put("753","RRH");
    serviceMap.put("753","RRH");
    serviceMap.put("753","USERREG");
    serviceMap.put("754","TELLSEND");
    serviceMap.put("754","KRB5PROP");
    serviceMap.put("754","TELLSEND");
    serviceMap.put("760","KRBUPDATE");
    serviceMap.put("782","CONSERVER");
    serviceMap.put("783","SPAMD");
    serviceMap.put("843","FLASH");
    serviceMap.put("847","DHCPF");
    serviceMap.put("848","GDOI");
    serviceMap.put("860","ISCSI");
    serviceMap.put("873","RSYNC");
    serviceMap.put("888","CDDBP");
    serviceMap.put("901","SWAT");
    serviceMap.put("901","VMWARE");
    serviceMap.put("901","VMWARE");
    serviceMap.put("902","IDEAFARM-DOOR");
    serviceMap.put("902","VMWARE");
    serviceMap.put("902","IDEAFARM-DOOR");
    serviceMap.put("902","VMWARE");
    serviceMap.put("903","VMWARE");
    serviceMap.put("904","VMWARE");
    serviceMap.put("911","NCA");
    serviceMap.put("944","NFS");
    serviceMap.put("953","RNDC");
    serviceMap.put("973","NFS6");
    serviceMap.put("989","FTPSD");
    serviceMap.put("990","FTPSC");
    serviceMap.put("991","NAS");
    serviceMap.put("992","STELNET");
    serviceMap.put("993","IMAPS");
    serviceMap.put("995","POP3S");
  }

  public String resolveAddress(String address) {
    String resolved = hostMap.get(address);

    if(resolved == null) {
      try {
      InetAddress inetAddress = InetAddress.getByName(address);
      resolved = inetAddress.getHostName();
      hostMap.put(address, resolved);
      return resolved;
      } catch (Exception e) {
        Log.d("IptablesLog", e.toString(), e);
        return address;
      }
    } else {
      return resolved;
    }
  }

  public String resolveService(String service) {
    String name = serviceMap.get(service);
    if(name == null)
      return service;
    else
      return name;
  }
}
