package ink.chyk.pass

import androidx.core.graphics.*

class HashColorHelper {
  companion object {
    private fun isCourseStopped(courseName: String): Boolean =
      courseName.startsWith("停课")

    private val colorCache = mutableMapOf<String, Int>()

    fun calcColor(
      courseName: String,
      darkMode: Boolean,
      ignoreStopped: Boolean = false
    ): Int {
      // 根据名称的哈希值计算颜色
      if (colorCache.containsKey(courseName)) {
        return colorCache[courseName]!!
      }
      val courseColor = if (!ignoreStopped && isCourseStopped(courseName)) {
        0xFFB0B0B0.toInt()
      } else {
        val hue = Math.floorMod(courseName.hashCode(), 360).toFloat()
        val saturation = 0.6f
        val lightness = if (darkMode) 0.6f else 0.4f
        ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))
      }
      colorCache[courseName] = courseColor
      return courseColor
    }
  }
}