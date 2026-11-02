package app.duinch.remotetunnel.network

import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

class ComputeReconnectBackoff(
    private val initialDelayMs: Long = 1000L,
    private val maxDelayMs: Long = 16000L,
    private val jitterRatio: Double = 0.25,
    private val randomProvider: () -> Double = { Random.nextDouble() }
) {
    operator fun invoke(attempt: Int): Long {
        if (attempt <= 0) return 0L

        val safeAttempt = min(attempt, 8)
        val exponential = initialDelayMs * 2.0.pow((safeAttempt - 1).toDouble())
        val capped = min(exponential.toLong(), maxDelayMs)

        val jitterRange = (capped * jitterRatio).toLong()
        val jitter = if (jitterRange > 0) (randomProvider() * jitterRange).toLong() else 0L

        return min(capped + jitter, maxDelayMs)
    }
}
