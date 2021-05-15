package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event

import java.util.*

interface ReloadDataEventListener : EventListener {

    fun onReloadRequest(event: ReloadDataEvent)
}