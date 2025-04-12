package ink.chyk.pass

import androidx.core.graphics.*

class HashColorHelper {
  companion object {
    private fun isCourseStopped(courseName: String): Boolean =
      courseName.startsWith("停课")

    private val hueCache = mutableMapOf<String, Float>()

    fun calcColor(
      courseName: String
    ): Int {
      val saturation = 0.6f
      val lightness = 0.6f
      return calcColorWith(courseName, saturation, lightness)
    }

    fun calcTextColor(
      courseName: String,
      darkMode: Boolean,
    ): Int {
      val saturation = 0.6f
      val lightness = if (darkMode) 0.8f else 0.35f
      return calcColorWith(courseName, saturation, lightness)
    }

    fun calcBackgroundColor(
      courseName: String,
      darkMode: Boolean,
      ignoreStopped: Boolean = false
    ): Int {
      val saturation = if (darkMode) 0.4f else 0.5f
      val lightness = if (darkMode) 0.25f else 0.85f
      return calcColorWith(courseName, saturation, lightness, ignoreStopped)
    }

    private fun calcColorWith(
      courseName: String,
      saturation: Float,
      lightness: Float,
      ignoreStopped: Boolean = false
    ): Int {
      // 根据名称的哈希值计算颜色
      val hue = if (hueCache.containsKey(courseName)) {
        hueCache[courseName]!!
      } else {
        (if (!ignoreStopped && isCourseStopped(courseName)) {
          0.0f
        } else {
          Math.floorMod(courseName.hashCode(), 360).toFloat()
        }).also {
          hueCache[courseName] = it
        }
      }
      return ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))
    }
  }
}