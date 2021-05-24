package com.github.xeonkryptos.integration.gitlab.ui.general.event

import java.util.*

interface PagingEventListener : EventListener {

    fun onPreviousPage(event: PagingEvent)

    fun onNextPage(event: PagingEvent)
}