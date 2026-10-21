package com.ltnstore.titvi.util

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * Gom các phím số 0..9 lại trong khoảng [timeoutMs].
 * Khi hết thời gian, gọi onNumberEntered(number).
 */
class NumberBuffer(
    private val timeoutMs: Long = 1000L,
    private val onNumberEntered: (Int) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val buffer = StringBuilder()
    private var lastPressTime = 0L

    private val flushRunnable = Runnable {
        val value = buffer.toString().toIntOrNull()
        buffer.setLength(0)
        lastPressTime = 0L
        if (value != null && value > 0) onNumberEntered(value)
    }

    fun onDigit(digit: Int) {
        val now = SystemClock.elapsedRealtime()
        // Nếu quá lâu kể từ lần trước, xoá buffer
        if (now - lastPressTime > timeoutMs) buffer.setLength(0)
        buffer.append(digit)
        lastPressTime = now
        handler.removeCallbacks(flushRunnable)
        handler.postDelayed(flushRunnable, timeoutMs)
    }

    fun currentInput(): String = buffer.toString()

    fun cancel() {
        handler.removeCallbacks(flushRunnable)
        buffer.setLength(0)
        lastPressTime = 0L
    }
}