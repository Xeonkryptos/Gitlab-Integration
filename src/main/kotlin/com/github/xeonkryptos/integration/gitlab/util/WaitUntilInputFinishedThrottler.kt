package com.github.xeonkryptos.integration.gitlab.util

import com.intellij.openapi.Disposable
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class WaitUntilInputFinishedThrottler(private val throttledTask: () -> Unit) : Disposable {

    companion object {
        const val THROTTLE_TASK_TIME_IN_MILLISECONDS: Long = 500L
    }

    private val timer: Timer = Timer()
    private val throttleTask: TimerTask = object : TimerTask() {
        private var lastRequestExecuted: Long = 0L

        override fun run() {
            if (lastLetterTyped != lastRequestExecuted && System.nanoTime() - lastLetterTyped > TimeUnit.MILLISECONDS.toNanos(THROTTLE_TASK_TIME_IN_MILLISECONDS)) {
                try {
                    throttledTask.invoke()
                } catch (ignored: Exception) {
                }
                lastRequestExecuted = lastLetterTyped
            }
        }
    }

    @Volatile
    private var lastLetterTyped: Long = 0L

    init {
        timer.schedule(throttleTask, THROTTLE_TASK_TIME_IN_MILLISECONDS, THROTTLE_TASK_TIME_IN_MILLISECONDS)
    }

    fun onNewInputReceived() {
        lastLetterTyped = System.nanoTime()
    }

    override fun dispose() {
        timer.cancel()
        throttleTask.cancel()
        timer.purge()
    }
}