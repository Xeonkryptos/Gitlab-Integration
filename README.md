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
The goal of this plugin is to simplify your work with Gitlab. No need anymore to leave your preferred IDE to check out a new project you need or create a new one and update the origin and all that
nasty stuff. Note, you're not restricted to the official Gitlab at https://gitlab.com. This plugin works with all self-hosted Gitlab instances, too, as long as they're supporting Gitlab's V4 API.

Supported features:

* Multi-user support on different Gitlab instances (many users for one Gitlab instance (yeah it is really supported) as many gitlab instances at the same time, too)
* Tree representation of all projects (at least you have the rights to see them, though) or only your own. Every project of every host is visible in the same tree. Filtering via text field is available, too.
* Cloning of a project in the representation to your local system
* Disabling of ssl certificate verification is possible, too. You should only enable it, when you know what you're doing and trusting the corresponding host, or the certificate of this host. Mostly
  you need this feature, when you're working with a self-hosted Gitlab instance with a self-signed certificate.

Planned features:

* Switch between tree representation and list representation of your projects
* Create new gitlab projects from within the IDE or set the origin of a local project to an existing in your Gitlab instance 
* Make configurable if checkout should be done via HTTPS or SSH protocol
* Multiuser support for different gitlab hosts
* Same as the official Github plugin made by Jetbrains, handle merge requests from within the IDE (a task for the far future)
<!-- Plugin description end -->
