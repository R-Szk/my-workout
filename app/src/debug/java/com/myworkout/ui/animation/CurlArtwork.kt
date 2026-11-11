package com.myworkout.ui.animation

import android.content.res.AssetManager
import android.util.Xml
import android.graphics.Matrix
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.core.graphics.PathParser
import org.xmlpull.v1.XmlPullParser

internal data class CurlPath(val path: Path, val fill: Color?, val stroke: Color?, val width: Float)
internal data class CurlArtwork(val groups: Map<String, List<CurlPath>>) {
    companion object {
        fun load(assets: AssetManager): CurlArtwork = assets.open("exercises/curl/character.svg").use { input ->
            val parser = Xml.newPullParser().apply { setInput(input, "UTF-8") }
            val groups = mutableMapOf<String, MutableList<CurlPath>>()
            var group = ""
            var originY = 0f
            fun color(name: String): Color? = parser.getAttributeValue(null, name)?.takeUnless { it == "none" }?.let {
                Color(it.toColorInt())
            }
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) when (parser.name) {
                    "g" -> {
                        group = parser.getAttributeValue(null, "id") ?: ""
                        originY = parser.getAttributeValue(null, "data-origin-y")?.toFloat() ?: 0f
                    }
                    "path" -> groups.getOrPut(group) { mutableListOf() }.add(CurlPath(
                        requireNotNull(PathParser.createPathFromPathData(parser.getAttributeValue(null, "d"))).apply {
                            if (originY != 0f) transform(Matrix().apply { setTranslate(0f, -originY) })
                        }.asComposePath(),
                        color("fill"), color("stroke"), parser.getAttributeValue(null, "stroke-width")?.toFloat() ?: 1f,
                    ))
                }
                parser.next()
            }
            require(groups.keys.containsAll(listOf("body", "farUpper", "farForearm", "farHand", "farDumbbell", "nearUpper", "nearForearm", "nearHand", "nearDumbbell")))
            CurlArtwork(groups)
        }
    }
}
