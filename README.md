# Gitlab-Integration

![Build](https://github.com/Xeonkryptos/Gitlab-Integration/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/16852-gitlab-integration.svg)](https://plugins.jetbrains.com/plugin/16852-gitlab-integration)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/16852-gitlab-integration.svg)](https://plugins.jetbrains.com/plugin/16852-gitlab-integration)

<!-- Plugin description -->
The goal of this plugin is to simplify your work with Gitlab. No need anymore to leave your preferred IDE to check out a
new project, share your local projects on Gitlab and all that nasty stuff.<br/>
Note, you're not restricted to the official Gitlab at https://gitlab.com. This plugin works with all self-hosted Gitlab
instances, too, as long as they're supporting Gitlab's V4 API.

Supported features:

* Multi-user support on different Gitlab instances (many users for one Gitlab instance (yeah it is really supported) as
  many gitlab instances at the same time, too)
* Representation of all projects (at least you have the rights to see them, though) or only your own. Filtering via text
  field is available, too.
* Cloning of a project in the representation to your local system
* Disabling of ssl certificate verification is possible. You should only enable it, when you know what you're doing and
  trusting the corresponding host, or the certificate of this host. Mostly you need this feature, when you're working
  with a self-hosted Gitlab instance with a self-signed certificate.
* Share your local project on Gitlab. Simply create your own project locally with all the things you need, or simply use
  an already available one and share it with your Gitlab instance. The local git repository gets created if it is
  missing, the Gitlab project will be created for you and first commit and push are done for you too (if your project is
  already a git repository, a new remote is set, and a push of the existing commits is done. No additional commit gets
  created).

Planned features:

* Set the origin of a local project to an existing in your Gitlab instance with the share feature.<br/>
  At the moment, the project gets created all the time. If the creation fails, the entire share process fails. Means, if
  the project exists already, nothing is done as the process aborts. Maybe you have a Gitlab project template already
  and need to link both together. This isn't possible yet.
* Make configurable if checkout and push should be done via HTTPS or SSH protocol (SSH is mandatory for both at the
  moment).
* Same as the official Github plugin made by Jetbrains, handle merge requests from within the IDE (a task for the far
  far future)

<!-- Plugin description end -->
