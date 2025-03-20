package ink.chyk.pass.viewmodels

import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.pass.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.Pair


class ImportCoursesViewModel(
  private val pass: PassAPI,
  private val mmkv: MMKV,
) : ViewModel() {

  private var _importing = MutableStateFlow(false)
  val importing: StateFlow<Boolean> = _importing
  private var _importCompleted = MutableStateFlow(false)
  val importCompleted: StateFlow<Boolean> = _importCompleted
  private var _output = MutableStateFlow("")
  val output: StateFlow<String> = _output
  private var _hasErrors = MutableStateFlow(false)
  val hasErrors: StateFlow<Boolean> = _hasErrors

  private val fetcher = CourseFetcher(pass, mmkv)

  private suspend fun realRunImport(): Pair<String, Boolean> {
    // 首先获取课程信息
    val result = fetcher.fetchCourses()
    if (result.hasErrors) {
      return Pair(result.output, true)
    }
    // 如果没有错误，那么就解析课程信息
    try {
      CourseImporter.importCourses(result, mmkv)
    } catch (e: Exception) {
      return Pair("获取课程表成功，但是在导入过程中发生错误：" + e.message, true)
    }
    return Pair(result.output, false)
  }

  fun runImport() {
    _importing.value = true
    _importCompleted.value = false
    _hasErrors.value = false
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        realRunImport()
      }.let { (output, hasErrors) ->
        _importing.value = false
        _hasErrors.value = hasErrors
        _importCompleted.value = true
        _output.value = output
      }
    }
  }
}
