package com.myworkout.ui.animation

import kotlin.math.*

data class CurlPoint(val x: Float, val y: Float)
data class CurlArm(val shoulder: CurlPoint, val elbow: CurlPoint, val angleDegrees: Float) {
    fun pointAt(distance: Float): CurlPoint {
        val angle = Math.toRadians(angleDegrees.toDouble())
        return CurlPoint(elbow.x - sin(angle).toFloat() * distance, elbow.y + cos(angle).toFloat() * distance)
    }
    val wrist: CurlPoint get() = pointAt(84f)
    val grip: CurlPoint get() = pointAt(96f)
    val dumbbell: CurlPoint get() = grip
    // The shaft crosses the palm; elbow flexion must not spin it in the image plane.
    val dumbbellAngleDegrees: Float get() = 0f
}
data class CurlPose(val near: CurlArm, val far: CurlArm) {
    companion object {
        const val CYCLE_MILLIS = 2500.0
        fun at(progress: Float): CurlPose {
            val p = progress.coerceIn(0f, 1f).toDouble()
            val amount = ((1.0 - cos(2.0 * PI * p)) / 2.0).toFloat()
            val angle = -8f - 137f * amount
            return CurlPose(CurlArm(CurlPoint(204f, 183f), CurlPoint(204f, 252f), angle), CurlArm(CurlPoint(119f, 183f), CurlPoint(119f, 244f), angle))
        }
        fun advance(progress: Double, elapsedNanos: Long, playing: Boolean): Double =
            if (!playing) progress else (progress + elapsedNanos.coerceAtLeast(0) / (CYCLE_MILLIS * 1_000_000.0)) % 1.0
    }
}
