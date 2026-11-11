package com.myworkout.animation

import com.myworkout.ui.animation.CurlPose
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class CurlPoseTest {
    @Test fun elbowsAndLimbLengthsStayFixedThroughoutTheCycle() {
        val start = CurlPose.at(0f)
        for (frame in 0..250) {
            val pose = CurlPose.at(frame / 250f)
            for ((arm, original) in listOf(pose.near to start.near, pose.far to start.far)) {
                assertEquals(original.elbow, arm.elbow)
                assertEquals(84.0, hypot((arm.wrist.x-arm.elbow.x).toDouble(), (arm.wrist.y-arm.elbow.y).toDouble()), .001)
                assertEquals(96.0, hypot((arm.dumbbell.x-arm.elbow.x).toDouble(), (arm.dumbbell.y-arm.elbow.y).toDouble()), .001)
            }
        }
    }
    @Test fun curlLiftsTheWeightsAndReturnsWithoutJumpingAtTheSeam() {
        val bottom = CurlPose.at(0f)
        val top = CurlPose.at(.5f)
        assertTrue(top.near.wrist.y < top.near.elbow.y)
        assertTrue(bottom.near.wrist.y > bottom.near.elbow.y)
        assertEquals(bottom, CurlPose.at(1f))
        val before = CurlPose.at(.9999f).near.wrist
        assertEquals(bottom.near.wrist.x, before.x, .01f)
        assertEquals(bottom.near.wrist.y, before.y, .01f)
        assertEquals(CurlPose.at(.25f), CurlPose.at(.75f))
    }
    @Test fun pausedOrBackgroundClockDoesNotAdvanceAndResumeKeepsPhase() {
        assertEquals(.4, CurlPose.advance(.4, 900_000_000, false), 0.0)
        assertEquals(.5, CurlPose.advance(.4, 250_000_000, true), .000001)
        assertEquals(.1, CurlPose.advance(.9, 500_000_000, true), .000001)
        assertEquals(.4, CurlPose.advance(.4, -1, true), 0.0)
    }
    @Test fun shoulderElbowGripAndWeightUseTheSameFixedRig() {
        val start = CurlPose.at(0f)
        for (frame in 0..500) {
            val pose = CurlPose.at(frame / 500f)
            for ((arm, original) in listOf(pose.near to start.near, pose.far to start.far)) {
                assertEquals(original.shoulder, arm.shoulder)
                assertEquals(original.elbow, arm.elbow)
                assertEquals(arm.grip, arm.dumbbell)
                assertEquals(0f, arm.dumbbellAngleDegrees, 0f)
                assertEquals(12.0, hypot((arm.grip.x-arm.wrist.x).toDouble(), (arm.grip.y-arm.wrist.y).toDouble()), .001)
            }
        }
    }
    @Test fun weightsRemainInsideTheArtworkAndMotionIsContinuous() {
        var last = CurlPose.at(0f)
        for (frame in 1..500) {
            val pose = CurlPose.at(frame / 500f)
            for (arm in listOf(pose.near, pose.far)) {
                assertTrue(arm.dumbbell.x in 35f..325f)
                assertTrue(arm.dumbbell.y in 130f..370f)
            }
            assertTrue(hypot((pose.near.wrist.x-last.near.wrist.x).toDouble(), (pose.near.wrist.y-last.near.wrist.y).toDouble()) < 2.0)
            last = pose
        }
    }
}
