# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased][unreleased]

## [2.25.0] - 2015-08-02
### Changed
Significant performance improvement for handling of apps with logging disabled.  Any apps with logging disabled will now have zero cpu/battery impact.

If you like to use Netflix, YouTube, Spotify, or any other high-traffic app while NetworkLog is enabled, but don't want to drain your battery with logging, just disable logging for that app.

## [2.24.2] - 2015-07-31
### Fixed
Fix an issue introduced by previous update (Lollipop fix) that prevented logging from working on some earlier (Jelly Bean, etc) devices.

## [2.24.1] - 2015-07-27
### Fixed
Fix an issue that prevented logging on Lollipop devices.

## [2.24.0] - 2015-05-16
### Changed
- Show total sent/recv statistics for packets/bytes for each app in the Apps tab without needing to expand the app
- History now loads from most recent logfile entry (no longer shows empty log if logging has been turned off for longer than the load-history time range)

### Fixed
- Support arm64 devices

## [2.23.0] - 2014-04-16
### Fixed
- Improve detection of root and handling of root commands
- Improve sorting by traffic speed -- now sorts apps without current traffic by timestamp
- Performance optimizations

