package com.myworkout

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.myworkout.ui.animation.*
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class CurlRigRenderingTest {
    private fun render(art: CurlArtwork, pose: CurlPose): Bitmap {
        val image = ImageBitmap(360, 600)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(image), Size(360f, 600f)) {
            drawCurlCharacter(art, pose)
        }
        return image.asAndroidBitmap()
    }
    private fun save(bitmap: Bitmap, name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
            ?: File(context.getExternalFilesDir(null), "curl-validation")
        directory.mkdirs()
        android.util.Log.i("CurlRender", "Exporting $name to $directory")
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun renderedJointsAndGripStayConnectedAndProduceInspectableKeyPoses() {
        val art = CurlArtwork.load(InstrumentationRegistry.getInstrumentation().targetContext.assets)
        val keyImages = mutableListOf<Bitmap>()
        for (frame in 0..8) {
            val pose = CurlPose.at(frame / 8f)
            val bitmap = render(art, pose)
            try {
                assertEquals("Transparent background", 0, bitmap.getPixel(0, 0))
                for ((arm, color) in listOf(pose.near to 0xFFE3A779.toInt(), pose.far to 0xFFD49B71.toInt())) {
                    // Check the visible joint itself, not only a fixed number in the pose model.
                    for (dy in -3..3) for (dx in -3..3)
                        assertEquals("Elbow patch changed at frame $frame", color, bitmap.getPixel(arm.elbow.x.toInt()+dx, arm.elbow.y.toInt()+dy))
                }
                val grip = pose.near.grip
                assertEquals("Plate must remain horizontal and attached to the palm at frame $frame", 0xFF4A5D6E.toInt(),
                    bitmap.getPixel((grip.x-28).roundToInt(), grip.y.roundToInt()))
                if (frame == 8) assertTrue("Rendered loop seam must match exactly", bitmap.sameAs(keyImages.first()))
                if (frame in listOf(0, 2, 4)) keyImages.add(bitmap.copy(Bitmap.Config.ARGB_8888, true))
            } finally { bitmap.recycle() }
        }
        try {
            for ((index, image) in keyImages.withIndex()) save(image, "curl-fixed-${index}.png")
            for (rig in listOf(false, true)) {
                val sheet = Bitmap.createBitmap(1080, 640, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(sheet)
                canvas.drawColor(0xFF121D2F.toInt())
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 19f }
                for ((index, image) in keyImages.withIndex()) {
                    canvas.drawBitmap(image, index*360f, 35f, null)
                    canvas.drawText(listOf("開始", "中間", "曲げ終わり")[index], index*360f+145f, 25f, paint)
                    if (rig) {
                        val pose = CurlPose.at(listOf(0f,.25f,.5f)[index])
                        paint.color = 0xFF2DD4BF.toInt(); paint.strokeWidth = 2f
                        for (arm in listOf(pose.near,pose.far)) {
                            val x=index*360f; val y=35f
                            canvas.drawLine(x+arm.shoulder.x,y+arm.shoulder.y,x+arm.elbow.x,y+arm.elbow.y,paint)
                            canvas.drawCircle(x+arm.shoulder.x,y+arm.shoulder.y,4f,paint)
                            canvas.drawCircle(x+arm.elbow.x,y+arm.elbow.y,4f,paint)
                            canvas.drawLine(x+arm.elbow.x,y+arm.elbow.y,x+arm.grip.x,y+arm.grip.y,paint)
                            canvas.drawCircle(x+arm.grip.x,y+arm.grip.y,4f,paint)
                        }
                        paint.color = android.graphics.Color.WHITE
                    }
                }
                save(sheet, if (rig) "curl-fixed-poses-rig.png" else "curl-fixed-poses.png")
                sheet.recycle()
            }
        } finally { keyImages.forEach { it.recycle() } }
    }
}
