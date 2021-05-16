package com.github.xeonkryptos.integration.gitlab.api.gitlab.model

enum class AccessLevels(val accessLevelId: Int) {

    NO_ACCESS(0), MINIMAL_ACCESS(5), GUEST(10), REPORTER(20), DEVELOPER(30), MAINTAINER(40), OWNER(50);
}