/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.net.InetAddress;
import java.lang.Runnable;

public class NetworkResolver {
  final HashMap<String, String> serviceMap = new HashMap<String, String>();
  final HashMap<String, String> protocolMap = new HashMap<String, String>();
  final HashMap<String, String> resolvedHostMap = new HashMap<String, String>();
  final HashMap<String, Object> resolvingHostMap = new HashMap<String, Object>();
  final HashMap<String, ArrayList<NetworkResolverUpdater>> hostUpdatersMap = new HashMap<String, ArrayList<NetworkResolverUpdater>>();

  public NetworkResolver() {
    serviceMap.put("1", "TCPMUX");
    serviceMap.put("5", "JOB");
    serviceMap.put("7", "ECHO");
    serviceMap.put("11", "SYSTAT");
    serviceMap.put("13", "DAYTIME");
    serviceMap.put("15", "NETSTAT");
    serviceMap.put("17", "QUOTE");
    serviceMap.put("18", "MSGSEND");
    serviceMap.put("19", "CHARGEN");
    serviceMap.put("20", "FTPT");
    serviceMap.put("21", "FTP");
    serviceMap.put("42", "HOSTNAME");
    serviceMap.put("42", "WINS");
    serviceMap.put("43", "WHOIS");
    serviceMap.put("47", "NIFTP");
    serviceMap.put("53", "DNS");
    serviceMap.put("56", "RAP");
    serviceMap.put("57", "MAIL");
    serviceMap.put("67", "BOOTP");
    serviceMap.put("68", "BOOTP");
    serviceMap.put("69", "TFTP");
    serviceMap.put("70", "GOPHER");
    serviceMap.put("71", "NETRJS");
    serviceMap.put("72", "NETRJS");
    serviceMap.put("73", "NETRJS");
    serviceMap.put("74", "NETRJS");
    serviceMap.put("79", "FINGER");
    serviceMap.put("80", "HTTP");
    serviceMap.put("107", "RTELNET");
    serviceMap.put("109", "POP2");
    serviceMap.put("110", "POP3");
    serviceMap.put("111", "SUNRPC");
    serviceMap.put("113", "AUTH");
    serviceMap.put("115", "SFTP");
    serviceMap.put("117", "UUCP");
    serviceMap.put("118", "SQL");
    serviceMap.put("119", "NNTP");
    serviceMap.put("123", "NTP");
    serviceMap.put("135", "DCE");
    serviceMap.put("135", "RPCL");
    serviceMap.put("137", "NETBIOSN");
    serviceMap.put("138", "NETBIOSD");
    serviceMap.put("139", "NETBIOSS");
    serviceMap.put("143", "IMAP");
    serviceMap.put("152", "BFTP");
    serviceMap.put("153", "SGMP");
    serviceMap.put("156", "SQL");
    serviceMap.put("158", "DMSP");
    serviceMap.put("161", "SNMP");
    serviceMap.put("162", "SNMPTRAP");
    serviceMap.put("177", "XDMCP");
    serviceMap.put("179", "BORDER GATEWAY PROTOCOL");
    serviceMap.put("194", "IRC");
    serviceMap.put("199", "SMUX");
    serviceMap.put("201", "APPLETALK");
    serviceMap.put("213", "IPX");
    serviceMap.put("218", "MPP");
    serviceMap.put("220", "IMAP3");
    serviceMap.put("259", "ESRO");
    serviceMap.put("264", "BGMP");
    serviceMap.put("280", "HTTP-MGMT");
    serviceMap.put("369", "RPC2PORTMAP");
    serviceMap.put("370", "SECURECAST1");
    serviceMap.put("389", "LDAP");
    serviceMap.put("401", "UPS");
    serviceMap.put("427", "SLP");
    serviceMap.put("443", "HTTPS");
    serviceMap.put("444", "SNPP");
    serviceMap.put("445", "SMB");
    serviceMap.put("464", "KERBEROS");
    serviceMap.put("500", "ISAKMP");
    serviceMap.put("517", "TALK");
    serviceMap.put("518", "NTALK");
    serviceMap.put("520", "EFS");
    serviceMap.put("520", "RIP");
    serviceMap.put("524", "NCP");
    serviceMap.put("525", "TIMED");
    serviceMap.put("530", "RPC");
    serviceMap.put("531", "AIM");
    serviceMap.put("532", "NETNEWS");
    serviceMap.put("533", "NETWALL");
    serviceMap.put("540", "UUCP");
    serviceMap.put("542", "COMMERCE");
    serviceMap.put("543", "KLOGIN");
    serviceMap.put("544", "KSHELL");
    serviceMap.put("546", "DHCPV6C");
    serviceMap.put("547", "DHCPV6S");
    serviceMap.put("548", "AFP");
    serviceMap.put("550", "RWHO");
    serviceMap.put("554", "RTSP");
    serviceMap.put("556", "REMOTEFS");
    serviceMap.put("560", "RMONITOR");
    serviceMap.put("561", "MONITOR");
    serviceMap.put("563", "NNTPS");
    serviceMap.put("587", "SMTP");
    serviceMap.put("591", "HTTP2");
    serviceMap.put("593", "HTTP-RPC");
    serviceMap.put("604", "TUNNEL");
    serviceMap.put("623", "ASF-RMCP");
    serviceMap.put("631", "IPP");
    serviceMap.put("631", "CUPS");
    serviceMap.put("635", "RLZ-DBASE");
    serviceMap.put("636", "LDAPS");
    serviceMap.put("639", "MSDP");
    serviceMap.put("646", "LDP");
    serviceMap.put("647", "DHCPF");
    serviceMap.put("648", "RRP");
    serviceMap.put("651", "IEEE-MMS");
    serviceMap.put("654", "MMS");
    serviceMap.put("657", "RMC");
    serviceMap.put("666", "DOOM");
    serviceMap.put("674", "ACAP");
    serviceMap.put("691", "MSEXCHANGE");
    serviceMap.put("694", "HEARTBEAT");
    serviceMap.put("695", "MMS-SSL");
    serviceMap.put("698", "OLSR");
    serviceMap.put("700", "EPP");
    serviceMap.put("701", "LMP");
    serviceMap.put("702", "IRIS");
    serviceMap.put("706", "SILC");
    serviceMap.put("711", "MPLS");
    serviceMap.put("712", "TBRPF");
    serviceMap.put("749", "KERBEROS");
    serviceMap.put("750", "KERBEROS4");
    serviceMap.put("751", "KERBEROS");
    serviceMap.put("752", "KPASSWD");
    serviceMap.put("753", "RRH");
    serviceMap.put("753", "RRH");
    serviceMap.put("753", "USERREG");
    serviceMap.put("754", "TELLSEND");
    serviceMap.put("754", "KRB5PROP");
    serviceMap.put("754", "TELLSEND");
    serviceMap.put("760", "KRBUPDATE");
    serviceMap.put("782", "CONSERVER");
    serviceMap.put("783", "SPAMD");
    serviceMap.put("843", "FLASH");
    serviceMap.put("847", "DHCPF");
    serviceMap.put("848", "GDOI");
    serviceMap.put("860", "ISCSI");
    serviceMap.put("873", "RSYNC");
    serviceMap.put("888", "CDDBP");
    serviceMap.put("901", "SWAT");
    serviceMap.put("901", "VMWARE");
    serviceMap.put("901", "VMWARE");
    serviceMap.put("902", "IDEAFARM-DOOR");
    serviceMap.put("902", "VMWARE");
    serviceMap.put("902", "IDEAFARM-DOOR");
    serviceMap.put("902", "VMWARE");
    serviceMap.put("903", "VMWARE");
    serviceMap.put("904", "VMWARE");
    serviceMap.put("911", "NCA");
    serviceMap.put("944", "NFS");
    serviceMap.put("953", "RNDC");
    serviceMap.put("973", "NFS6");
    serviceMap.put("989", "FTPSD");
    serviceMap.put("990", "FTPSC");
    serviceMap.put("991", "NAS");
    serviceMap.put("992", "STELNET");
    serviceMap.put("993", "IMAPS");
    serviceMap.put("995", "POP3S");

    protocolMap.put("0", "HOPOPT");
    protocolMap.put("1", "ICMP");
    protocolMap.put("2", "IGMP");
    protocolMap.put("3", "GGP");
    protocolMap.put("4", "IPv4");
    protocolMap.put("5", "ST");
    protocolMap.put("6", "TCP");
    protocolMap.put("7", "CBT");
    protocolMap.put("8", "EGP");
    protocolMap.put("9", "IGP");
    protocolMap.put("10", "BBN");
    protocolMap.put("11", "NVP2");
    protocolMap.put("12", "PUP");
    protocolMap.put("13", "ARGUS");
    protocolMap.put("14", "EMCON");
    protocolMap.put("15", "XNET");
    protocolMap.put("16", "CHAOS");
    protocolMap.put("17", "UDP");
    protocolMap.put("18", "MUX");
    protocolMap.put("19", "DCN");
    protocolMap.put("20", "HMP");
    protocolMap.put("21", "PRM");
    protocolMap.put("22", "XNS");
    protocolMap.put("27", "RDP");
    protocolMap.put("28", "IRTP");
    protocolMap.put("29", "ISO");
    protocolMap.put("30", "BLT");
    protocolMap.put("31", "MFE-NSP");
    protocolMap.put("32", "MERIT-INP");
    protocolMap.put("33", "DCCP");
    protocolMap.put("34", "3PC");
    protocolMap.put("35", "IDPR");
    protocolMap.put("36", "XTP");
    protocolMap.put("37", "DDP");
    protocolMap.put("38", "IDPR-CMTP");
    protocolMap.put("39", "TP++");
    protocolMap.put("40", "IL");
    protocolMap.put("41", "IPv6");
    protocolMap.put("42", "SDRP");
    protocolMap.put("43", "IPv6-Route");
    protocolMap.put("44", "IPv6-Frag");
    protocolMap.put("45", "IDRP");
    protocolMap.put("46", "RSVP");
    protocolMap.put("47", "GRE");
    protocolMap.put("48", "MHRP");
    protocolMap.put("49", "BNA");
    protocolMap.put("50", "ESP");
    protocolMap.put("51", "AH");
    protocolMap.put("52", "I-NLSP");
    protocolMap.put("53", "SWIPE");
    protocolMap.put("54", "NARP");
    protocolMap.put("55", "MOBILE");
    protocolMap.put("56", "TLSP");
    protocolMap.put("57", "SKIP");
    protocolMap.put("58", "IPv6-ICMP");
    protocolMap.put("59", "IPv6-NoNxt");
    protocolMap.put("60", "IPv6-Opts");
    protocolMap.put("62", "CFTP");
    protocolMap.put("64", "SAT-EXPAK");
    protocolMap.put("65", "KRYPTOLAN");
    protocolMap.put("66", "RVD");
    protocolMap.put("67", "IPPC");
    protocolMap.put("69", "SAT-MON");
    protocolMap.put("70", "VISA");
    protocolMap.put("71", "IPCV");
    protocolMap.put("72", "CPNX");
    protocolMap.put("73", "CPHB");
    protocolMap.put("74", "WSN");
    protocolMap.put("75", "PVP");
    protocolMap.put("76", "BR-SAT-MON");
    protocolMap.put("77", "SUN-ND");
    protocolMap.put("78", "WB-MON");
    protocolMap.put("79", "WB-EXPAK");
    protocolMap.put("80", "ISO-IP");
    protocolMap.put("81", "VMTP");
    protocolMap.put("82", "SECURE-VMTP");
    protocolMap.put("83", "VINES");
    protocolMap.put("84", "TTP");
    protocolMap.put("84", "IPTM");
    protocolMap.put("85", "NSFNET-IGP");
    protocolMap.put("86", "DGP");
    protocolMap.put("87", "TCF");
    protocolMap.put("88", "EIGRP");
    protocolMap.put("89", "OSPF");
    protocolMap.put("90", "SPRITE-RPC");
    protocolMap.put("91", "LARP");
    protocolMap.put("92", "MTP");
    protocolMap.put("93", "AX.25");
    protocolMap.put("94", "IPIP");
    protocolMap.put("95", "MICP");
    protocolMap.put("96", "SCC-SP");
    protocolMap.put("97", "ETHERIP");
    protocolMap.put("98", "ENCAP");
    protocolMap.put("100", "GMTP");
    protocolMap.put("101", "IFMP");
    protocolMap.put("102", "PNNI");
    protocolMap.put("103", "PIM");
    protocolMap.put("104", "ARIS");
    protocolMap.put("105", "SCPS");
    protocolMap.put("106", "QNX");
    protocolMap.put("107", "A/N");
    protocolMap.put("108", "IPComp");
    protocolMap.put("109", "SNP");
    protocolMap.put("110", "Compaq-Peer");
    protocolMap.put("111", "IPX-in-IP");
    protocolMap.put("112", "VRRP");
    protocolMap.put("113", "PGM");
    protocolMap.put("115", "L2TP");
    protocolMap.put("116", "DDX");
    protocolMap.put("117", "IATP");
    protocolMap.put("118", "STP");
    protocolMap.put("119", "SRP");
    protocolMap.put("120", "UTI");
    protocolMap.put("121", "SMP");
    protocolMap.put("122", "SM");
    protocolMap.put("123", "PTP");
    protocolMap.put("125", "FIRE");
    protocolMap.put("126", "CRTP");
    protocolMap.put("127", "CRUDP");
    protocolMap.put("128", "SSCOPMCE");
    protocolMap.put("129", "IPLT");
    protocolMap.put("130", "SPS");
    protocolMap.put("131", "PIPE");
    protocolMap.put("132", "SCTP");
    protocolMap.put("133", "FC");
    protocolMap.put("138", "MANET");
    protocolMap.put("139", "HIP");
    protocolMap.put("140", "SHIM6");
  }

  public String getResolvedAddress(final String address) {
    return resolvedHostMap.get(address);
  }

  public String resolveAddress(final String address) {
    return resolveAddress(address, null);
  }

  public String resolveAddress(final String address, final NetworkResolverUpdater updater) {
    String resolved;
    synchronized(resolvedHostMap) {
      resolved = resolvedHostMap.get(address);
    }

    if(resolved == null && updater != null) {
      synchronized(hostUpdatersMap) {
        ArrayList<NetworkResolverUpdater> updaters = hostUpdatersMap.get(address);
        if(updaters == null) {
          updaters = new ArrayList<NetworkResolverUpdater>();
          hostUpdatersMap.put(address, updaters);
        }
        updaters.add(updater);
      }
    }

    Object resolving;
    synchronized(resolvingHostMap) {
      resolving = resolvingHostMap.get(address);
    }

    if(resolving != null) {
      return null;
    }

    if(resolved == null) {
      synchronized(resolvingHostMap) {
        resolvingHostMap.put(address, address);
      }

      new Thread(new Runnable() {
        public void run() {
          try {
            if(MyLog.enabled && MyLog.level >= 1) {
              MyLog.d(1, "Resolving " + address);
            }

            InetAddress inetAddress = InetAddress.getByName(address);
            String resolved = inetAddress.getHostName();
            
            synchronized(resolvedHostMap) {
              resolvedHostMap.put(address, resolved);
            }

            synchronized(resolvingHostMap) {
              resolvingHostMap.remove(address);
            }

            if(MyLog.enabled && MyLog.level >= 1) {
              MyLog.d(1, "Resolved " + address + " to " + resolved);
            }

            synchronized(hostUpdatersMap) {
              ArrayList<NetworkResolverUpdater> updaters = hostUpdatersMap.get(address);
              if(updaters != null) {
                Iterator<NetworkResolverUpdater> iterator = updaters.iterator();
                while(iterator.hasNext()) {
                  NetworkResolverUpdater update = iterator.next();
                  update.setResolved(resolved);
                  NetworkLog.handler.postDelayed(update, 500);
                }
                updaters.clear();
                hostUpdatersMap.remove(address);
              }
            }
          } catch(Exception e) {
            Log.d("NetworkLog", e.toString(), e);
          }
        }
      }, "NetResolv:" + address).start();

      return null;
    } else {
      return resolved;
    }
  }

  public String resolveService(String service) {
    String name = serviceMap.get(service);

    if(name == null) {
      return service;
    }
    else {
      return name;
    }
  }

  public String resolveProtocol(String protocol) {
    String name = protocolMap.get(protocol);

    if(name == null) {
      return protocol;
    }
    else {
      return name;
    }
  }
}
