# Gitlab-Integration

![Build](https://github.com/Xeonkryptos/Gitlab-Integration/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [x] Verify the [pluginGroup](/gradle.properties), [plugin ID](/src/main/resources/META-INF/plugin.xml) and [sources package](/src/main/kotlin).
- [x] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html).
- [ ] [Publish a plugin manually](https://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/publishing_plugin.html) for the first time.
- [ ] Set the Plugin ID in the above README badges.
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).
- [x] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

<!-- Plugin description -->
This plugin is designed to work with Gitlab in the same manner as the Github plugin of Jetbrains itself. At the moment, only cloning from Gitlab is supported. Some upcoming and planned features you
can see below.

Planned features:

* Switch between tree representation and list representation of your projects
* Create new gitlab projects from within the IDE
* Make configurable if checkout should be done via HTTPS or SSH protocol
* Multiuser support for different gitlab hosts
* Same as the official Github plugin made by Jetbrains, handle merge requests from the IDE
<!-- Plugin description end -->
