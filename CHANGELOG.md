<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Gitlab-Integration Changelog

## [Unreleased]

### Added

- Added support for different gitlab hosts
- Added support for different users on the same host
- Added configuration to disable certificate validation (usually needed for self-hosted gitlab instances with a self-signed certificate)
- Download the avatar of the user and show it next to the search filter field
- Download user's repositories after authentication and add those into the tree view
- Filter functionality into a tree
- Added gitlab brand icon into clone dialog
- Created basic extension point to register UI components into clone dialog to support gitlab
- Added login dialog into a custom Gitlab installation via access token
- store gitlab hosts and tokens to load them on the restart of IntelliJ
