package com.github.xeonkryptos.integration.gitlab.ui.general.event

import java.util.*

interface ReloadDataEventListener : EventListener {

    fun onReloadRequest(event: ReloadDataEvent)
}