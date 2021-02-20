package com.github.xeonkryptos.integration.gitlab.util

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ModalityState
import java.awt.Component

/**
 * @author Xeonkryptos
 * @since 20.02.2021
 */
fun Application.invokeOnDispatchThread(component: Component, action: () -> Unit) {
    if (!this.isDispatchThread) {
        this.invokeLater({ action.invoke() }, ModalityState.stateForComponent(component))
    } else {
        action.invoke()
    }
}
