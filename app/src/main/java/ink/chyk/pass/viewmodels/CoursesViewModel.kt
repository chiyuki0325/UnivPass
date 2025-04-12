package ink.chyk.pass.viewmodels

import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.pass.*
import ink.chyk.pass.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.time.*
import java.time.format.*
import java.util.Locale
import kotlin.Pair


class CoursesViewModel(
  private val mmkv: MMKV
) : ViewModel() {
  // 课程表 ViewModel

  // 2025.3.11 更改：date 状态不再由 ViewModel 维护，转为在界面中维护
  // 为了做翻页

  // 日期格式化器
  // 储存成一个状态避免啰唆
  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  // 今天的日期 id
  val today get() = LocalDate.now().format(formatter)

  // 学期开始时的日期 id
  private val termStart = mmkv.decodeString("term_start") ?: today

  // 学期开始时的日期对象
  private val termStartDate = LocalDate.parse(termStart, formatter)

  fun isCourseImported(): Boolean {
    // 课程表是否导入过
    return mmkv.containsKey("course_keys")
  }

  fun getCoursesByDate(dateId: String): List<Course> {
    // 根据 dateId 获取课程
    try {
      return mmkv.decodeString("course_${dateId}")?.let {
        Json.decodeFromString(it)
      } ?: emptyList()
    } catch (e: Exception) {
      // 解析失败，清空课表重新导入
      mmkv.remove("course_keys")
      return emptyList()
    }
  }

  fun getWeekday(dateId: String): String {
    // 传入 dateId 获取星期几
    val localDate = LocalDate.parse(dateId, formatter)
    return localDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
  }

  fun getWeekdayNum(dateId: String): Int {
    // 传入 dateId 获取星期几，周日为0，以此类推
    val localDate = LocalDate.parse(dateId, formatter)
    return localDate.dayOfWeek.value
  }

  fun thisWeek(dateId: String): Int {
    // 根据 dateId 计算当前周数
    val localDate = LocalDate.parse(dateId, formatter)
    return (localDate.toEpochDay() - termStartDate.toEpochDay()).toInt() / 7 + 1
  }

  fun thisWeekDates(dateId: String): List<Pair<Pair<LocalDate, String>, Int>> {
    // 计算本周日期，数据提供给底部切换周几的按钮
    // 当前 dateId -> 日期对象，日期字符串，课程数量
    val localDate = LocalDate.parse(dateId, formatter)
    val dayOfWeek = localDate.dayOfWeek.value.toLong().let { if (it == 7L) 0L else it }
    val weekStart = localDate.minusDays(dayOfWeek)
    return (0..6).map {
      val thatDate = weekStart.plusDays(it.toLong())
      val thatDateId = thatDate.format(formatter)
      val courseCount = getCoursesByDate(thatDateId).size
      thatDate to thatDateId to courseCount
    }
  }

  fun isDateInTerm(dateId: String): Boolean {
    // 判断 dateId 日期是否在学期内
    val termStart = mmkv.decodeString("term_start") ?: return false
    return termStart.toInt() <= dateId.toInt()
  }

  fun prevWeek(dateId: String): String {
    val newDate = LocalDate.parse(dateId, formatter).minusWeeks(1).format(formatter)
    // 边界检查
    if (!isDateInTerm(newDate)) {
      return dateId
    }
    return newDate
  }

  fun nextWeek(dateId: String): String {
    return LocalDate.parse(dateId, formatter).plusWeeks(1).format(formatter)
  }

  fun backToday(): String {
    return today?: LocalDate.now().format(formatter)
  }

  fun isToday(dateId: String): Boolean {
    return dateId == today
  }

  fun prevDay(dateId: String): String {
    return LocalDate.parse(dateId, formatter).minusDays(1).format(formatter)
  }

  fun nextDay(dateId: String): String {
    return LocalDate.parse(dateId, formatter).plusDays(1).format(formatter)
  }
}