#!/usr/bin/env perl

use warnings;
use strict;

my $md5s = `bash -c "md5sum libs/**/*"`;

print "$md5s\n";

my ($grep_armv5) = $md5s =~ m{^([^ ]+) \*libs/armeabi/grep$}m;
my ($nflog_armv5) = $md5s =~ m{^([^ ]+) \*libs/armeabi/nflog$}m;
my ($run_pie_armv5) = $md5s =~ m{^([^ ]+) \*libs/armeabi/run_pie$}m;

my ($grep_armv7) = $md5s =~ m{^([^ ]+) \*libs/armeabi-v7a/grep$}m;
my ($nflog_armv7) = $md5s =~ m{^([^ ]+) \*libs/armeabi-v7a/nflog$}m;
my ($run_pie_armv7) = $md5s =~ m{^([^ ]+) \*libs/armeabi-v7a/run_pie$}m;

my ($grep_x86) = $md5s =~ m{^([^ ]+) \*libs/x86/grep$}m;
my ($nflog_x86) = $md5s =~ m{^([^ ]+) \*libs/x86/nflog$}m;
my ($run_pie_x86) = $md5s =~ m{^([^ ]+) \*libs/x86/run_pie$}m;

my ($grep_mips) = $md5s =~ m{^([^ ]+) \*libs/mips/grep$}m;
my ($nflog_mips) = $md5s =~ m{^([^ ]+) \*libs/mips/nflog$}m;
my ($run_pie_mips) = $md5s =~ m{^([^ ]+) \*libs/mips/run_pie$}m;

open my $in, '<', 'src/com/googlecode/networklog/SysUtils.java' or die "Couldn't open SysUtils.java for reading: $!";
my @lines = <$in>;
close $in;

open my $out, '>', 'src/com/googlecode/networklog/SysUtils.java' or die "Couldn't open SysUtils.java for writing: $!";

foreach my $text (@lines) {
  $text =~ s/grepMd5 = ".*";\s+\/\/ grep_armv5/grepMd5 = "$grep_armv5";  \/\/ grep_armv5/;
  $text =~ s/nflogMd5 = ".*";\s+\/\/ nflog_armv5/nflogMd5 = "$nflog_armv5";  \/\/ nflog_armv5/;
  $text =~ s/run_pieMd5 = ".*";\s+\/\/ run_pie_armv5/run_pieMd5 = "$run_pie_armv5";  \/\/ run_pie_armv5/;

  $text =~ s/grepMd5 = ".*";\s+\/\/ grep_armv7/grepMd5 = "$grep_armv7";  \/\/ grep_armv7/;
  $text =~ s/nflogMd5 = ".*";\s+\/\/ nflog_armv7/nflogMd5 = "$nflog_armv7";  \/\/ nflog_armv7/;
  $text =~ s/run_pieMd5 = ".*";\s+\/\/ run_pie_armv7/run_pieMd5 = "$run_pie_armv7";  \/\/ run_pie_armv7/;

  $text =~ s/grepMd5 = ".*";\s+\/\/ grep_x86/grepMd5 = "$grep_x86";  \/\/ grep_x86/;
  $text =~ s/nflogMd5 = ".*";\s+\/\/ nflog_x86/nflogMd5 = "$nflog_x86";  \/\/ nflog_x86/;
  $text =~ s/run_pieMd5 = ".*";\s+\/\/ run_pie_x86/run_pieMd5 = "$run_pie_x86";  \/\/ run_pie_x86/;

  $text =~ s/grepMd5 = ".*";\s+\/\/ grep_mips/grepMd5 = "$grep_mips";  \/\/ grep_mips/;
  $text =~ s/nflogMd5 = ".*";\s+\/\/ nflog_mips/nflogMd5 = "$nflog_mips";  \/\/ nflog_mips/;
  $text =~ s/run_pieMd5 = ".*";\s+\/\/ run_pie_mips/run_pieMd5 = "$run_pie_mips";  \/\/ run_pie_mips/;

  print $out $text;
}

close $out;
