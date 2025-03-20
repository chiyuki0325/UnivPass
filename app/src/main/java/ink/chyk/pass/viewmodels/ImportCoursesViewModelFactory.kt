package ink.chyk.pass.viewmodels

import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.pass.api.*

class ImportCoursesViewModelFactory(
  private val pass: PassAPI
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(ImportCoursesViewModel::class.java)) {
      return ImportCoursesViewModel(
        pass,
        MMKV.defaultMMKV()
      ) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}