<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Gitlab-Integration Changelog

## [Unreleased]

### Changed

- Made compatible to the newest IntelliJ IDE platform version

## [0.0.2-alpha]

### Added

- Show message notification bubble when loading of projects or accounts fail

### Changed

- Show error messages separately when they can't be assigned to a specific UI component (i.e., on login error messages
  when embedded in clone dialog)
- Moved general Gitlab settings entry point into Version Control

### Fixed

- Ignoring of disabled ssl verification when signing in for the first time via login dialog
- Initial loading of projects when sign in for the first time in clone dialog
- Sign in issues when downloading of the user's avatar failed for unknown reasons
- Detection of projects already on a known Gitlab instance
- Not stored deactivation of SSL verification after login procedure
- Incorrect checkbox state in gitlab settings view for disable SSL verification (typically marked as disabled when
  enabled)

### Security

- Remove gitlab account credentials on reset in gitlab settings dialog

## [0.0.1-alpha]

### Added

- Added support for different gitlab hosts
- Added support for different users on the same host
- Added support for disabled certificate validation (usually needed for self-hosted gitlab instances with a self-signed
  certificate)
- Download the avatar of the user and show it next to the search filter field
- Download repositories with paging feature to make cloning from within the IDE possible
- Added feature to share a project/module on Gitlab
