package ink.chyk.pass.viewmodels

import androidx.lifecycle.*
import com.tencent.mmkv.*

class PassCodeViewModelFactory : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(PassCodeViewModel::class.java)) {
      return PassCodeViewModel(
        MMKV.defaultMMKV(),
      ) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}