package ink.chyk.pass.viewmodels

import androidx.lifecycle.*
import com.tencent.mmkv.*

class CoursesViewModelFactory : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(CoursesViewModel::class.java)) {
      return CoursesViewModel(
        MMKV.defaultMMKV()
      ) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}