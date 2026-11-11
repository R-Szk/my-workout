package com.myworkout.ui.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.*

/** One shared renderer for the live animation and the pose-sheet verification. */
internal fun DrawScope.drawCurlCharacter(art: CurlArtwork, pose: CurlPose) {
    val fit = minOf(size.width / 360f, size.height / 600f)
    withTransform({ translate((size.width - 360f * fit) / 2, (size.height - 600f * fit) / 2); scale(fit, fit, Offset.Zero) }) {
        fun group(name: String) { art.groups.getValue(name).forEach { part ->
            part.fill?.let { drawPath(part.path, it) }
            part.stroke?.let { drawPath(part.path, it, style = Stroke(part.width)) }
        } }
        fun arm(side: String, arm: CurlArm, skin: Color) {
            withTransform({ translate(arm.elbow.x, arm.elbow.y); rotate(arm.angleDegrees, Offset.Zero) }) { group(side + "Forearm") }
            // Cover the rotating cut edge with a fixed, round elbow joint.
            drawCircle(skin, 12f, Offset(arm.elbow.x, arm.elbow.y))
            val wrist = Offset(arm.wrist.x, arm.wrist.y)
            val grip = Offset(arm.grip.x, arm.grip.y)
            drawLine(Color(0xFF493847), wrist, grip, 14f, StrokeCap.Round)
            drawLine(skin, wrist, grip, 12f, StrokeCap.Round)
            // Wrist/grip come from the forearm rig; the shaft keeps its projected orientation.
            withTransform({ translate(arm.dumbbell.x, arm.dumbbell.y); rotate(arm.dumbbellAngleDegrees, Offset.Zero) }) { group(side + "Dumbbell") }
            withTransform({ translate(arm.grip.x, arm.grip.y) }) { group(side + "Hand") }
        }
        group("farUpper"); group("body")
        arm("far", pose.far, Color(0xFFD49B71))
        group("nearUpper")
        arm("near", pose.near, Color(0xFFE3A779))
    }
}
