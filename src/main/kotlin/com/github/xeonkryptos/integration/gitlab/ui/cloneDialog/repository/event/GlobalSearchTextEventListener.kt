package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event

import java.util.*

interface GlobalSearchTextEventListener : EventListener {

    fun onGlobalSearchTextChanged(event: GlobalSearchTextEvent)

    fun onGlobalSearchTextDeleted(event: GlobalSearchTextEvent)
}