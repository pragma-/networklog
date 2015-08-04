
## [2.25.1](https://github.com/pragma-/networklog/compare/2.25.0...2.25.1)  (2015-08-04)
##### Changed
Improve German translations (thanks CHEF-KOCH!)

## [2.25.0](https://github.com/pragma-/networklog/compare/2.24.2...2.25.0)  (2015-08-02)
##### Changed
Significant performance improvement for handling of apps with logging disabled.  Any apps with logging disabled will now have zero cpu/battery impact.

If you like to use Netflix, YouTube, Spotify, or any other high-traffic app while NetworkLog is enabled, but don't want to drain your battery with logging, just disable logging for that app.

## [2.24.2](https://github.com/pragma-/networklog/compare/2.24.1...2.24.2)  (2015-07-31)
##### Fixed
Fix an issue introduced by previous update (Lollipop fix) that prevented logging from working on some earlier (Jelly Bean, etc) devices.

## [2.24.1](https://github.com/pragma-/networklog/compare/2.24.0...2.24.1)  (2015-07-27)
##### Fixed
Fix an issue that prevented logging on Lollipop devices.

## [2.24.0](https://github.com/pragma-/networklog/compare/2.23.0...2.24.0)  (2015-05-16)
##### Changed
- Show total sent/recv statistics for packets/bytes for each app in the Apps tab without needing to expand the app
- History now loads from most recent logfile entry (no longer shows empty log if logging has been turned off for longer than the load-history time range)

##### Fixed
- Support arm64 devices

## [2.23.0](https://github.com/pragma-/networklog/compare/2.22.1...2.23.0)  (2014-04-16)
##### Fixed
- Improve detection of root and handling of root commands
- Improve sorting by traffic speed -- now sorts apps without current traffic by timestamp
- Performance optimizations

## [2.22.1](https://github.com/pragma-/networklog/compare/2.22.0...2.22.1)  (2014-04-07)
##### Fixed
Correct issue with previous update that prevented logging on some devices.

## [2.22.0](https://github.com/pragma-/networklog/compare/2.21.0...2.22.0)  (2014-04-07)
##### Added
- Add alternative logging methods for Samsung devices and other devices with restrictive SELinux enforcement policies (Samsung Galaxy S4, Samsung Galaxy Note, and others)
  The proper logging method will be automatically set on the first app-run, and can be manually configured via the new Logging Method preference (Settings -> Log Service -> Logging Method)

##### Changed
- Update Chinese translations (thanks myliyifei)

## [2.21.0](https://github.com/pragma-/networklog/compare/2.20.1...2.21.0)  (2013-11-19)
##### Added
- Added ability to disable logging for selected apps (for example, to reduce clutter/log-file space from bandwidth-heavy apps such as YouTube or Netflix, or for trusted apps)
- Added "Manage Apps" option in Settings to enable/disable logging for selected apps
- Added context menu items to enable/disable logging for selected apps
- Added context menu items to enable/disable connection notifications for selected apps
- Added context menu items to query WHOIS information for selected IP addresses

## [2.20.1](https://github.com/pragma-/networklog/compare/2.20.0...2.20.1)  (2013-10-04)
## [2.20.0](https://github.com/pragma-/networklog/compare/2.19.0...2.20.0)  (2013-09-29)
## [2.19.0](https://github.com/pragma-/networklog/compare/2.18.0...2.19.0)  (2013-09-19)
## [2.18.0](https://github.com/pragma-/networklog/compare/2.17.0...2.18.0)  (2013-04-30)
## [2.17.0](https://github.com/pragma-/networklog/compare/2.16.0...2.17.0)  (2013-04-11)
## [2.16.0](https://github.com/pragma-/networklog/compare/2.15.0...2.16.0)  (2013-03-28)
## [2.15.0](https://github.com/pragma-/networklog/compare/2.14.0...2.15.0)  (2013-03-23)
## [2.14.0](https://github.com/pragma-/networklog/compare/2.13.0...2.14.0)  (2013-03-20)
## [2.13.0](https://github.com/pragma-/networklog/compare/2.12.1...2.13.0)  (2013-03-11)
## [2.12.1](https://github.com/pragma-/networklog/compare/2.12.0...2.12.1)  (2013-03-06)
## [2.12.0](https://github.com/pragma-/networklog/compare/2.11.0...2.12.0)  (2013-03-05)
## [2.11.0](https://github.com/pragma-/networklog/compare/2.10.0...2.11.0)  (2013-02-24)
## [2.10.0](https://github.com/pragma-/networklog/compare/2.9.0...2.10.0)  (2013-02-16)
## [2.9.0](https://github.com/pragma-/networklog/compare/2.8.4...2.9.0)  (2013-02-11)
## [2.8.4](https://github.com/pragma-/networklog/compare/2.8.3...2.8.4)  (2013-02-04)
## [2.8.3](https://github.com/pragma-/networklog/compare/2.8.2...2.8.3)  (2013-01-31)
## [2.8.2](https://github.com/pragma-/networklog/compare/2.8.1...2.8.2)  (2013-01-30)
## [2.8.1](https://github.com/pragma-/networklog/compare/2.8.0...2.8.1)  (2013-01-29)
## [2.8.0](https://github.com/pragma-/networklog/compare/2.7.2...2.8.0)  (2013-01-25)
## 2.7.2  (2013-01-17)

