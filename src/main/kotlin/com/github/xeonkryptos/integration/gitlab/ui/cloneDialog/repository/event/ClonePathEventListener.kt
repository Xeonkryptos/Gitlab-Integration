package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event

import java.util.*

interface ClonePathEventListener : EventListener {

    fun onClonePathChanged(event: ClonePathEvent)
}